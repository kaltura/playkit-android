package com.kaltura.playkit.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.RequestBuilder;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class AssetService extends PhoenixService {

    public static RequestBuilder assetGet(String baseUrl, /*int partnerId,*/ String ks, String assetId, String referenceType) {
        /*if(TextUtils.isEmpty(ks)){
            return assetGet(baseUrl, partnerId, assetId, referenceType);
        }*/
        return new RequestBuilder()
                .service("asset")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag("asset-get")
                .params(getAssetGetReqParams(ks, assetId, referenceType));
    }

    /*static RequestBuilder assetGet(String baseUrl, int partnerId, String assetId, String referenceType) {
        return new MultiRequestBuilder(OttUserService.anonymousLogin(baseUrl, partnerId),
                new RequestBuilder()
                        .params(getAssetGetReqParams("{1:result:ks}", assetId, referenceType)) //on http://52.210.223.65:8080/v4_0/api_v3 its without the {}
                        .addParam("service","asset")
                        .addParam("action","get"))
                .method("POST")
                .tag("asset-multi-get")
                .url(baseUrl)
                .service("multirequest");
    }*/


    private static JsonObject getAssetGetReqParams(String ks, String assetId, String referenceType) {
        JsonObject getParams = getPhoenixParams();
        getParams.addProperty("ks", ks);
        getParams.addProperty("id", assetId);
        getParams.addProperty("assetReferenceType", referenceType);
        getParams.addProperty("type", referenceType); // sometimes request expect type as property sometimes assetReferenceType

        // needed to make sure response will retrieve the media file no matter if apiVersion property supplied or not
        getParams.addProperty("with","[{\"type\": \"files\"}]");/*"objectType": "KalturaCatalogWithHolder"*/

        return getParams;
    }

}
