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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public abstract class PKPlugin {

    public interface Factory {
        String getName();
        String getVersion();
        PKPlugin newInstance();
        void warmUp(Context context);
    }

    protected abstract void onLoad(Player player, Object config, MessageBus messageBus, Context context);
    protected abstract void onUpdateMedia(PKMediaConfig mediaConfig);
    protected abstract void onUpdateConfig(Object config);
    protected abstract void onApplicationPaused();
    protected abstract void onApplicationResumed();

    protected abstract void onDestroy();

    protected PlayerDecorator getPlayerDecorator() {
        return null;
    }

    public static JsonObject replaceKeysInPluginConfig(PKMediaEntry mediaEntry, JsonObject pluginConfig) {
        String configStr = pluginConfig.getAsJsonObject().toString();
        if (mediaEntry == null || mediaEntry.getMetadata() == null) {
            return pluginConfig;
        }
        Map<String,String> metadataMap = mediaEntry.getMetadata();
        Map<String,String> replacementMap = new HashMap<>();
        for (Map.Entry<String, String> metadataEntry : metadataMap.entrySet()) {
            replacementMap.put("\\{\\{" + metadataEntry.getKey() + "\\}\\}", metadataEntry.getValue());
        }

        for (Map.Entry<String, String> replacementEntry : replacementMap.entrySet()) {
            configStr = configStr.replaceAll(replacementEntry.getKey(), replacementEntry.getValue());
        }
        JsonParser parser = new JsonParser();
        return parser.parse(configStr).getAsJsonObject();
    }
}
