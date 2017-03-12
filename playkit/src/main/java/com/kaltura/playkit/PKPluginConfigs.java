package com.kaltura.playkit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Noam Tamim @ Kaltura on 22/02/2017.
 */
public class PKPluginConfigs {

    private Map<String, Object> configs = new HashMap<>();

    public Object getPluginConfig(String pluginName) {
        return configs.get(pluginName);
    }

    public void setPluginConfig(String pluginName, Object settings) {
        this.configs.put(pluginName, settings);
    }

    public Map<String, Object> getPluginConfigsMap() {
        return Collections.unmodifiableMap(configs);
    }
}
