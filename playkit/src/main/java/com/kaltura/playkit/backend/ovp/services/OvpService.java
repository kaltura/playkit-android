package com.kaltura.playkit.backend.ovp.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.backend.ovp.OvpConfigs;

/**
 * Created by tehilarozin on 14/11/2016.
 */
public class OvpService {

    public static JsonObject getOvpParams(){
        JsonObject params = new JsonObject();
        params.addProperty("clientTag", OvpConfigs.ClientTag);
        params.addProperty("apiVersion",OvpConfigs.ApiVersion);

        return params;
    }
}
