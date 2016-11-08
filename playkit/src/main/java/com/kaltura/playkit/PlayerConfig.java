package com.kaltura.playkit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 */
public class PlayerConfig {

    public final Media media = new Media();
    public final Plugins plugins = new Plugins();

    public static class Media {
        private boolean mAutoPlay = false;
        private long mStartPosition = 0;
        private PKMediaEntry mMediaEntry;

        public boolean isAutoPlay() {
            return mAutoPlay;
        }

        public Media setAutoPlay(boolean autoPlay) {
            this.mAutoPlay = autoPlay;
            return this;
        }

        public long getStartPosition() {
            return mStartPosition;
        }

        public Media setStartPosition(long startPosition) {
            this.mStartPosition = startPosition;
            return this;
        }

        public PKMediaEntry getMediaEntry() {
            return mMediaEntry;
        }

        public Media setMediaEntry(PKMediaEntry mediaEntry) {
            this.mMediaEntry = mediaEntry;
            return this;
        }
    }
    
    public static class Plugins {

        private Map<String, JSONObject> pluginConfig = new HashMap<>();

        public void enablePlugin(String name) {
            JSONObject pluginConfig = getPluginConfig(name);
            if (pluginConfig.length() == 0) {
                try {
                    pluginConfig.put("enable", true);
                } catch (JSONException e) {
                    Assert.failState("Failed to enable plugin: " + e);
                }
            }
        }

        public void disablePlugin(String name) {
            pluginConfig.remove(name);
        }

        public JSONObject getPluginConfig(String pluginName) {
            JSONObject jsonObject = pluginConfig.get(pluginName);
            if (jsonObject == null) {
                jsonObject = new JSONObject();
                pluginConfig.put(pluginName, jsonObject);
            }
            return jsonObject;
        }

        public Plugins setPluginConfig(String pluginName, JSONObject config) {
            pluginConfig.put(pluginName, config);
            return this;
        }

        public Map<String, JSONObject> getPluginConfigMap() {
            return Collections.unmodifiableMap(pluginConfig);
        }
    }
}
