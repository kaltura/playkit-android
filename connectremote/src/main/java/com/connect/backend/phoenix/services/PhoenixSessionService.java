package com.connect.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.connect.backend.phoenix.PhoenixRequestBuilder;

/**
 * Created by tehilarozin on 28/11/2016.
 */

public class PhoenixSessionService extends PhoenixService {

    public static PhoenixRequestBuilder get(String baseUrl, String ks){
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);

        return new PhoenixRequestBuilder()
                .service("session")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag("session-get")
                .params(params);
    }
}
