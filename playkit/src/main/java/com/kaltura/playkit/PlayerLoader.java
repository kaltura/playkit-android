/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.ads.AdvertisingConfig;
import com.kaltura.playkit.ads.PKAdvertisingController;
import com.kaltura.playkit.player.PlayerController;
import com.kaltura.playkit.plugins.playback.KalturaPlaybackRequestAdapter;
import com.kaltura.playkit.plugins.playback.KalturaUDRMLicenseRequestAdapter;
import com.kaltura.playkit.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;


class LoadedPlugin {
    LoadedPlugin(PKPlugin plugin, PlayerDecorator decorator) {
        this.plugin = plugin;
        this.decorator = decorator;
    }

    PKPlugin plugin;
    PlayerDecorator decorator;
}

class PlayerLoader extends PlayerDecoratorBase {

    private static final PKLog log = PKLog.get("PlayerLoader");

    private Context context;
    private MessageBus messageBus;

    private Map<String, LoadedPlugin> loadedPlugins = new LinkedHashMap<>();
    private PlayerController playerController;
    private @Nullable AdvertisingConfig advertisingConfig;
    private PKAdvertisingController pkAdvertisingController;
    private final String kavaPluginKey = "kava";
    private boolean isKavaImpressionFired;

    PlayerLoader(Context context, MessageBus messageBus) {
        this.context = context;
        if (messageBus != null) {
            this.messageBus = messageBus;
        } else {
            this.messageBus = new MessageBus();
        }
    }

    public void load(@NonNull PKPluginConfigs pluginsConfig) {

        playerController = new PlayerController(context);

        // By default, set Kaltura decorator.

        KalturaPlaybackRequestAdapter.install(playerController, getDefaultReferrer());
        KalturaUDRMLicenseRequestAdapter.install(playerController, getDefaultReferrer());

        playerController.setEventListener(messageBus::post);

        Player player = playerController;
        PlayerEngineWrapper playerEngineWrapper = null;

        for (Map.Entry<String, Object> entry : pluginsConfig) {
            String name = entry.getKey();
            PKPlugin plugin = loadPlugin(name, player, entry.getValue(), messageBus, context);

            if (plugin == null) {
                log.w("Plugin not found: " + name);
                continue;
            }

            // Check if the plugin provides a PlayerDecorator.
            PlayerDecorator decorator = plugin.getPlayerDecorator();
            if (decorator != null) {
                decorator.setPlayer(player);
                player = decorator;
            }

            PlayerEngineWrapper wrapper = plugin.getPlayerEngineWrapper();
            if (wrapper != null && playerEngineWrapper == null) {
                playerEngineWrapper = wrapper;
            }

            loadedPlugins.put(name, new LoadedPlugin(plugin, decorator));
        }

        playerController.setPlayerEngineWrapper(playerEngineWrapper);

        setPlayer(player);
    }

    private String getDefaultReferrer() {
        return new Uri.Builder().scheme("app").authority(context.getPackageName()).toString();
    }

    @Override
    public void updatePluginConfig(@NonNull final String pluginName, @Nullable final Object pluginConfig) {
        LoadedPlugin loadedPlugin = loadedPlugins.get(pluginName);
        if (loadedPlugin != null) {
            loadedPlugin.plugin.onUpdateConfig(pluginConfig);
        }
//        messageBus.post(() -> {
//            LoadedPlugin loadedPlugin = loadedPlugins.get(pluginName);
//            if (loadedPlugin != null) {
//                loadedPlugin.plugin.onUpdateConfig(pluginConfig);
//            }
//        });
    }

    @Override
    public void destroy() {
        isKavaImpressionFired = false;
        stop();
        releasePlugins();
        releasePlayer();
    }

    @Override
    public void onApplicationResumed() {
        getPlayer().onApplicationResumed();

        for (Map.Entry<String, LoadedPlugin> stringLoadedPluginEntry : loadedPlugins.entrySet()) {
            stringLoadedPluginEntry.getValue().plugin.onApplicationResumed();
        }
    }

