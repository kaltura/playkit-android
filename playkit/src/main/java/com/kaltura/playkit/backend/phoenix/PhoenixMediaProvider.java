package com.kaltura.playkit.backend.phoenix;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.phoenix.data.AssetInfo;
import com.kaltura.playkit.backend.phoenix.data.AssetResult;
import com.kaltura.playkit.backend.phoenix.data.BaseResult;
import com.kaltura.playkit.backend.phoenix.data.LicensedUrl;
import com.kaltura.playkit.backend.phoenix.data.MediaFile;
import com.kaltura.playkit.backend.phoenix.data.ResultAdapter;
import com.kaltura.playkit.backend.phoenix.services.AssetService;
import com.kaltura.playkit.backend.phoenix.services.LicensedUrlService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.MultiRequestBuilder;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.connect.SessionProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class PhoenixMediaProvider implements MediaEntryProvider {

    private RequestQueue requestsExecutor;
    private boolean alwaysFetchLicense;
    private SessionProvider sessionProvider;
    private MediaAsset mediaAsset;


    private class MediaAsset{
        String assetId;
        @APIDefines.AssetReferenceType String referenceType;
        List<String> formats;
        String EpgId;
        long startDate;

        boolean hasFormats() {
            return formats != null && formats.size() > 0;
        }
    }

    //!! add parameter for streamType - catchup/startOver/...

    public PhoenixMediaProvider() {
        this.mediaAsset = new MediaAsset();
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.alwaysFetchLicense = true;
    }

    public PhoenixMediaProvider setSessionProvider(@NonNull SessionProvider ksProvider) {
        this.sessionProvider = ksProvider;
        return this;
    }

    public PhoenixMediaProvider setAssetId(@NonNull String assetId) {
        this.mediaAsset.assetId = assetId;
        return this;
    }


    public PhoenixMediaProvider setFormats(@NonNull String... formats) {
        this.mediaAsset.formats = new ArrayList<>(Arrays.asList(formats));
        return this;
    }

    /**
     * @param referenceType - can be one of the {@link com.kaltura.playkit.backend.phoenix.APIDefines.AssetReferenceType} values
     * @return
     */
     public PhoenixMediaProvider setReferenceType(@NonNull String referenceType) {
        this.mediaAsset.referenceType = referenceType;
        return this;
    }

    public PhoenixMediaProvider setEpgId(@Nullable String epgId) {
        this.mediaAsset.EpgId = epgId;
        return this;
    }

    public PhoenixMediaProvider setStartDate(long startDate) {
        this.mediaAsset.startDate = startDate;
        return this;
    }

    public PhoenixMediaProvider setAlwaysFetchLicense(boolean alwaysFetchLicense) {
        this.alwaysFetchLicense = alwaysFetchLicense;
        return this;
    }

    public PhoenixMediaProvider setRequestExecutor(@NonNull RequestQueue executor) {
        this.requestsExecutor = executor;
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
        // Ott play must have defined format(s) in order to select the right media file to play.
        if (!this.mediaAsset.hasFormats()) {
            if (completion != null) {
                completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": media file format is required!")));
            }
            return;
        }

        RequestBuilder requestBuilder = AssetService.assetGet(sessionProvider.baseUrl(), sessionProvider.partnerId(), sessionProvider.getKs(), mediaAsset.assetId, mediaAsset.referenceType);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                onAssetGetResponse(response, completion);
            }
        });

        requestsExecutor.queue(requestBuilder.build());

    }


    private void onAssetGetResponse(final ResponseElement response, final OnMediaLoadCompletion completion) {
        ErrorElement error = null;
        PKMediaEntry mediaEntry = null;

        if (response != null && response.isSuccess()) {
            AssetInfo asset = null;

            try {
                AssetResult assetResult = PhoenixParser.parseAssetResult(response.getResponse());
                if(assetResult.error != null){
                    error = assetResult.error;
                } else {
                    asset = assetResult.asset;
                }
            } catch (JsonSyntaxException ex) {
                error = ErrorElement.LoadError.message("failed parsing remote response: " + ex.getMessage());
            }

            if (asset != null) {
                mediaEntry = PhoenixParser.getMedia(asset, mediaAsset.formats);
            }
            /*if (asset != null) {
                        if(alwaysFetchLicense){
                            fetchLicenseLinks(asset, completion);
                            return;

                        } else {
                            mediaEntry = PhoenixParser.getMedia(asset, mediaAsset.formats);
                        }
            }*/

        } else {
            error = response != null && response.getError() != null ? response.getError() : ErrorElement.LoadError;
        }

        if(error == null && alwaysFetchLicense){
            fetchLicenseLinks(mediaEntry, completion);

        } else if (completion != null) {
            completion.onComplete(Accessories.buildResult(mediaEntry, error));
        }
    }

    private void fetchLicenseLinks(PKMediaEntry mediaEntry, OnMediaLoadCompletion completion) {
        //TODO: create request to the BE to fetch licenses to the sources within mediaEntry
        switch (mediaAsset.referenceType){ // or on asset.type?)
            case APIDefines.AssetReferenceType.Media:
                //get for media
                fetchLicensedLinksFormMedia(mediaEntry, completion);
                break;

            case APIDefines.AssetReferenceType.InternalEpg:
                //get for epg

                break;

            //?? in case of external EPG? can we call BE licensed link with that id?
            //?? in case of recording - how can we distinguish recording from Epg/media
        }

    }

   /* private void getLicensedLinksForMedia(PKMediaEntry mediaEntry, OnMediaLoadCompletion completion) {

        new FetcherTask(mediaEntry, )
        getForMedia(sessionProvider.baseUrl(), sessionProvider.getKs(), mediaAsset.assetId, mediaAsset)
    }*/

    public static PKMediaEntry getMediaEntry(AssetInfo assetInfo, List<String> formats) {
        return PhoenixParser.getMedia(assetInfo, formats);
    }




     void fetchLicensedLinksFormMedia(PKMediaEntry mediaEntry, final OnMediaLoadCompletion callback ){
            //countdown = sources.size();
            MultiRequestBuilder requestBuilder = (MultiRequestBuilder) new MultiRequestBuilder().service("multirequest").method("POST").url(sessionProvider.baseUrl());
            for(PKMediaSource source : mediaEntry.getSources()){
                requestBuilder.add(LicensedUrlService.getForMedia(sessionProvider.baseUrl(), sessionProvider.getKs(), mediaAsset.assetId, source.getId(), source.getUrl()));

            }
            requestBuilder.completion(new OnRequestCompletion() {
                @Override
                public void onComplete(ResponseElement response) {
                    if(response.isSuccess()) {
                        List<Object> responses = PhoenixParser.parseMultiresponse(response.getResponse(), 0, new Class[]{LicensedUrl.class});
                        for(Object responseObject : responses){
                            LicensedUrl licensedUrl = (LicensedUrl)responseObject;

                        }

                    } else {
                        callback.onComplete(Accessories.<PKMediaEntry>buildResult(null, response.getError()));
                    }
                }
            });

            requestsExecutor.queue(requestBuilder.build());
        }




    static class PhoenixParser {

        static PKMediaEntry getMedia(AssetInfo assetInfo, List<String> formats) {
            return getMedia(assetInfo.getId()+"", assetInfo.getFiles(), formats);
        }

        static PKMediaEntry getMedia(String id, List<MediaFile> mediaFiles) {
            return getMedia(id, mediaFiles, null);
        }

        private static PKMediaEntry getMedia(String assetId, List<MediaFile> mediaFiles, List<String> formats){
            PKMediaEntry mediaEntry = new PKMediaEntry();
            mediaEntry.setId("" + assetId);

            ArrayList<PKMediaSource> sources = new ArrayList<>();
            long maxDuration = 0;
            if(mediaFiles != null) {
                // if provided, only the "formats" matching MediaFiles should be parsed and added to the PKMediaEntry media sources
                for (MediaFile file : mediaFiles) {
                    if (formats == null || formats.contains(file.getType())) {
                        sources.add(new PKMediaSource().setId(file.getId() + "").setUrl(file.getUrl()));
                        maxDuration = Math.max(file.getDuration(), maxDuration);
                    }
                }
            }
            return mediaEntry.setDuration(maxDuration).setSources(sources);
        }

        static AssetResult parseAssetResult(String json) throws JsonSyntaxException {
            return new GsonBuilder().registerTypeAdapter(AssetResult.class, new ResultAdapter()).create().fromJson(json, AssetResult.class);
        }

        static List<Object> parseMultiresponse(String response, int parseFromIdx, @NonNull Class...types) throws JsonSyntaxException {

            JsonParser parser = new JsonParser();
            JsonElement responseElement = parser.parse(response);
            ArrayList<Object> responsesObjects = new ArrayList<>();
            if(responseElement.isJsonArray()){
                JsonArray responses = responseElement.getAsJsonArray();
                Gson gson = new GsonBuilder().registerTypeAdapter(BaseResult.class, new ResultAdapter()).create();
                int tIdx = 0;
                Class claz;
                for(int i = parseFromIdx; i <= responses.size() /*&& tIdx < types.length*/; i++){
                    claz = types[tIdx];
                    responsesObjects.add(gson.fromJson(responses.get(i), claz));
                    if(tIdx < types.length){
                        tIdx++;
                    }
                }
            }
            return responsesObjects;
        }
    }
}
/*public PhoenixMediaProvider(SessionProvider sessionProvider, String assetId, String assetReferenceType, String... formats) {
        this.sessionProvider = sessionProvider;
        this.assetId = assetId;
        this.referenceType = assetReferenceType;
        this.formats = Arrays.asList(formats);
    }*/

/*private String assetId;
    private String referenceType;
    private List<String> formats;*/