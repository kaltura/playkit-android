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
import com.kaltura.netkit.connect.request.MultiRequestBuilder;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.api.ovp.OvpConfigs;

/**
 * @hide
 */
public class OvpService {

    public static String[] getRequestConfigKeys(){
        return new String[]{"clientTag", "apiVersion", "format"};
    }

    public static JsonObject getOvpConfigParams(){
        JsonObject params = new JsonObject();
        params.addProperty("clientTag", PlayKitManager.CLIENT_TAG);
        params.addProperty("apiVersion",OvpConfigs.ApiVersion);
        params.addProperty("format",1); //json format

        return params;
    }

    public static MultiRequestBuilder getMultirequest(String baseUrl, String ks){
        return getMultirequest(baseUrl, ks, -1);
    }

    public static MultiRequestBuilder getMultirequest(String baseUrl, String ks, int partnerId){
        JsonObject ovpParams = OvpService.getOvpConfigParams();
        ovpParams.addProperty("ks", ks);
        if(partnerId > 0) {
            ovpParams.addProperty("partnerId", partnerId);
        }
        return (MultiRequestBuilder) new MultiRequestBuilder().method("POST")
                .url(baseUrl)
                .params(ovpParams)
                .service("multirequest");
    }
}
