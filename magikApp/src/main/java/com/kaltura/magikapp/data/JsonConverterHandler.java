package com.kaltura.magikapp.data;

import com.google.gson.GsonBuilder;


/**
 * Created by itanbarpeled on 17/12/2016.
 */

public class JsonConverterHandler {


    public static ConverterStandalonePlayer getConverterStandalonePlayer(String json) {

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(ConverterMediaProvider.class, new MediaProviderGsonAdapter());
        gsonBuilder.registerTypeHierarchyAdapter(ConverterPlugin.class, new PluginGsonAdapter());
        gsonBuilder.registerTypeHierarchyAdapter(ConverterAddon.class, new AddonsGsonAdapter());

        JsonStandalonePlayer standalonePlayer = gsonBuilder.create().fromJson(json, JsonStandalonePlayer.class);

        return standalonePlayer.getStandalonePlayer();

    }

}

