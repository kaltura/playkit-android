package com.kaltura.playkit.backend.phoenix;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

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
import com.kaltura.playkit.backend.phoenix.data.MediaFile;
import com.kaltura.playkit.backend.phoenix.data.ResultAdapter;
import com.kaltura.playkit.backend.phoenix.services.AssetService;
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
    private SessionProvider sessionProvider;
    private MediaAsset mediaAsset;
    //private boolean alwaysFetchLicense;


    private class MediaAsset {
        String assetId;
        @APIDefines.AssetReferenceType
        String referenceType;
        List<String> formats;
        String epgId;
        long startDate;
        String streamType;

        boolean hasFormats() {
            return formats != null && formats.size() > 0;
        }
    }

    //!! add parameter for streamType - catchup/startOver/...

    public PhoenixMediaProvider() {
        this.mediaAsset = new MediaAsset();
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        //this.alwaysFetchLicense = true;
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
        this.mediaAsset.epgId = epgId;
        return this;
    }

    public PhoenixMediaProvider setStartDate(long startDate) {
        this.mediaAsset.startDate = startDate;
        return this;
    }

    public PhoenixMediaProvider setLiveStreamType(@APIDefines.LiveStreamType String streamType){
        this.mediaAsset.streamType = streamType;
        return this;
    }

    /*public PhoenixMediaProvider setAlwaysFetchLicense(boolean alwaysFetchLicense) {
        this.alwaysFetchLicense = alwaysFetchLicense;
        return this;
    }*/

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
        ErrorElement error = validateLoad();
        if(error != null){
            if (completion != null) {
                completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, error));
            }
            return;
        }

        RequestBuilder requestBuilder = AssetService.assetGet(sessionProvider.baseUrl(), /*sessionProvider.partnerId(),*/ sessionProvider.getKs(), mediaAsset.assetId, mediaAsset.referenceType);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                onAssetGetResponse(response, completion);
            }
        });

        requestsExecutor.queue(requestBuilder.build());

    }

    /**
     * validate basic parameters needed
     * @return
     */
    private ErrorElement validateLoad() {
        ErrorElement error = validateKs();
        return error != null ? error : validateAsset();
    }


    /**
     * Asset id is required for data fetching.
     * Ott play must have defined format(s) in order to select the right media file to play.
     * @return
     */
    private ErrorElement validateAsset() {
        String error = TextUtils.isEmpty(this.mediaAsset.assetId) ? ": Missing required parameters, assetId" :
                (!this.mediaAsset.hasFormats() ? ": at least 1 media file format is required!" : null);
        return error != null ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + error) :
                null;
    }

    private ErrorElement validateKs() {
        return TextUtils.isEmpty(this.sessionProvider.getKs()) ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError +": SessionProvider should provide a valid KS token") :
                null;
    }


    private void onAssetGetResponse(final ResponseElement response, final OnMediaLoadCompletion completion) {
        ErrorElement error = null;
        PKMediaEntry mediaEntry = null;

        if (response != null && response.isSuccess()) {
            AssetInfo asset = null;

            try {
                AssetResult assetResult = PhoenixParser.parseAssetResult(response.getResponse());
                if (assetResult.error != null) {
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

         } else {
            error = response != null && response.getError() != null ? response.getError() : ErrorElement.LoadError;
        }

        /*if (error == null && alwaysFetchLicense) {
            fetchLicenseLinks(mediaEntry, completion);

        } else*/
        if (completion != null) {
            completion.onComplete(Accessories.buildResult(mediaEntry, error));
        }
    }

    public static PKMediaEntry getMediaEntry(AssetInfo assetInfo, List<String> formats) {
        return PhoenixParser.getMedia(assetInfo, formats);
    }

    /*private void fetchLicenseLinks(PKMediaEntry mediaEntry, OnMediaLoadCompletion completion) {
        //TODO: create request to the BE to fetch licenses to the sources within mediaEntry
        switch (mediaAsset.referenceType) { // or on asset.type?)
            case APIDefines.AssetReferenceType.Media:
                //get for media
                fetchLicensedLinksForMedia(mediaEntry, completion);
                break;

            case APIDefines.AssetReferenceType.InternalEpg:
                //get for epg

                break;

            //?? in case of external EPG? can we call BE licensed link with that id?
            //?? in case of recording - how can we distinguish recording from Epg/media
        }

    }*/


    /**
     * Builds a {@link MultiRequestBuilder} construct on licensedUrl/get request on each source in the
     * media entry. Multirequest is used in order to pass all licensedUrl/get requests in one pass to the BE.
     *
     * //@param mediaEntry - contains the sources for the requests
     * //@param completion - the provided {@link OnMediaLoadCompletion} implementation.
     */
    /*void fetchLicensedLinksForMedia(final PKMediaEntry mediaEntry, final OnMediaLoadCompletion completion) {

        MultiRequestBuilder requestBuilder = (MultiRequestBuilder) PhoenixService.getMultirequest(sessionProvider.baseUrl(), sessionProvider.getKs()).tag("licensedLinks-multi-"+mediaEntry.getSources().size());
        for (PKMediaSource source : mediaEntry.getSources()) {
            requestBuilder.add(LicensedUrlService.getForMedia(sessionProvider.baseUrl(), "", mediaAsset.assetId, source.getId(), source.getUrl()));
        }

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                ErrorElement errorElement = null;
                PKMediaEntry pkMediaEntry = mediaEntry;

                if (response.isSuccess()) {
                    // parse multiresponses to an Object list.
                    List<Object> responses = PhoenixParser.parseMultiresponse(response.getResponse(), 0, new Class[]{LicensedUrl.class});
                    int failuresCount = 0;
                    // update the sources urls with the licensed ones:
                    for (int resIdx = 0; resIdx < responses.size(); resIdx++) {
                        LicensedUrl licensedUrl = (LicensedUrl) responses.get(resIdx);
                        if (licensedUrl.error == null) { // error != null indicates failure on license retrieval for this source.
                            pkMediaEntry.getSources().get(resIdx).setUrl(licensedUrl.getLicensedUrl());
                        } else {
                            failuresCount++;
                        }
                    }
                    if(failuresCount == responses.size()){
                        errorElement = ErrorElement.LoadError.message("failed retrieving licensed links for media entry sources");
                    }

                } else {
                    errorElement = response.getError() != null ? response.getError() : ErrorElement.LoadError.message("failed retrieving licensed links for media entry sources");
                    if (alwaysFetchLicense) {
                        pkMediaEntry = null; //?? !! should we?
                    }
                }

                if (completion != null) {
                    completion.onComplete(Accessories.<PKMediaEntry>buildResult(pkMediaEntry, errorElement));
                }
            }
        });

        requestsExecutor.queue(requestBuilder.build());
    }
*/

    static class PhoenixParser {

        static PKMediaEntry getMedia(AssetInfo assetInfo, List<String> formats) {
            return getMedia(assetInfo.getId() + "", assetInfo.getFiles(), formats);
        }

        static PKMediaEntry getMedia(String id, List<MediaFile> mediaFiles) {
            return getMedia(id, mediaFiles, null);
        }

        private static PKMediaEntry getMedia(String assetId, List<MediaFile> mediaFiles, List<String> formats) {
            PKMediaEntry mediaEntry = new PKMediaEntry();
            mediaEntry.setId("" + assetId);

            ArrayList<PKMediaSource> sources = new ArrayList<>();
            long maxDuration = 0;
            if (mediaFiles != null) {
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

        static List<Object> parseMultiresponse(String response, int parseFromIdx, @NonNull Class... types) throws JsonSyntaxException {

            JsonParser parser = new JsonParser();
            JsonElement resultElement = parser.parse(response).getAsJsonObject().get("result");
            ArrayList<Object> responsesObjects = new ArrayList<>();

            if (resultElement.isJsonArray()) {
                JsonArray responses = resultElement.getAsJsonArray();
                Gson gson = new GsonBuilder().registerTypeAdapter(BaseResult.class, new ResultAdapter()).create();
                int tIdx = 0;
                Class claz;
                for (int i = parseFromIdx; i < responses.size() /*&& tIdx < types.length*/; i++) {
                    claz = types[tIdx];
                    responsesObjects.add(gson.fromJson(responses.get(i), claz));
                    if (tIdx < types.length - 1) {
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