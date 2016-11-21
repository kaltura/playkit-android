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
        /*KalturaLicensedUrlMediaRequest - contentId (mediaFile id), baseUrl(current provided url)
        *KalturaLicensedUrlEpgRequest - streamType [catchup, startover, trick_play ]- enum, startDate (long)
        * KalturaLicensedUrlRecordingRequest- fileType
        *
        * request format:
        * request: {
		objectType: "KalturaLicensedUrlBaseRequest",
		assetId: "value"
	}*/
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
        JsonObject reqParams = getPhoenixParams();
        reqParams.addProperty("request:objectType", "KalturaLicensedUrlEpgRequest");
        reqParams.addProperty("request:streamType", streamType);
        reqParams.addProperty("request:startDate", startDate);
        reqParams.addProperty("request:assetId", assetId);
        reqParams.addProperty("ks", ks);

        return new RequestBuilder()
                .service("licensedUrl")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag("licensedlink-epg-get")
                .params(reqParams);
    }

    public static RequestBuilder getForRecording(String baseUrl, String ks, String assetId, String fileType) {
        JsonObject reqParams = getPhoenixParams();
        reqParams.addProperty("request:objectType", "KalturaLicensedUrlMediaRequest");
        reqParams.addProperty("request:fileType", fileType); //file format (HD,SD...)
        reqParams.addProperty("request:assetId", assetId);
        reqParams.addProperty("ks", ks);

        return new RequestBuilder()
                .service("licensedUrl")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag("licensedlink-rec-get")
                .params(reqParams);
    }

    private static RequestBuilder getLicensedLinksRequestBuilder(String baseUrl, String ks, String tag, JsonObject requestProperty) {
        JsonObject reqParams = getPhoenixParams();
        reqParams.addProperty("ks", ks);
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
