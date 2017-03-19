package com.kaltura.playkit.backend.phoenix.services;

import android.support.annotation.Nullable;
import android.text.TextUtils;

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

    public static PhoenixRequestBuilder switchUser(String baseUrl, String ks, String userIdToSwitch, @Nullable String udid){
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        params.addProperty("userIdToSwitch", userIdToSwitch);
        if(!TextUtils.isEmpty(udid)){
            params.addProperty("udid", udid);
        }

        return new PhoenixRequestBuilder()
                .service("session")
                .action("switchUser")
                .method("POST")
                .url(baseUrl)
                .tag("session-switchUser")
                .params(params);
    }
}
