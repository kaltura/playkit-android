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

package com.kaltura.playkit.api.ovp.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.api.ovp.OvpRequestBuilder;

/**
 * @hide
 */

public class OvpSessionService extends OvpService {

    public static OvpRequestBuilder anonymousSession(String baseUrl, int partnerId){
        JsonObject params = new JsonObject();
        params.addProperty("widgetId", "_"+partnerId);

        return new OvpRequestBuilder()
                .service("session")
                .action("startWidgetSession")
                .method("POST")
                .url(baseUrl)
                .tag("session-startWidget")
                .params(params);
    }

    public static OvpRequestBuilder get(String baseUrl, String ks){
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);

        return new OvpRequestBuilder()
                .service("session")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag("session-get")
                .params(params);
    }

    public static OvpRequestBuilder end(String baseUrl, String ks) {
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);

        return new OvpRequestBuilder()
                .service("session")
                .action("end")
                .method("POST")
                .url(baseUrl)
                .tag("session-end")
                .params(params);
    }
}
