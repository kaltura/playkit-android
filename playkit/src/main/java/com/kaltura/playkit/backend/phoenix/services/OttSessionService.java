package com.kaltura.playkit.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.RequestBuilder;

/**
 * Created by tehilarozin on 28/11/2016.
 */

public class OttSessionService extends PhoenixService {

    public static RequestBuilder get(String baseUrl, String ks){
        JsonObject params = getPhoenixParams();
        params.addProperty("ks", ks);

        return new RequestBuilder()
                .service("session")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag("session-get")
                .params(params);
    }
}
