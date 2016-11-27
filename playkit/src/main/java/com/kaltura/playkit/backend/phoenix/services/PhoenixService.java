package com.kaltura.playkit.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.backend.phoenix.PhoenixConfigs;
import com.kaltura.playkit.connect.MultiRequestBuilder;

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

    public static MultiRequestBuilder getMultirequest(String baseUrl, String ks){
        JsonObject params = getPhoenixParams();
        params.addProperty("ks", ks);
        return (MultiRequestBuilder) new MultiRequestBuilder().service("multirequest").method("POST").url(baseUrl).params(params);
    }
}
