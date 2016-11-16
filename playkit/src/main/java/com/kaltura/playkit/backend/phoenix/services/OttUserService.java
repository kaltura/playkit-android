package com.kaltura.playkit.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.RequestBuilder;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class OttUserService extends PhoenixService {

    public static RequestBuilder userLogin(String baseUrl, int partnerId, String username, String password){
        return userLogin(baseUrl, partnerId, username, password, null);
    }

    public static RequestBuilder userLogin(String baseUrl, int partnerId, String username, String password, String udid){
        return new RequestBuilder()
                .service("ottUser")
                .action("login")
                .method("POST")
                .url(baseUrl)
                .tag("asset-multi-get")
                .params(getLoginReqParams(partnerId, username, password, udid));
    }

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
        JsonObject params = getPhoenixParams();
        params.addProperty("partnerId", partnerId);
        return params;
    }

    public static JsonObject getLoginReqParams(int partnerId, String username, String password, String udid) {
        JsonObject params = getPhoenixParams();
        params.addProperty("partnerId", partnerId);
        params.addProperty("username", username);
        params.addProperty("password", password);
        if(udid != null){
            params.addProperty("udid", udid);
        }
        return params;
    }
}
