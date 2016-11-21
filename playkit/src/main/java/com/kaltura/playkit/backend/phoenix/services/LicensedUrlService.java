package com.kaltura.playkit.backend.phoenix.services;

import com.google.gson.JsonObject;
import com.kaltura.playkit.connect.RequestBuilder;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class LicensedUrlService extends PhoenixService {

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
        JsonObject reqParams = getPhoenixParams();
        reqParams.addProperty("request:objectType", "KalturaLicensedUrlMediaRequest");
        reqParams.addProperty("request:contentId", mediaId);
        reqParams.addProperty("request:baseUrl", mediaBaseUrl);
        reqParams.addProperty("request:assetId", assetId);
        reqParams.addProperty("ks", ks);

        return new RequestBuilder()
                .service("licensedUrl")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .tag("licensedlink-media-get")
                .params(reqParams);
    }

    /**
     * @param baseUrl
     * @param ks
     * @param streamType: catchup / start_over / trick_play
     * @param startDate
     * @return
     */
    public static RequestBuilder getForEPG(String baseUrl, String ks, String assetId, String streamType, long startDate) {
        JsonObject reqParams = getPhoenixParams();
        reqParams.addProperty("request:objectType", "KalturaLicensedUrlMediaRequest");
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
        reqParams.addProperty("request:fileType", fileType);
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

    //TODO: check if assetId is needed
}