    @Override
    public void onApplicationPaused() {
        for (Map.Entry<String, LoadedPlugin> stringLoadedPluginEntry : loadedPlugins.entrySet()) {
            stringLoadedPluginEntry.getValue().plugin.onApplicationPaused();
        }
        getPlayer().onApplicationPaused();
    }

    private void releasePlayer() {
        getPlayer().destroy();
    }

    @Override
    public void prepare(@NonNull final PKMediaConfig mediaConfig) {

        //If mediaConfig is not valid, playback is impossible, so return.
        //setMedia() is responsible to notify application with exact error that happened.
        if (!playerController.setMedia(mediaConfig)) {
            return;
        }

        super.prepare(mediaConfig);

        if (pkAdvertisingController != null && playerController.getController(AdController.class) != null) {
            pkAdvertisingController.setAdController(playerController.getController(AdController.class));
            pkAdvertisingController.setAdvertising(advertisingConfig);
        }

        for (Map.Entry<String, LoadedPlugin> loadedPluginEntry : loadedPlugins.entrySet()) {
            loadedPluginEntry.getValue().plugin.onUpdateMedia(mediaConfig);
        }

        if (!PKDeviceCapabilities.isKalturaPlayerAvailable() &&
                loadedPlugins != null && !loadedPlugins.containsKey(kavaPluginKey)) {
            Player player = getPlayer();
            String sessionId = (player != null && player.getSessionId() != null) ? player.getSessionId() : "";
            doKavaAnalyticsCall(mediaConfig, sessionId);
        }

//        messageBus.post(new Runnable() {
//            @Override
//            public void run() {
//                for (Map.Entry<String, LoadedPlugin> loadedPluginEntry : loadedPlugins.entrySet()) {
//                    loadedPluginEntry.getValue().plugin.onUpdateMedia(mediaConfig);
//                }
//            }
//        });
    }

    @Override
    public void setAdvertising(@Nullable AdvertisingConfig advertisingConfig, @NonNull PKAdvertisingController pkAdvertisingController) {
        if (!PKDeviceCapabilities.isKalturaPlayerAvailable()) {
            log.e("Advertising is being used to configure custom AdLayout. This feature is not available in Playkit SDK. " +
                    "It is only being used by Kaltura Player SDK.");
            return;
        }
        this.advertisingConfig = advertisingConfig;
        this.pkAdvertisingController = pkAdvertisingController;
    }

    private void releasePlugins() {
        // Unload in the reversed order they were loaded, peeling off the decorators.
        List<Map.Entry<String, LoadedPlugin>> plugins = new ArrayList<>(loadedPlugins.entrySet());
        ListIterator<Map.Entry<String, LoadedPlugin>> listIterator = plugins.listIterator(plugins.size());

        Player currentLayer = getPlayer();

        while (listIterator.hasPrevious()) {
            Map.Entry<String, LoadedPlugin> pluginEntry = listIterator.previous();
            LoadedPlugin loadedPlugin = pluginEntry.getValue();

            // Peel off decorator, if this plugin added one
            if (loadedPlugin.decorator != null) {
                Assert.checkState(loadedPlugin.decorator == currentLayer, "Decorator/layer mismatch");
                if (currentLayer instanceof PlayerDecorator) {
                    currentLayer = ((PlayerDecorator) currentLayer).getPlayer();
                }
            }

            // Release the plugin
            loadedPlugin.plugin.onDestroy();
            loadedPlugins.remove(pluginEntry.getKey());
        }

        setPlayer(currentLayer);
    }

