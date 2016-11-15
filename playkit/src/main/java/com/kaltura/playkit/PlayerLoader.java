package com.kaltura.playkit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;


class LoadedPlugin {
    public LoadedPlugin(PKPlugin plugin, PlayerDecorator decorator) {
        this.plugin = plugin;
        this.decorator = decorator;
    }

    PKPlugin plugin;
    PlayerDecorator decorator;

}

class PlayerLoader extends PlayerDecoratorBase {
    private static final String TAG = "PlayerLoader";
    private Context context;
    private MessageBus messageBus;
    
    private Map<String, LoadedPlugin> loadedPlugins = new LinkedHashMap<>();

    PlayerLoader(Context context) {
        this.context = context;
        messageBus = new MessageBus(context);
    }
    
    private List<String> getPluginNames(PlayerConfig.Plugins pluginsConfig) {

        List<String> plugins = new ArrayList<>();
        for (Map.Entry<String, JSONObject> pluginConfig : pluginsConfig.getPluginConfigMap().entrySet()) {
            String name = pluginConfig.getKey();
            plugins.add(name);
        }
        return plugins;
    }

    public void load(@NonNull PlayerConfig playerConfig) {
        Player player = new POCPlayer(context);
        player.prepare(playerConfig.media);

        PlayerDecorator selectedDecorator = null;

        for (Map.Entry<String, JSONObject> pluginConfig : playerConfig.plugins.getPluginConfigMap().entrySet()) {
            String name = pluginConfig.getKey();
            PKPlugin plugin = loadPlugin(name, player, playerConfig, messageBus, context);

            if (plugin == null) {
                Log.w(TAG, "Plugin not found: " + name);
                continue;
            }
            
            // Check if the plugin provides a PlayerDecorator.
            PlayerDecorator decorator = plugin.getPlayerDecorator();
            if (decorator != null) {
                if (selectedDecorator != null) {
                    throw new IllegalStateException("Only one decorator allowed");
                }
                selectedDecorator = decorator;
                selectedDecorator.setPlayer(player);
            }

            loadedPlugins.put(name, new LoadedPlugin(plugin, decorator));
        }

        if (selectedDecorator != null) {
            player = selectedDecorator;
        }
        setPlayer(player);
    }

//    public void update(@NonNull PlayerConfig playerConfig) {
//        
//        // Handle the simple case: same list of plugins (same order)
//        if (getPluginNames(playerConfig.plugins).equals(new ArrayList<>(mLoadedPlugins.keySet()))) {
//            updatePluginConfig(playerConfig);
//        } else {
//            // reload everything.
//            releasePlugins();
//            releasePlayer();
//            
//            load(playerConfig);
//        }
//    }

    @Override
    public void release() {
        releasePlugins();
        releasePlayer();
    }

    private void releasePlayer() {
        getPlayer().release();
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
            loadedPlugin.plugin.release();
            loadedPlugins.remove(pluginEntry.getKey());
        }
        
        setPlayer(currentLayer);
    }

    private void updatePluginConfig(PlayerConfig playerConfig) {
        for (Map.Entry<String, LoadedPlugin> entry : loadedPlugins.entrySet()) {
            entry.getValue().plugin.update(playerConfig);
        }
    }

    private PKPlugin loadPlugin(String name, Player player, PlayerConfig config, MessageBus messageBus, Context context) {
        PKPlugin plugin = PlayKitManager.createPlugin(name);
        if (plugin != null) {
            plugin.load(player, config.media, config.plugins.getPluginConfig(name), messageBus, context);
        }
        return plugin;
    }

}
