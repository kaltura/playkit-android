package com.connect.backend.tvpapi.services;

import com.connect.utils.RequestBuilder;
import com.google.gson.JsonObject;

/**
 * Created by zivilan on 08/12/2016.
 */

public class MediaMarkService {
    public static RequestBuilder sendTVPAPIEVent(String baseUrl, JsonObject initObj, String action, String assetId, String fileId, long position) {
        return new RequestBuilder()
                .method("POST")
                .url(baseUrl)
                .tag("media-action")
                .params(buildRequestBody(initObj, action, assetId,  fileId,  position));
    }

    private static JsonObject buildRequestBody(JsonObject initObj, String action, String assetId, String fileId, long position) {
        JsonObject params = new JsonObject();
        params.add("initObj", initObj);
        if (!action.equals("hit")) {
            params.addProperty("Action", action);
        }
        params.addProperty("mediaType", 0);
        params.addProperty("iMediaID", assetId);
        params.addProperty("iFileID", fileId);
        params.addProperty("iLocation", position);
        return params;
    }
}
