package com.kaltura.playkit.backend.phoenix.services;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.RequestBuilder;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class OttUserService extends PhoenixService {

    public static RequestBuilder userLogin(String baseUrl, int partnerId, String username, String password){
        return userLogin(baseUrl, partnerId, username, password, null);
    }

    public static RequestBuilder userLogin(String baseUrl, int partnerId, String username, String password, @Nullable String udid){
        return new RequestBuilder()
                .service("ottUser")
                .action("login")
                .method("POST")
                .url(baseUrl)
                .tag("ottuser-login")
                .params(getLoginReqParams(partnerId, username, password, udid));
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

    public static RequestBuilder anonymousLogin(String baseUrl, int partnerId){
        return new RequestBuilder()
                .service("ottUser")
                .action("anonymousLogin")
                .method("POST")
                .url(baseUrl)
                .tag("ottuser-anonymous-login")
                .params(getAnonymousReqParams(partnerId));
    }

    public static JsonObject getAnonymousReqParams(int partnerId) {
        JsonObject params = getPhoenixParams();
        params.addProperty("partnerId", partnerId);
        return params;
    }

    public static RequestBuilder refreshSession(String baseUrl, String refreshToken, @Nullable String udid){
        return new RequestBuilder()
                .service("ottUser")
                .action("refreshSession")
                .method("POST")
                .url(baseUrl)
                .tag("ottuser-refresh-session")
                .params(getRefreshReqParams(refreshToken, udid));
    }

    private static JsonObject getRefreshReqParams(String refreshToken, String udid) {
        JsonObject params = getPhoenixParams();
        params.addProperty("refreshToken", refreshToken);
        if(!TextUtils.isEmpty(udid)) {
            params.addProperty("udid", udid);
        }
        return params;
    }

    public static RequestBuilder logout(String baseUrl, String ks, @Nullable String udid){//?? check if udid needed here
        JsonObject params = getPhoenixParams();
        params.addProperty("ks", ks);
        if(!TextUtils.isEmpty(udid)) {
            params.addProperty("udid", udid);
        }

        return new RequestBuilder()
                .service("ottUser")
                .action("logout")
                .method("POST")
                .url(baseUrl)
                .tag("ottuser-logout")
                .params(params);
    }




}
