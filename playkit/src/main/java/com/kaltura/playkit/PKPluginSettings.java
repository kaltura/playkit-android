package com.kaltura.playkit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Noam Tamim @ Kaltura on 22/02/2017.
 */
public class PKPluginSettings {

    private Map<String, Object> settings = new HashMap<>();

    public Object getPluginSettings(String pluginName) {
        return settings.get(pluginName);
    }

    public void setPluginSettings(String pluginName, Object settings) {
        this.settings.put(pluginName, settings);
    }

    public Map<String, Object> getPluginSettingsMap() {
        return Collections.unmodifiableMap(settings);
    }
}
