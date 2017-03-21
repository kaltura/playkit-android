package com.kaltura.playkit;

import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Player configuration data holder.
 * This object is used by the player in order to preset desired configurations.
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 */
public class PlayerConfig {

    public final Media media = new Media();
    public final Plugins plugins = new Plugins();

    public static class Media {
        private long startPosition = 0;
        private PKMediaEntry mediaEntry;

        /**
         * Getter for start position. Default is 0.
         * Note, that start position is in seconds.
         * @return - the start position
         */
        public long getStartPosition() {
            return startPosition;
        }

        /**
         * Setter for start position.
         * Note, that start position is in seconds.
         * @param startPosition - the position from which the media should start.
         * @return - the config object.
         */
        public Media setStartPosition(long startPosition) {
            this.startPosition = startPosition;
            return this;
        }


        public PKMediaEntry getMediaEntry() {
            return mediaEntry;
        }

        public Media setMediaEntry(PKMediaEntry mediaEntry) {
            this.mediaEntry = mediaEntry;
            return this;
        }
    }
    
    public static class Plugins {

        private Map<String, JsonObject> pluginConfig = new HashMap<>();

        public void enablePlugin(String name) {
            JsonObject pluginConfig = getPluginConfig(name);
            if (pluginConfig.size() == 0) {
                pluginConfig.addProperty("enable", true);
            }
        }

        public void disablePlugin(String name) {
            pluginConfig.remove(name);
        }

        public JsonObject getPluginConfig(String pluginName) {
            JsonObject jsonObject = pluginConfig.get(pluginName);
            if (jsonObject == null) {
                jsonObject = new JsonObject();
                pluginConfig.put(pluginName, jsonObject);
            }
            return jsonObject;
        }

        public Plugins setPluginConfig(String pluginName, JsonObject config) {
            pluginConfig.put(pluginName, config);
            return this;
        }

        public Map<String, JsonObject> getPluginConfigMap() {
            return Collections.unmodifiableMap(pluginConfig);
        }
    }
}
