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

import com.google.gson.JsonObject;
import com.kaltura.playkit.api.phoenix.PhoenixRequestBuilder;

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