    /**
     * Do KavaAnalytics call
     * @param pkMediaConfig mediaConfig
     */
    private void doKavaAnalyticsCall(PKMediaConfig pkMediaConfig, String sessionId) {
        Pair<Integer, String> metaData = getRequiredAnalyticsInfo(pkMediaConfig);

        int partnerId = metaData.first == null ? 0 : metaData.first;
        String entryId = TextUtils.isEmpty(metaData.second) ? "" : metaData.second;

        if (partnerId <= 0) {
            partnerId = NetworkUtils.DEFAULT_KAVA_PARTNER_ID;
            entryId = NetworkUtils.DEFAULT_KAVA_ENTRY_ID;
        }

        if (!isKavaImpressionFired) {
            NetworkUtils.sendKavaAnalytics(context, partnerId, entryId, NetworkUtils.KAVA_EVENT_IMPRESSION, sessionId);
            isKavaImpressionFired = true;
        }

        NetworkUtils.sendKavaAnalytics(context, partnerId, entryId, NetworkUtils.KAVA_EVENT_PLAY_REQUEST, sessionId);
    }

    /**
     * Get EntryId and PartnerId from Metadata
     * @param pkMediaConfig mediaConfig
     * @return Pair of Entry and partner Ids
     */
    private Pair<Integer, String> getRequiredAnalyticsInfo(PKMediaConfig pkMediaConfig) {
        final String kavaPartnerIdKey = "kavaPartnerId";
        final String kavaEntryIdKey = "entryId";
        int kavaPartnerId = 0;
        String kavaEntryId = null;

        if (pkMediaConfig.getMediaEntry() != null && pkMediaConfig.getMediaEntry().getMetadata() != null) {
            if (pkMediaConfig.getMediaEntry().getMetadata().containsKey(kavaPartnerIdKey)) {
                String partnerId = pkMediaConfig.getMediaEntry().getMetadata().get(kavaPartnerIdKey);
                kavaPartnerId = Integer.parseInt(partnerId != null && TextUtils.isDigitsOnly(partnerId) && !TextUtils.isEmpty(partnerId) ? partnerId : "0");
            }

            if (pkMediaConfig.getMediaEntry().getMetadata().containsKey(kavaEntryIdKey)) {
                kavaEntryId = pkMediaConfig.getMediaEntry().getMetadata().get(kavaEntryIdKey);
            }
        }

        return Pair.create(kavaPartnerId, kavaEntryId);
    }

    private PKPlugin loadPlugin(String name, Player player, Object config, MessageBus messageBus, Context context) {
        PKPlugin plugin = PlayKitManager.createPlugin(name);
        if (plugin != null) {
            plugin.onLoad(player, config, messageBus, context);
        }
        return plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public PKEvent.Listener addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        return messageBus.listen(listener, events);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        messageBus.remove(listener, events);
    }

    @SuppressWarnings("deprecation")
    @Override
    public PKEvent.Listener addStateChangeListener(@NonNull final PKEvent.Listener listener) {
        return messageBus.listen(listener, PlayerEvent.Type.STATE_CHANGED);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeStateChangeListener(@NonNull final PKEvent.Listener listener) {
        messageBus.remove(listener, PlayerEvent.Type.STATE_CHANGED);
    }

    @Override
    public void removeListener(@NonNull PKEvent.Listener listener) {
        messageBus.removeListener(listener);
    }

    @Override
    public <E extends PKEvent> void addListener(Object groupId, Class<E> type, PKEvent.Listener<E> listener) {
        messageBus.addListener(groupId, type, listener);
    }

    @Override
    public void addListener(Object groupId, Enum type, PKEvent.Listener listener) {
        messageBus.addListener(groupId, type, listener);
    }

    @Override
    public void removeListeners(@NonNull Object groupId) {
        messageBus.removeListeners(groupId);
    }

    @NonNull
    @Override
    public <PluginType> List<PluginType> getLoadedPluginsByType(Class<PluginType> pluginClass) {
        List<PluginType> filteredPlugins = new ArrayList<>();
        for (LoadedPlugin loadedPlugin : loadedPlugins.values()) {
            if (pluginClass.isAssignableFrom(loadedPlugin.plugin.getClass())) {
                @SuppressWarnings({"unchecked", "isAssignableFrom checks both superclass and superinterface"})
                PluginType pluginType = (PluginType) loadedPlugin.plugin;
                filteredPlugins.add(pluginType);
            }
        }
        return filteredPlugins;
    }
}
