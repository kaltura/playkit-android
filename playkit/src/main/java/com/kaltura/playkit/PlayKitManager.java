package com.kaltura.playkit;

import android.content.Context;

import com.kaltura.playkit.player.PlayerController;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Noam Tamim @ Kaltura on 13/10/2016.
 */
public class PlayKitManager {

    private static Map<String, Plugin.Factory> sPluginFactories = new HashMap<>();

    public static Player newPlayer(Context context) {
        return new POCPlayer(context);
    }

    public static Player createPlayer(Context context) {
        return new PlayerController(context);
    }

    public static void registerPlugin(Plugin.Factory pluginFactory) {
        String name = pluginFactory.getName();
        sPluginFactories.put(name, pluginFactory);
    }
    
    static Plugin newPluginInstance(String name) {
        Plugin.Factory pluginFactory = sPluginFactories.get(name);
        return pluginFactory == null ? null : pluginFactory.newInstance();
    }


}
