package com.kaltura.playkit.mediaproviders.phoenix;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.phoenix.data.AssetInfo;
import com.kaltura.playkit.mediaproviders.phoenix.data.AssetResult;
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

    public PhoenixMediaProvider(String baseUrl, int partnerId, String assetId, String assetReferenceType, String format) {
        //requestsHandler = new PhoenixRequestsHandler(baseUrl, new APIOkRequestsExecutor());
        this.baseUrl = baseUrl;
        this.partnerId = partnerId;
        this.assetId = assetId;
        this.referenceType = assetReferenceType;
        this.format = format;
    }

    public PhoenixMediaProvider(String baseUrl, String ks, String assetId, String assetReferenceType, String format) {
        //requestsHandler = new PhoenixRequestsHandler(baseUrl, new APIOkRequestsExecutor());
        this.baseUrl = baseUrl;
        this.ks = ks;
        this.assetId = assetId;
        this.referenceType = assetReferenceType;
        this.format = format;
    }

    public PhoenixMediaProvider setRequestExecutor(RequestQueue executor) {
        this.requestsExecutor = executor;
        return this;
    }

    public PhoenixMediaProvider setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public PhoenixMediaProvider setFormat(String format) {
        this.format = format;
        return this;
    }

    public PhoenixMediaProvider setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public PhoenixMediaProvider setKs(String ks) {
        this.ks = ks;
        return this;
    }

    public PhoenixMediaProvider setPartnerId(int partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public PhoenixMediaProvider setReferenceType(String referenceType) {
        this.referenceType = referenceType;
        return this;
    }

    /**
     * Activates the providers data fetching process.
     * According to previously provided arguments, a request is built and passed to the remote server.
     * Fetching flow can ended with {@link PKMediaEntry} object if succeeded or with {@link ErrorElement} if failed.
     *
     * @param completion - a callback for handling the result of data fetching flow.
     */
    @Override
    public void load(final OnMediaLoadCompletion completion) {
        // Ott play must have defined format in order to select the right media file to play.
        if (TextUtils.isEmpty(format)) {
            if (completion != null) {
                completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": media file format is required!")));
            }
            return;
        }

        RequestBuilder requestBuilder = TextUtils.isEmpty(ks) ?
                AssetService.assetGet(baseUrl, partnerId, assetId, referenceType) :
                AssetService.assetGet(baseUrl, ks, assetId, referenceType);

        requestsExecutor.queue(requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                onAssetRequestResult(response, completion);
            }
        }).build());

        /*new RequestBuilder()
                .method("POST")
                .url(getRequestUrl())
                .tag("asset-get")
                .body(getAssetRequestBody().toString())
                .completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {
                        onAssetRequestResult(response, completion);
                    }
                }).build();

        requestsExecutor.queue(requestElement);*/
    }

    /*@NonNull
    private String getRequestUrl() {
        return baseUrl + (TextUtils.isEmpty(ks) ? "service/multirequest" : "service/asset/action/get");
    }*/

    private void onAssetRequestResult(final ResponseElement response, final OnMediaLoadCompletion completion) {
        ErrorElement error = null;
        PKMediaEntry mediaEntry = null;

        if (response != null && response.isSuccess()) {
            AssetInfo asset = null;

            try {
                asset = PhoenixParser.parseAssetResult(response.getResponse());
            } catch (JsonSyntaxException ex) {
                error = ErrorElement.LoadError.message("failed parsing remote response: " + ex.getMessage());
            }

            if (asset != null) {
                // only the provided "format" matching MediaFile should be parsed and added to the PKMediaEntry media sources
                for (final MediaFile file : asset.getFiles()) {
                    if (file.getType().equals(format)) {
                        ArrayList<MediaFile> newFiles = new ArrayList<>(Collections.singletonList(file));
                        asset.setFiles(newFiles);
                        break;
                    }
                }

                mediaEntry = PhoenixParser.getMedia(asset);
            }
        } else {
            error = response != null && response.getError() != null ? response.getError() : ErrorElement.LoadError;
        }

        if (completion != null) {
            completion.onComplete(Accessories.buildResult(mediaEntry, error));
        }
    }

   /* @NonNull
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
*/

    static class PhoenixParser {

        static PKMediaEntry getMedia(AssetInfo assetInfo) {
            PKMediaEntry mediaEntry = new PKMediaEntry();
            mediaEntry.setId("" + assetInfo.getId());

            ArrayList<PKMediaSource> sources = new ArrayList<>();
            for (MediaFile file : assetInfo.getFiles()) {
                PKMediaSource source = new PKMediaSource();
                source.setId("" + file.getId());
                source.setUrl(file.getUrl());

                //source.setMimeType(Defines.getMimeType(Accessories.getFileExt(file.getUrl())));
                sources.add(source);
                mediaEntry.setDuration(file.getDuration()); // ??
            }
            mediaEntry.setSources(sources);
            return mediaEntry;
        }

        static AssetInfo parseAssetResult(String json) throws JsonSyntaxException {
            return new Gson().fromJson(json, AssetResult.class).asset;
        }
    }
}
