package com.kaltura.playkit.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.RequestBuilder;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class LicensedUrlService extends PhoenixService {

    /**
     * both VOD and live content uses the media license request
     * @param baseUrl
     * @param ks
     * @param assetId
     * @param mediaId
     * @param mediaBaseUrl
     * @return
     */
    public static RequestBuilder getForMedia(String baseUrl, String ks, String assetId, String mediaId, String mediaBaseUrl) {
        JsonObject requestProperty = new JsonObject();
        requestProperty.addProperty("objectType", "KalturaLicensedUrlMediaRequest");
        requestProperty.addProperty("contentId", mediaId);
        requestProperty.addProperty("baseUrl", mediaBaseUrl);
        requestProperty.addProperty("assetId", assetId);

        return getLicensedLinksRequestBuilder(baseUrl, ks, "licensedlink-media-get", requestProperty);
    }

    /**
     * @param baseUrl
     * @param ks
     * @param streamType: catchup / start_over / trick_play
     * @param startDate
     * @return
     */
    public static RequestBuilder getForShiftedLive(String baseUrl, String ks, String assetId, String streamType, long startDate) {

        JsonObject requestProperty = new JsonObject();
        requestProperty.addProperty("objectType", "KalturaLicensedUrlEpgRequest");
        requestProperty.addProperty("streamType", streamType);
        requestProperty.addProperty("startDate", startDate);
        requestProperty.addProperty("assetId", assetId);

        return getLicensedLinksRequestBuilder(baseUrl, ks, "licensedlink-epg-get", requestProperty);
    }

    public static RequestBuilder getForRecording(String baseUrl, String ks, String assetId, String fileType) {

        JsonObject requestProperty = new JsonObject();
        requestProperty.addProperty("objectType", "KalturaLicensedUrlEpgRequest");
        requestProperty.addProperty("fileType", fileType); //file format (HD,SD...)
        requestProperty.addProperty("assetId", assetId);

        return getLicensedLinksRequestBuilder(baseUrl, ks, "licensedlink-rec-get", requestProperty);
    }

    private static RequestBuilder getLicensedLinksRequestBuilder(String baseUrl, String ks, String tag, JsonObject requestProperty) {
        JsonObject reqParams = getPhoenixParams();
        if(!ks.equals("")) {
            reqParams.addProperty("ks", ks);
        }
        reqParams.add("request", requestProperty);

        return new RequestBuilder()
                .service("licensedUrl")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag(tag)
                .params(reqParams);
    }

    //TODO: check if assetId is needed
}
