package com.kaltura.magikapp.data;

import com.google.gson.JsonObject;

/**
 * Created by itanbarpeled on 27/11/2016.
 */

public abstract class ConverterPlugin {

    String pluginName;


    public abstract JsonObject toJson();


    public String getPluginName() {
        return pluginName;
    }


    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }
}
