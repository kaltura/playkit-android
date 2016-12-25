package com.kaltura.playkit.backend.phoenix.services;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.playkit.backend.phoenix.PhoenixRequestBuilder;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class OttUserService extends PhoenixService {

    public static PhoenixRequestBuilder userLogin(String baseUrl, int partnerId, String username, String password){
        return userLogin(baseUrl, partnerId, username, password, null);
    }

    public static PhoenixRequestBuilder userLogin(String baseUrl, int partnerId, String username, String password, @Nullable String udid){
        JsonObject params = new JsonObject();
        params.addProperty("partnerId", partnerId);
        params.addProperty("username", username);
        params.addProperty("password", password);
        if(udid != null){
            params.addProperty("udid", udid);
        }

        return new PhoenixRequestBuilder()
                .service("ottUser")
                .action("login")
                .method("POST")
                .url(baseUrl)
                .tag("ottuser-login")
                .params(params);
    }

    public static PhoenixRequestBuilder anonymousLogin(String baseUrl, int partnerId, @Nullable String udid){
        JsonObject params = new JsonObject();
        params.addProperty("partnerId", partnerId);
        if(udid != null){
            params.addProperty("udid", udid);
        }

        return new PhoenixRequestBuilder()
                .service("ottUser")
                .action("anonymousLogin")
                .method("POST")
                .url(baseUrl)
                .tag("ottuser-anonymous-login")
                .params(params);
    }

    public static PhoenixRequestBuilder refreshSession(String baseUrl, String ks, String refreshToken, @Nullable String udid){
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        params.addProperty("refreshToken", refreshToken);
        if(!TextUtils.isEmpty(udid)) {
            params.addProperty("udid", udid);
        }

        return new PhoenixRequestBuilder()
                .service("ottUser")
                .action("refreshSession")
                .method("POST")
                .url(baseUrl)
                .tag("ottuser-refresh-session")
                .params(params);
    }

    public static PhoenixRequestBuilder logout(String baseUrl, String ks, @Nullable String udid){//?? check if udid needed here
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        if(!TextUtils.isEmpty(udid)) {
            params.addProperty("udid", udid);
        }

        return new PhoenixRequestBuilder()
                .service("ottUser")
                .action("logout")
                .method("POST")
                .url(baseUrl)
                .tag("ottuser-logout")
                .params(params);
    }

}
