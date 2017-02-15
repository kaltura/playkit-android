package com.kaltura.playkit.backend.phoenix.services;

import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.playkit.backend.phoenix.APIDefines;
import com.kaltura.playkit.backend.phoenix.PhoenixRequestBuilder;

import java.util.List;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class AssetService extends PhoenixService {

    public static PhoenixRequestBuilder get(String baseUrl, String ks, String assetId, @APIDefines.AssetReferenceType String referenceType) {
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        params.addProperty("id", assetId);
        params.addProperty("assetReferenceType", referenceType);
        params.addProperty("type", referenceType); // sometimes request expect type as property sometimes assetReferenceType
        // needed to make sure response will retrieve the media file no matter if apiVersion property supplied or not
        params.addProperty("with","[{\"type\": \"files\"}]");

        return new PhoenixRequestBuilder()
                .service("asset")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag("asset-get")
                .params(params);
    }

    /**
     * builds the request for detailed asset sources data, including DRM data if has any.
     * @param baseUrl - api base server url
     * @param ks - valid session token
     * @param assetId - Asset id
     * @param assetType - {@link com.kaltura.playkit.backend.phoenix.APIDefines.KalturaAssetType}
     * @param contextOptions - list of extra details to narrow search of sources
     * @return
     */
    public static PhoenixRequestBuilder getPlaybackContext(String baseUrl, String ks, String assetId,
                           @APIDefines.KalturaAssetType String assetType, KalturaPlaybackContextOptions contextOptions){

        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        params.addProperty("assetId", assetId);
        params.addProperty("assetType", assetType);
        params.add("contextDataParams", contextOptions != null ? contextOptions.toJson() : new JsonObject());

        return new PhoenixRequestBuilder()
                .service("asset")
                .action("getPlaybackContext")
                .method("POST")
                .url(baseUrl)
                .tag("asset-getPlaybackContext")
                .params(params);
    }


    public static class KalturaPlaybackContextOptions{

        String context;
        String protocol;
        String assetFileIds;

        public KalturaPlaybackContextOptions(@APIDefines.PlaybackContextType String context){
            this.context = context;
        }

        public KalturaPlaybackContextOptions setMediaProtocol(String protocol){
            this.protocol = protocol;
            return this;
        }

        public KalturaPlaybackContextOptions setMediaFileIds(String ids){
            this.assetFileIds = ids;
            return this;
        }

        public KalturaPlaybackContextOptions setMediaFileIds(List<String> ids){
            this.assetFileIds = TextUtils.join(",", ids);
            return this;
        }

        public JsonObject toJson(){
            JsonObject params = new JsonObject();
            if(!TextUtils.isEmpty(context)) {
                params.addProperty("context", context);
            }

            if(!TextUtils.isEmpty(protocol)) {
                params.addProperty("mediaProtocol", protocol);
            }
            if(!TextUtils.isEmpty(assetFileIds)) {
                params.addProperty("assetFileIds", assetFileIds);
            }

            return params;
        }
    }

}
