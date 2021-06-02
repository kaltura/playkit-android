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
    private boolean isKavaImpressionFired;
    private String kavaPluginKey = "kava";
    private String kavaPartnerIdKey = "kavaPartnerId";

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
        stop();
        releasePlugins();
        releasePlayer();
        isKavaImpressionFired = false;
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

        if (loadedPlugins != null && !isKavaImpressionFired) {
            int[] info = getKavaImpressionInfo(mediaConfig);
            int partnerId = info[0] > 0 ? info[0] : NetworkUtils.DEFAULT_KAVA_PARTNER_ID;
            boolean fireKavaImpression = info[1] != 0;

            if (fireKavaImpression){
                NetworkUtils.sendKavaImpression(context, partnerId);
                isKavaImpressionFired = true;
            }
        }

        for (Map.Entry<String, LoadedPlugin> loadedPluginEntry : loadedPlugins.entrySet()) {
            loadedPluginEntry.getValue().plugin.onUpdateMedia(mediaConfig);
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

    private int[] getKavaImpressionInfo(PKMediaConfig pkMediaConfig) {
        // 0th element denotes partnerId
        // 1st element denotes if we need to send the Kava impression or not.
        // 0 = don't send 1 = send it
        int[] impressionResponse = new int[] {-1,0};

        if (pkMediaConfig.getMediaEntry() != null &&
                pkMediaConfig.getMediaEntry().getMetadata() != null &&
                pkMediaConfig.getMediaEntry().getMetadata().containsKey(kavaPartnerIdKey)) {

            String partnerId = pkMediaConfig.getMediaEntry().getMetadata().get(kavaPartnerIdKey);
            impressionResponse[0] = !TextUtils.isEmpty(partnerId) ? Integer.parseInt(partnerId) : 0;
            if (impressionResponse[0] <= 0) {
                impressionResponse[1] = 1; // PartnerId is coming <= 0 from BE
            }
        }

        if (!loadedPlugins.containsKey(kavaPluginKey) && impressionResponse[0] > 0) {
            impressionResponse[1] = 1; // Kava doesn't exist but partner uses BE
        } else if (!loadedPlugins.containsKey(kavaPluginKey) && impressionResponse[0] <= 0) {
            impressionResponse[1] = 1; // Neither Kava exists nor partner uses BE
        } else if (!loadedPlugins.containsKey(kavaPluginKey)) {
            impressionResponse[1] = 1; // Kava doesn't exist
        } else if (loadedPlugins.containsKey(kavaPluginKey) && impressionResponse[0] <= 0) {
            impressionResponse[1] = 1; //  Kava exists but partner does not use BE
        }

        return impressionResponse;
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
