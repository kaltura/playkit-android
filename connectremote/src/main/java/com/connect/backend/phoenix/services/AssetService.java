package com.connect.backend.phoenix.services;

import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.connect.backend.phoenix.PhoenixRequestBuilder;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class AssetService extends PhoenixService {

    public static PhoenixRequestBuilder assetGet(String baseUrl, String ks, String assetId, String referenceType) {
        return new PhoenixRequestBuilder()
                .service("asset")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag("asset-get")
                .params(getAssetGetReqParams(ks, assetId, referenceType));
    }

    private static JsonObject getAssetGetReqParams(String ks, String assetId, String referenceType) {
        JsonObject getParams = new JsonObject();
        getParams.addProperty("ks", ks);
        getParams.addProperty("id", assetId);
        getParams.addProperty("assetReferenceType", referenceType);
        getParams.addProperty("type", referenceType); // sometimes request expect type as property sometimes assetReferenceType

        // needed to make sure response will retrieve the media file no matter if apiVersion property supplied or not
        getParams.addProperty("with","[{\"type\": \"files\"}]");/*"objectType": "KalturaCatalogWithHolder"*/

        return getParams;
    }

    public static PhoenixRequestBuilder listByChannel(String baseUrl, String ks, int channelId, String sql) {
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        params.addProperty("idEqual", channelId);
        if(!TextUtils.isEmpty(sql)) {
            params.addProperty("kSql", sql); //"(and tagName:(:->in)'some tags seperated with comma')
        }
        // needed to make sure response will retrieve the media file no matter if apiVersion property supplied or not
        params.addProperty("with","[{\"type\": \"files\"}]");/*"objectType": "KalturaCatalogWithHolder"*/

        return new PhoenixRequestBuilder()
                .service("asset")
                .action("list")
                .method("POST")
                .url(baseUrl)
                .tag("asset-get")
                .params(params);
    }

}
