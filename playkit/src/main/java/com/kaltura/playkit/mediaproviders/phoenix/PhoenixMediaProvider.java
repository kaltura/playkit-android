package com.kaltura.playkit.mediaproviders.phoenix;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestElement;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.mediaproviders.base.ErrorElement;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.phoenix.data.AssetInfo;
import com.kaltura.playkit.mediaproviders.phoenix.data.MediaFile;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class PhoenixMediaProvider implements MediaEntryProvider {

    //private PhoenixRequestsHandler requestsHandler;
    private RequestQueue requestsExecutor = APIOkRequestsExecutor.getSingleton();
    private String baseUrl;
    private String assetId;
    private String ks;
    private int partnerId = 0;
    private String referenceType;
    private String format;

    public PhoenixMediaProvider(String baseUrl, int partnerId, String assetId, String assetReferenceType) {
        //requestsHandler = new PhoenixRequestsHandler(baseUrl, new APIOkRequestsExecutor());
        this.baseUrl = baseUrl;
        this.partnerId = partnerId;
        this.assetId = assetId;
        this.referenceType = assetReferenceType;
    }

    public PhoenixMediaProvider(String baseUrl, String ks, String assetId, String assetReferenceType) {
        //requestsHandler = new PhoenixRequestsHandler(baseUrl, new APIOkRequestsExecutor());
        this.baseUrl = baseUrl;
        this.ks = ks;
        this.assetId = assetId;
        this.referenceType = assetReferenceType;
    }

    public PhoenixMediaProvider requestExecutor(RequestQueue executor) {
        this.requestsExecutor = executor;
        return this;
    }


    @Override
    public void load(final OnMediaLoadCompletion completion) {

        RequestElement requestElement = new RequestBuilder()
                .method("POST")
                .url(baseUrl + "service/asset/action/get")
                .tag("asset-get")
                .body(getAssetRequestBody().toString())
                .completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {
                        onAssetRequestResult(response, completion);
                    }
                }).build();

        /*new RequestElement() {

                @Override
                public String getMethod() {
                    return "POST";
                }

                @Override
                public String getUrl() {
                    return baseUrl + "service/asset/action/get";
                }

                @Override
                public String getBody() {

                    JsonObject body = new JsonObject();
                    body.addProperty("ks", ks);
                    body.addProperty("id", assetId);
                    body.addProperty("assetReferenceType", assetReferenceType);

                    return body.toString();
                }

                @Override
                public String getTag() {
                    return "asset-get";
                }

                @Override
                public HashMap<String, String> getHeaders() {
                    return null;
                }

                @Override
                public String getId() {
                    return null;
                }

                @Override
                public RequestConfiguration config() {
                    return null;
                }

                @Override
                public void onComplete(final ResponseElement response) {
                    if (response != null && response.isSuccess()) {
                        AssetInfo asset = PhoenixParser.parseAsset(response.getResponse());
                        if (!TextUtils.isEmpty(format)) {
                            for (final MediaFile file : asset.getFiles()) {
                                if (file.getFormatType().equals(format)) {
                                    //?? only the "format" MediaFile should be parsed to MediaSource - app asked for a specific format(file)
                                    ArrayList<MediaFile> newFiles = new ArrayList<>(Collections.singletonList(file));
                                    asset.setFiles(newFiles);
                                }
                            }
                        }
                        final PKMediaEntry mediaEntry = PhoenixParser.getMedia(asset);
                        //TODO pass mediaEntry to completion callback
                        if (completion != null) {
                            completion.onComplete(new ResultElement<PKMediaEntry>() {
                                @Override
                                public PKMediaEntry getResponse() {
                                    return mediaEntry;
                                }

                                @Override
                                public boolean isSuccess() {
                                    return mediaEntry != null;
                                }

                                @Override
                                public ErrorElement getError() {
                                    return mediaEntry == null ? response.getError() : null;
                                }
                            });
                        }
                    }
                }
            };*/

        requestsExecutor.queue(requestElement);
    }


    private void onAssetRequestResult(final ResponseElement response, final OnMediaLoadCompletion completion) {
        if (response != null && response.isSuccess()) {
            AssetInfo asset = PhoenixParser.parseAsset(response.getResponse());
            if (!TextUtils.isEmpty(format)) {
                for (final MediaFile file : asset.getFiles()) {
                    if (file.getFormatType().equals(format)) {
                        //?? only the "format" MediaFile should be parsed to MediaSource - app asked for a specific format(file)
                        ArrayList<MediaFile> newFiles = new ArrayList<>(Collections.singletonList(file));
                        asset.setFiles(newFiles);
                    }
                }
            }
            final PKMediaEntry mediaEntry = PhoenixParser.getMedia(asset);
            //TODO pass mediaEntry to completion callback
            if (completion != null) {
                completion.onComplete(new ResultElement<PKMediaEntry>() {
                    @Override
                    public PKMediaEntry getResponse() {
                        return mediaEntry;
                    }

                    @Override
                    public boolean isSuccess() {
                        return mediaEntry != null;
                    }

                    @Override
                    public ErrorElement getError() {
                        return mediaEntry == null ? response.getError() : null;
                    }
                });
            }
        }
    }

    @NonNull
    private JsonObject getAssetRequestBody() {
        JsonObject body = new JsonObject();
        boolean isMultiReq = false;
        if (TextUtils.isEmpty(ks)) {
            JsonObject aReq = getAnonymousReqParams(partnerId);
            body.add("1", aReq);
            ks = "{1:result:ks}";
            isMultiReq = true;
        }

        if (isMultiReq) {
            body.add("2", getAssetGetReq());
        } else {
            body = getAssetGetReq();
        }
        return body;
    }

    private JsonObject getAssetGetReq() {
        JsonObject getParams = new JsonObject();
        getParams.addProperty("ks", ks);
        getParams.addProperty("id", assetId);
        getParams.addProperty("assetReferenceType", referenceType);
        return getParams;
    }

    private JsonObject getAnonymousReqParams(int partnerId) {
        JsonObject params = new JsonObject();
        params.addProperty("service", "ottUser");
        params.addProperty("action", "anonymousLogin");
        params.addProperty("partnerId", partnerId);
        return params;
    }


        /*requestsHandler.getMediaInfo(ks, assetId, referenceType, new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if(response != null && response.isSuccess()){
                    AssetInfo asset = PhoenixParser.parseAsset(response.getResponse());
                    if(!TextUtils.isEmpty(format)){
                        for(final MediaFile file : asset.getFiles()){
                            if(file.getFormatType().equals(format)){
                                //?? only the "format" MediaFile should be parsed to MediaSource - app asked for a specific format(file)
                                ArrayList<MediaFile> newFiles = new ArrayList<>(Collections.singletonList(file));
                                asset.setFiles(newFiles);
                            }
                        }
                    }
                    PKMediaEntry mediaEntry = PhoenixParser.getMedia(asset);
                    //TODO pass mediaEntry to completion callback
                }
            }
        });*/
}
