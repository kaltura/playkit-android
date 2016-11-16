package com.kaltura.playkit.backend.phoenix.services;

import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.MultiRequestBuilder;
import com.kaltura.playkit.connect.RequestBuilder;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class AssetService extends PhoenixService {

    public static RequestBuilder assetGet(String baseUrl, int partnerId, String ks, String assetId, String referenceType) {
        if(TextUtils.isEmpty(ks)){
            return assetGet(baseUrl, partnerId, assetId, referenceType);
        }
        return new RequestBuilder()
                .service("asset")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag("asset-get")
                .params(getAssetGetReqParams(ks, assetId, referenceType, partnerId));
    }

    static RequestBuilder assetGet(String baseUrl, int partnerId, String assetId, String referenceType) {
        return new MultiRequestBuilder(OttUserService.anonymousLogin(baseUrl, partnerId),
                new RequestBuilder().params(getAssetGetReqParams("{1:result:ks}", assetId, referenceType, partnerId)))
                .method("POST")
                .tag("asset-multi-get");
    }


    /*private static JsonObject getAssetMultiParams(int partnerId, String assetId, String referenceType) {
        JsonObject body = new JsonObject();
        anonymousLogin
        body.add("1", getAnonymousReqParams(partnerId).params);
        String ks = "{1:result:ks}";
        body.add("2", getAssetGetReqParams(ks, assetId, referenceType));
        return body;
    }*/

    private static JsonObject getAssetGetReqParams(String ks, String assetId, String referenceType, int partnerId) {
        JsonObject getParams = getPhoenixParams();
        getParams.addProperty("ks", ks);
        getParams.addProperty("id", assetId);
        getParams.addProperty("assetReferenceType", referenceType);
        getParams.addProperty("", partnerId);

        /*"with"    :  [
        {
           "type": "files",
           "objectType": "KalturaCatalogWithHolder"
        }
     ]*/

        return getParams;
    }

    /*private static JsonObject getAnonymousReqParams(int partnerId) {
        JsonObject params = new JsonObject();
        params.addProperty("service", "ottUser");
        params.addProperty("action", "anonymousLogin");
        params.addProperty("partnerId", partnerId);
        return params;
    }*/

}
