/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.content.Context;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.profiler.PlayKitProfiler;

import java.util.HashMap;
import java.util.Map;

public class PlayKitManager {

    private static final PKLog log = PKLog.get("PlayKitManager");


    public static final String VERSION_STRING = BuildConfig.VERSION_NAME;
    public static final String CLIENT_TAG = "playkit/android-" + VERSION_STRING;

    private static boolean shouldSendDeviceCapabilitiesReport = true;


    static {
        PKLog.i("PlayKitManager", "PlayKit " + VERSION_STRING);
    }


    private static Map<String, PKPlugin.Factory> sPluginFactories = new HashMap<>();

    public static void registerPlugins(Context context, PKPlugin.Factory... pluginFactories) {
        Context applicationContext = context != null ? context.getApplicationContext() : null;
        for (PKPlugin.Factory factory : pluginFactories) {
            String name = factory.getName();
            if (applicationContext != null && sPluginFactories.put(name, factory) == null) {
                factory.warmUp(applicationContext);
            }
        }
    }

    static PKPlugin createPlugin(String name) {
        PKPlugin.Factory pluginFactory = sPluginFactories.get(name);
        return pluginFactory == null ? null : pluginFactory.newInstance();
    }

    public static Player loadPlayer(Context context, @Nullable PKPluginConfigs pluginConfigs, MessageBus messageBus) {
        MediaSupport.initializeDrm(context, null);

        if (shouldSendDeviceCapabilitiesReport) {
            PKDeviceCapabilities.maybeSendReport(context);
        }

        PlayerLoader playerLoader = new PlayerLoader(context, messageBus);
        playerLoader.load(pluginConfigs != null ? pluginConfigs : new PKPluginConfigs());
        return playerLoader;
    }

    public static Player loadPlayer(Context context, @Nullable PKPluginConfigs pluginConfigs) {
        return loadPlayer(context, pluginConfigs, null);
    }

    public static void disableDeviceCapabilitiesReport() {
        shouldSendDeviceCapabilitiesReport = false;
    }

    public static final class ProfilerConfig {
        public String postURL;
        public float sendPercentage;

        private ProfilerConfig() {}
    }
}

