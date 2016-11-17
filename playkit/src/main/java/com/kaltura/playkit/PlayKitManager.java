package com.kaltura.playkit;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Noam Tamim @ Kaltura on 13/10/2016.
 */
public class PlayKitManager {
    private static Map<String, PKPlugin.Factory> sPluginFactories = new HashMap<>();

    public static void registerPlugins(PKPlugin.Factory ... pluginFactories) {
        for (PKPlugin.Factory factory : pluginFactories) {
            String name = factory.getName();
            sPluginFactories.put(name, factory);
        }
    }

    static PKPlugin createPlugin(String name) {
        PKPlugin.Factory pluginFactory = sPluginFactories.get(name);
        return pluginFactory == null ? null : pluginFactory.newInstance();
    }

    public static Player loadPlayer(PlayerConfig playerConfig, Context context) {
        PlayerLoader playerLoader = new PlayerLoader(context);
        playerLoader.load(playerConfig);
        return playerLoader;
    }
}

