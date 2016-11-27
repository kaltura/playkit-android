package com.kaltura.playkit.backend.phoenix;

import com.google.gson.GsonBuilder;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.backend.phoenix.data.AssetResult;
import com.kaltura.playkit.backend.phoenix.data.KalturaLicensedUrl;
import com.kaltura.playkit.backend.phoenix.data.OttResultAdapter;
import com.kaltura.playkit.backend.phoenix.services.LicensedUrlService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.connect.SessionProvider;

/**
 * Created by tehilarozin on 22/11/2016.
 */

public class PhoenixLicensedLinksPlugin {

    private SessionProvider sessionProvider;
    private RequestQueue requestsExecutor = APIOkRequestsExecutor.getSingleton();

    private void fetchShiftedLiveLicenseLinks(String assetId, String programId, String streamType, long startDate, OnCompletion<ResultElement<String>> completion) {
        //catchup/startOver/trickPlay
        RequestBuilder requestBuilder = LicensedUrlService.getForShiftedLive(sessionProvider.baseUrl(), sessionProvider.getKs(), assetId, streamType, startDate);
        execute(requestBuilder, completion);
    }

    private void fetchMediaLicenseLinks(String assetId, PKMediaSource fileSource, OnCompletion<ResultElement<String>> completion) {
        // vod/channel
        RequestBuilder requestBuilder = LicensedUrlService.getForMedia(sessionProvider.baseUrl(), sessionProvider.getKs(), assetId, fileSource.getId(), fileSource.getUrl());
        execute(requestBuilder, completion);
    }

    private void fetchRecordingLicenseLinks(String assetId, String fileFormat, OnCompletion<ResultElement<String>> completion) {
        // vod/channel
        RequestBuilder requestBuilder = LicensedUrlService.getForRecording(sessionProvider.baseUrl(), sessionProvider.getKs(), assetId, fileFormat);
        execute(requestBuilder, completion);
    }

    private void execute(RequestBuilder requestBuilder, final OnCompletion<ResultElement<String>> completion) {
        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                String licensedLink = null;
                ErrorElement error = null;
                //TODO check success and call completion with results
                if(response.isSuccess()){
                    KalturaLicensedUrl licensedUrl = parseLicensedUrl(response.getResponse());
                    if(licensedUrl.error == null) {
                        //-> we have valid licensed link
                        licensedLink = licensedUrl.getLicensedUrl();// in case mainUrl is null will return the alternative url
                    }

                    if(licensedLink == null) { // error response from the server
                        error = ErrorElement.LoadError.message("failed fetching licensed url, media can't be played");
                    }

                } else { // request execution failure
                    //-> report error with ErrorElement
                    error = response.getError() != null ? response.getError() : ErrorElement.LoadError.message("failed fetching licensed url, media can't be played");
                }

                if(completion != null){
                    completion.onComplete(Accessories.buildResult(licensedLink, error));
                }
            }
        });

        requestsExecutor.queue(requestBuilder.build());
    }

    private KalturaLicensedUrl parseLicensedUrl(String response){
        return new GsonBuilder().registerTypeAdapter(AssetResult.class, new OttResultAdapter()).create().fromJson(response, KalturaLicensedUrl.class);
    }



}
