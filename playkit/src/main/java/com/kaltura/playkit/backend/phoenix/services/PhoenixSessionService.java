package com.kaltura.playkit.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.backend.phoenix.PhoenixRequestBuilder;

/**
 * @hide
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

    public static PhoenixRequestBuilder switchUser(String baseUrl, String ks, String userIdToSwitch){
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        params.addProperty("userIdToSwitch", userIdToSwitch);

        return new PhoenixRequestBuilder()
                .service("session")
                .action("switchUser")
                .method("POST")
                .url(baseUrl)
                .tag("session-switchUser")
                .params(params);
    }
}
