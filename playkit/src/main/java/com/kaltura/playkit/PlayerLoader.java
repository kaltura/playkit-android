package com.kaltura.playkit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    
    private List<String> getPluginNames(PlayerConfig.Plugins pluginsConfig) {

        List<String> plugins = new ArrayList<>();
        for (Map.Entry<String, JsonObject> pluginConfig : pluginsConfig.getPluginConfigMap().entrySet()) {
            String name = pluginConfig.getKey();
            plugins.add(name);
        }
        return plugins;
    }

    public void load(@NonNull PlayerConfig playerConfig) {
        Player player = new POCPlayer(context);
        player.prepare(playerConfig.media);

        AdProvider adProvider = null;
        

        for (Map.Entry<String, JsonObject> pluginConfig : playerConfig.plugins.getPluginConfigMap().entrySet()) {
            String name = pluginConfig.getKey();
            PKPlugin plugin = loadPlugin(name, player, playerConfig, messageBus, context);

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
}
