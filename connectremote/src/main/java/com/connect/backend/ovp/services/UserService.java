package com.connect.backend.ovp.services;

import android.support.annotation.NonNull;

import com.google.gson.JsonObject;
import com.connect.backend.ovp.OvpRequestBuilder;

/**
 * Created by tehilarozin on 07/12/2016.
 */

public class UserService extends OvpService {

    /**
     * @param baseUrl
     * @param loginId   -mandatory - user's email address that identifies the user for login
     * @param password  -mandatory - user's password
     * @param partnerId -optional - if value = 0, won't be used
     * @return
     */
    public static OvpRequestBuilder loginByLoginId(@NonNull String baseUrl, @NonNull String loginId, @NonNull String password, int partnerId) {
        JsonObject params = new JsonObject();
        params.addProperty("loginId", loginId);
        params.addProperty("password", password);
        if (partnerId > 0) {
            params.addProperty("partnerId", partnerId);
        }

        return new OvpRequestBuilder()
                .service("user")
                .action("loginByLoginId")
                .method("POST")
                .url(baseUrl)
                .tag("user-loginbyloginid")
                .params(params);
    }

}
