package com.kaltura.magikapp.data;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Created by itanbarpeled on 27/11/2016.
 */

public class PluginGsonAdapter implements JsonDeserializer <ConverterPlugin> {


    private final static String PLUGIN_NAME = "pluginName";
    private final static String PARAMS = "params";


    @Override
    public ConverterPlugin deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {


        JsonObject plugin;
        ConverterPlugin converterPlugin = null;


        if (json.isJsonObject()) {

            plugin = json.getAsJsonObject();
            String pluginName = null;
            String pluginClassName = null;

            if (plugin.has(PLUGIN_NAME)) {
                pluginName = plugin.get(PLUGIN_NAME).getAsString();
                pluginClassName = getClass().getPackage().getName() + "." + PluginMapping.valueOf(pluginName).name;

                try {

                    Class pluginClass = Class.forName(pluginClassName);
                    converterPlugin = (ConverterPlugin) new Gson().fromJson(plugin.get(PARAMS), pluginClass);
                    converterPlugin.setPluginName(pluginName);

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        }

        return converterPlugin;

    }


    private enum PluginMapping {

        IMA("ConverterIma"),
        Youbora("ConverterYoubora"),
        KalturaStats("ConverterKalturaStats"),
        PhoenixAnalytics("ConverterPhoenixAnalytics"),
        TVPAPIAnalytics("ConverterTVPAPIAnalytics"),
        KalturaLiveStats("ConverterKalturaLiveStats"),
        KalturaAnalytics("ConverterAnalytics");


        public String name;

        PluginMapping(String name) {
            this.name = name;
        }

    }
}


