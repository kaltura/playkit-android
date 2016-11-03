package com.kaltura.playkit;

import android.content.Context;

import com.kaltura.playkit.player.PlayerController;

import org.json.JSONObject;

import java.util.Map;

public class PlayKit {
    public Player createPlayer(Context context, PlayerConfig config) {
        PlayerController player = new PlayerController(context);
        player.load(config);
        
        Player decoratedPlayer = null;

        for (Map.Entry<String, JSONObject> pluginConfig : config.getPluginConfigMap().entrySet()) {
            PKPlugin plugin = loadPlugin(pluginConfig.getKey(), player, config, context);
            
            if (plugin instanceof DecoratedPlayerProvider) {
                Player decorator = ((DecoratedPlayerProvider) plugin).getDecoratedPlayer();
                if (decorator != null) {
                    if (decoratedPlayer != null) {
                        throw new IllegalStateException("Only one decorator allowed");
                    }
                    decoratedPlayer = decorator;
                }
            }
        }

        return decoratedPlayer != null ? decoratedPlayer : player;
    }

    private PKPlugin loadPlugin(String name, Player player, PlayerConfig config, Context context) {
        PKPlugin plugin = PlayKitManager.createPlugin(name, this);
        if (plugin != null) {
            plugin.load(player, config, context);
        }
        return plugin;
    }

}
