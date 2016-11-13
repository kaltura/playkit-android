package com.kaltura.playkit.mediaproviders.phoenix;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.RequestBuilder;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class OttUser {

    public static RequestBuilder anonymousLogin(String baseUrl, int partnerId){
        return new RequestBuilder()
                .service("ottUser")
                .action("anonymousLogin")
                .method("POST")
                .url(baseUrl)
                .tag("asset-multi-get")
                .params(getAnonymousReqParams(partnerId));
    }

    public static JsonObject getAnonymousReqParams(int partnerId) {
        JsonObject params = new JsonObject();
        params.addProperty("partnerId", partnerId);
        return params;
    }
}
