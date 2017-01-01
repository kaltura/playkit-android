package com.connect.backend.ovp.services;

import com.connect.backend.ovp.OvpConfigs;
import com.connect.utils.MultiRequestBuilder;
import com.google.gson.JsonObject;

/**
 * Created by tehilarozin on 14/11/2016.
 */
public class OvpService {

    public static String[] getRequestConfigKeys(){
        return new String[]{"clientTag", "apiVersion", "format"};
    }

    public static JsonObject getOvpConfigParams(){
        JsonObject params = new JsonObject();
        params.addProperty("clientTag", OvpConfigs.ClientTag);
        params.addProperty("apiVersion",OvpConfigs.ApiVersion);
        params.addProperty("format",1); //json format

        return params;
    }

    public static MultiRequestBuilder getMultirequest(String baseUrl, String ks){
        JsonObject ovpParams = OvpService.getOvpConfigParams();
        ovpParams.addProperty("ks", ks);
        return (MultiRequestBuilder) new MultiRequestBuilder().method("POST")
                .url(baseUrl)
                .params(ovpParams)
                .service("multirequest");
    }
}
