package com.kaltura.playkit;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.Map;


public class PlayKit {

    public Player loadPlayer(Context context, PlayerConfig config) {

        PlayerController player = new PlayerController(context);

        PlayerDecorator playerDecorator = null;

        for (Map.Entry<String, JSONObject> pluginConfig : config.getPluginConfigMap().entrySet()) {
            String name = pluginConfig.getKey();
            PKPlugin plugin = loadPlugin(name, player, config, context);
            
            if (plugin == null) {
                Log.w("PlayKit", "Plugin not found: " + name);
                continue;
            }
            // Check if the plugin provides a PlayerDecorator.
            PlayerDecorator decorator = plugin.getPlayerDecorator();
            if (decorator != null) {
                if (playerDecorator != null) {
                    throw new IllegalStateException("Only one decorator allowed");
                }
                playerDecorator = decorator;
                playerDecorator.setPlayer(player);
            }
        }

        return playerDecorator != null ? playerDecorator : player;
    }

    private PKPlugin loadPlugin(String name, Player player, PlayerConfig config, Context context) {
        PKPlugin plugin = PlayKitManager.createPlugin(name, this);
        if (plugin != null) {
            plugin.load(player, config, context);
        }
        return plugin;
    }

}
