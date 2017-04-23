package com.kaltura.playkit.api.ovp.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.api.ovp.OvpRequestBuilder;

/**
 * @hide
 */
public class MetaDataService extends OvpService {

    public static OvpRequestBuilder list(String baseUrl, String ks, String entryId) {
        JsonObject filter = new JsonObject();
        filter.addProperty("objectType","KalturaMetadataFilter");
        filter.addProperty("objectIdEqual",entryId);
        filter.addProperty("metadataObjectTypeEqual","1");

        JsonObject params = new JsonObject();
        params.add("filter", filter);
        params.addProperty("ks", ks);

        return new OvpRequestBuilder().service("metadata_metadata")
                .action("list")
                .method("POST")
                .url(baseUrl)
                .tag("metadata_metadata-list")
                .params(params);
    }
}
