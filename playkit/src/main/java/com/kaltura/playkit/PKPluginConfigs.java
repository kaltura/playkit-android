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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Noam Tamim @ Kaltura on 22/02/2017.
 */
public class PKPluginConfigs implements Iterable<Map.Entry<String, Object>> {

    private Map<String, Object> configs = new HashMap<>();

    public Object getPluginConfig(String pluginName) {
        return configs.get(pluginName);
    }

    public void setPluginConfig(String pluginName, Object settings) {
        this.configs.put(pluginName, settings);
    }

    public Iterator<Map.Entry<String, Object>> iterator() {
        return configs.entrySet().iterator();
    }
}
