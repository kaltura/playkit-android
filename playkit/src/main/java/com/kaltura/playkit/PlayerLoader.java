package com.kaltura.playkit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kaltura.playkit.player.PlayerController;

import java.util.LinkedHashMap;
import java.util.Map;


class PlayerLoader extends PlayerDecorator {
    private static final String TAG = "PlayerLoader";
    private Context context;
    private MessageBus messageBus;
    
    private Map<String, LoadedPlugin> loadedPlugins = new LinkedHashMap<>();

    private static class LoadedPlugin {
        private LoadedPlugin(PKPlugin plugin) {
            this.plugin = plugin;
        }

        PKPlugin plugin;
    }

    PlayerLoader(Context context) {
        this.context = context;
        messageBus = new MessageBus(context);
    }
    
    public void load(@NonNull PlayerConfig playerConfig) {
        Player player = new PlayerController(context, playerConfig);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                messageBus.post(event);
            }
        });

        AdProvider adProvider = null;
        

        for (Map.Entry<String, JsonObject> pluginConfig : playerConfig.plugins.getPluginConfigMap().entrySet()) {
            String name = pluginConfig.getKey();
            PKPlugin plugin = loadPlugin(name, this, playerConfig, messageBus, context);

            if (plugin == null) {
                Log.w(TAG, "Plugin not found: " + name);
                continue;
            }
            
            // Check if the plugin is an AdProvider.
            if (plugin instanceof AdProvider) {
                if (adProvider != null) {
                    throw new IllegalStateException("Only one ad provider allowed");
                }
                adProvider = (AdProvider) plugin;
            }

            loadedPlugins.put(name, new LoadedPlugin(plugin));
        }
        
        if (adProvider != null) {
            PlayerDecorator decorator = new AdEnabledPlayerDecorator(adProvider);
            decorator.setPlayer(player);
        }

        setPlayer(player);
    }

    @Override
    public void release() {
        releasePlugins();
        releasePlayer();
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable JsonElement value) {
        LoadedPlugin loadedPlugin = loadedPlugins.get(pluginName);
        if (loadedPlugin != null) {
            loadedPlugin.plugin.onUpdateConfig(key, value);
        }
    }
    
    public void restore() {
        getPlayer().restore();
    }

    private void releasePlayer() {
        getPlayer().release();
    }

    private void releasePlugins() {
        for (Map.Entry<String, LoadedPlugin> loadedPluginEntry : loadedPlugins.entrySet()) {
            loadedPluginEntry.getValue().plugin.onDestroy();
        }
    }

    private PKPlugin loadPlugin(String name, Player player, PlayerConfig config, MessageBus messageBus, Context context) {
        PKPlugin plugin = PlayKitManager.createPlugin(name);
        if (plugin != null) {
            plugin.onLoad(player, config.media, config.plugins.getPluginConfig(name), messageBus, context);
        }
        return plugin;
    }

    @Override
    public void addEventListener(@NonNull PKEvent.Listener listener, PKEvent... events) {
        messageBus.listen(listener, events);
    }
}
