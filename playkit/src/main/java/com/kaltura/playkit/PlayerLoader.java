package com.kaltura.playkit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.kaltura.playkit.player.PlayerController;

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
        this.messageBus = new MessageBus(context);
    }
    
    public void load(@NonNull PlayerConfig playerConfig) {
        PlayerController playerController = new PlayerController(context, playerConfig.media);
        
        playerController.setEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                messageBus.post(event);
            }
        });

        Player player = playerController;

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
                decorator.setPlayer(player);
                player = decorator;
            }

            loadedPlugins.put(name, new LoadedPlugin(plugin, decorator));
        }

        setPlayer(player);
    }

    @Override
    public void release() {
        releasePlugins();
        releasePlayer();
    }

    @Override
    public void restore() {
        getPlayer().restore();
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
            loadedPlugin.plugin.onDestroy();
            loadedPlugins.remove(pluginEntry.getKey());
        }
        
        setPlayer(currentLayer);
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

    @Override
    public void addStateChangeListener(@NonNull final PlayerState.Listener listener) {
        messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                if (event instanceof PlayerState.Event) {
                    listener.onPlayerStateChanged(PlayerLoader.this, ((PlayerState.Event)event).newState);
                }
            }
        }, PlayerState.EVENT_TYPE);
    }
}
