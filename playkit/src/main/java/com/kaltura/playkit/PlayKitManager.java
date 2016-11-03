package com.kaltura.playkit;

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

    static PKPlugin createPlugin(String name, PlayKit playKit) {
        PKPlugin.Factory pluginFactory = sPluginFactories.get(name);
        return pluginFactory == null ? null : pluginFactory.newInstance(playKit);
    }

}

