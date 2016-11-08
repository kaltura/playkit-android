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
        mPlugin = plugin;
        mDecorator = decorator;
    }

    PKPlugin mPlugin;
    PlayerDecorator mDecorator;

}

class PlayerLoader extends PlayerDecoratorBase {
    private static final String TAG = "PlayerLoader";
    private Context mContext;
    private MessageBus mMessageBus;
    
    private Map<String, LoadedPlugin> mLoadedPlugins = new LinkedHashMap<>();

    PlayerLoader(Context context) {
        mContext = context;
        mMessageBus = new MessageBus(context);
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
        Player player = new POCPlayer(mContext);
        player.prepare(playerConfig.media);

        PlayerDecorator selectedDecorator = null;

        for (Map.Entry<String, JSONObject> pluginConfig : playerConfig.plugins.getPluginConfigMap().entrySet()) {
            String name = pluginConfig.getKey();
            PKPlugin plugin = loadPlugin(name, player, playerConfig, mMessageBus, mContext);

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

            mLoadedPlugins.put(name, new LoadedPlugin(plugin, decorator));
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
        List<Map.Entry<String, LoadedPlugin>> plugins = new ArrayList<>(mLoadedPlugins.entrySet());
        ListIterator<Map.Entry<String, LoadedPlugin>> listIterator = plugins.listIterator(plugins.size());
        
        Player currentLayer = getPlayer();
        
        while (listIterator.hasPrevious()) {
            Map.Entry<String, LoadedPlugin> pluginEntry = listIterator.previous();
            LoadedPlugin loadedPlugin = pluginEntry.getValue();
            
            // Peel off decorator, if this plugin added one
            if (loadedPlugin.mDecorator != null) {
                Assert.checkState(loadedPlugin.mDecorator == currentLayer, "Decorator/layer mismatch");
                if (currentLayer instanceof PlayerDecorator) {
                    currentLayer = ((PlayerDecorator) currentLayer).getPlayer();
                }
            }
            
            // Release the plugin
            loadedPlugin.mPlugin.release();
            mLoadedPlugins.remove(pluginEntry.getKey());
        }
        
        setPlayer(currentLayer);
    }

    private void updatePluginConfig(PlayerConfig playerConfig) {
        for (Map.Entry<String, LoadedPlugin> entry : mLoadedPlugins.entrySet()) {
            entry.getValue().mPlugin.update(playerConfig);
        }
    }

    private PKPlugin loadPlugin(String name, Player player, PlayerConfig config, MessageBus messageBus, Context context) {
        PKPlugin plugin = PlayKitManager.createPlugin(name);
        if (plugin != null) {
            plugin.load(player, config, messageBus, context);
        }
        return plugin;
    }

}
