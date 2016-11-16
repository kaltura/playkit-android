package com.kaltura.playkit.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.backend.phoenix.PhoenixConfigs;

/**
 * Created by tehilarozin on 14/11/2016.
 */
public class PhoenixService {

    public static JsonObject getPhoenixParams(){
        JsonObject params = new JsonObject();
        params.addProperty("clientTag", PhoenixConfigs.ClientTag);
        params.addProperty("apiVersion",PhoenixConfigs.ApiVersion);

        return params;
    }
}
