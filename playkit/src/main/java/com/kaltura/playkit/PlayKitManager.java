package com.kaltura.playkit;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Noam Tamim @ Kaltura on 13/10/2016.
 */
public class PlayKitManager {
    private static Map<String, Plugin.Factory> sPluginFactories = new HashMap<>();
    private static ConcurrentHashMap<String, Class<? extends Plugin>> map;

    public static Player newPlayer(Context context) {
        return new POCPlayer(context);
    }

    public static void registerPlugin(Plugin.Factory pluginFactory) {
        String name = pluginFactory.getName();
        sPluginFactories.put(name, pluginFactory);
    }
    
    static Plugin getPluginInstance(String name) {
        Plugin.Factory pluginFactory = sPluginFactories.get(name);
        return pluginFactory == null ? null : pluginFactory.newInstance();
    }
}
