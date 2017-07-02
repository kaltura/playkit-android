/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.api.phoenix.services;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.playkit.api.phoenix.PhoenixRequestBuilder;

/**
 * @hide
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

    public static PhoenixRequestBuilder socialLogin(@NonNull String baseUrl, int partnerId, @NonNull String token, String socialNetwork, @Nullable String udid) {
        JsonObject params = new JsonObject();
        params.addProperty("token", token);
        params.addProperty("partnerId", partnerId);
        params.addProperty("type", socialNetwork);
        if(udid != null) {
            params.addProperty("udid", udid);
        }

        return new PhoenixRequestBuilder()
                .service("social")
                .action("login")
                .method("POST")
                .url(baseUrl)
                .tag("social-login")
                .params(params);
    }

    public enum KalturaSocialNetwork{
        FACEBOOK("facebook");

        public String value;

        KalturaSocialNetwork(String value){
            this.value = value;
        }
    }
}
