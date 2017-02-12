package com.kaltura.playkit.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.backend.phoenix.PhoenixRequestBuilder;

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

}
