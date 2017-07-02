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

import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.playkit.api.phoenix.APIDefines;
import com.kaltura.playkit.api.phoenix.PhoenixRequestBuilder;

import java.util.List;

/**
 * @hide
 */

public class AssetService extends PhoenixService {

    public static PhoenixRequestBuilder get(String baseUrl, String ks, String assetId, APIDefines.AssetReferenceType referenceType) {
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        params.addProperty("id", assetId);
        params.addProperty("assetReferenceType", referenceType.value);
        params.addProperty("type", referenceType.value); // sometimes request expect type as property sometimes assetReferenceType
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
     * @param assetType - {@link com.kaltura.playkit.api.phoenix.APIDefines.KalturaAssetType}
     * @param contextOptions - list of extra details to narrow search of sources
     * @return
     */
    public static PhoenixRequestBuilder getPlaybackContext(String baseUrl, String ks, String assetId,
                           APIDefines.KalturaAssetType assetType, KalturaPlaybackContextOptions contextOptions){

        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        params.addProperty("assetId", assetId);
        params.addProperty("assetType", assetType.value);
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

        private APIDefines.PlaybackContextType context;
        private String protocol;
        private String assetFileIds;

        public KalturaPlaybackContextOptions(APIDefines.PlaybackContextType context){
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
            if(context != null) {
                params.addProperty("context", context.value);
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
