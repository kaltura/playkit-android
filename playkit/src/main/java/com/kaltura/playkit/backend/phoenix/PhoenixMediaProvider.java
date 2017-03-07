package com.kaltura.playkit.backend.phoenix;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.JsonParseException;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.backend.SessionProvider;
import com.kaltura.playkit.backend.base.BECallableLoader;
import com.kaltura.playkit.backend.base.BEMediaProvider;
import com.kaltura.playkit.backend.base.FormatsHelper;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.base.data.KalturaDrmPlaybackPluginData;
import com.kaltura.playkit.backend.phoenix.data.KalturaMediaAsset;
import com.kaltura.playkit.backend.phoenix.data.KalturaPlaybackContext;
import com.kaltura.playkit.backend.phoenix.data.KalturaPlaybackSource;
import com.kaltura.playkit.backend.phoenix.data.PhoenixParser;
import com.kaltura.playkit.backend.phoenix.services.AssetService;
import com.kaltura.playkit.backend.phoenix.services.OttUserService;
import com.kaltura.playkit.backend.phoenix.services.PhoenixService;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.MultiRequestBuilder;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.kaltura.playkit.PKDrmParams.Scheme.playready;
import static com.kaltura.playkit.PKDrmParams.Scheme.playready_cenc;
import static com.kaltura.playkit.PKDrmParams.Scheme.widevine_cenc;
import static com.kaltura.playkit.PKDrmParams.Scheme.widevine_classic;


/**
 * Created by tehilarozin on 27/10/2016.
 */

/*
* usages:
*
* by formats - request will fetch all available source, filter sources response according to requested formats list
*
* by mediaFile ids - request include the requests file ids and will fetch sources for those files only.
*
* mandatory fields: assetId, assetType, contextType
*
*
* */

public class PhoenixMediaProvider extends BEMediaProvider {

    private static final String TAG = "PhoenixMediaProvider";

    private static final boolean EnableEmptyKs = true;

    private MediaAsset mediaAsset;


    private class MediaAsset {

        public String assetId;

        public APIDefines.KalturaAssetType assetType;

        public APIDefines.PlaybackContextType contextType;

        public List<String> formats;

        public List<String> mediaFileIds;

        public String protocol;

        public MediaAsset(){
            protocol = "https";
        }

        public boolean hasFormats() {
            return formats != null && formats.size() > 0;
        }

        public boolean hasFiles() {
            return mediaFileIds != null && mediaFileIds.size() > 0;
        }
    }

    //!! add parameter for streamType - catchup/startOver/...

    public PhoenixMediaProvider() {
        super(PhoenixMediaProvider.TAG);
        this.mediaAsset = new MediaAsset();
    }

    /**
     * MANDATORY! provides the baseUrl and the session token(ks) for the API calls.
     *
     * @param sessionProvider
     * @return
     */
    public PhoenixMediaProvider setSessionProvider(@NonNull SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
        return this;
    }

    /**
     * MANDATORY! the media asset id, to fetch the data for.
     *
     * @param assetId
     * @return
     */
    public PhoenixMediaProvider setAssetId(@NonNull String assetId) {
        this.mediaAsset.assetId = assetId;
        return this;
    }

    /**
     * ESSENTIAL!! defines the playing asset group type
     * Defaults to - {@link com.kaltura.playkit.backend.phoenix.APIDefines.KalturaAssetType#Media}
     *
     * @param assetType - can be one of the following types {@link com.kaltura.playkit.backend.phoenix.APIDefines.KalturaAssetType}
     * @return
     */
    public PhoenixMediaProvider setAssetType(@NonNull APIDefines.KalturaAssetType assetType) {
        this.mediaAsset.assetType = assetType;
        return this;
    }

    /**
     * ESSENTIAL!! defines the playing context: Trailer, Catchup, Playback etc
     * Defaults to - {@link com.kaltura.playkit.backend.phoenix.APIDefines.PlaybackContextType#Playback}
     *
     * @param contextType - can be one of the following types {@link com.kaltura.playkit.backend.phoenix.APIDefines.PlaybackContextType}
     * @return
     */
    public PhoenixMediaProvider setContextType(@NonNull APIDefines.PlaybackContextType contextType) {
        this.mediaAsset.contextType = contextType;
        return this;
    }

    /**
     * OPTIONAL
     * defines which of the sources to consider on {@link PKMediaEntry} creation.
     *
     * @param formats - 1 or more content format definition. can be: Hd, Sd, Download, Trailer etc
     * @return
     */
    public PhoenixMediaProvider setFormats(@NonNull String... formats) {
        this.mediaAsset.formats = new ArrayList<>(Arrays.asList(formats));
        return this;
    }

    /**
     * OPTIONAL - if not available all sources will be fetched
     * Provide a list of media files ids. will be used in the getPlaybackContext API request                                                                                                 .
     *
     * @param mediaFileIds - list of MediaFile ids to narrow sources fetching from API to
     *                     the specific files
     * @return
     */
    public PhoenixMediaProvider setFileIds(@NonNull String... mediaFileIds) {
        this.mediaAsset.mediaFileIds = new ArrayList<>(Arrays.asList(mediaFileIds));
        return this;
    }


    /**
     * OPTIONAL
     * Defaults to {@link com.kaltura.playkit.connect.APIOkRequestsExecutor} implementation.
     *
     * @param executor
     * @return
     */
    public PhoenixMediaProvider setRequestExecutor(@NonNull RequestQueue executor) {
        this.requestsExecutor = executor;
        return this;
    }


    protected Loader factorNewLoader(OnMediaLoadCompletion completion) {
        return new Loader(requestsExecutor, sessionProvider, mediaAsset, completion);
    }


    /**
     * Checks for non empty value on the mandatory parameters.
     *
     * @return - error in case of at least 1 invalid mandatory parameter.
     */
    @Override
    protected ErrorElement validateParams() {
        ErrorElement error = null;

        if (TextUtils.isEmpty(this.mediaAsset.assetId)) {
            error = ErrorElement.BadRequestError.addMessage(": Missing required parameter [assetId]");

        } else {

            //set Defaults if not provided:
            if (mediaAsset.assetType == null) {
                mediaAsset.assetType = APIDefines.KalturaAssetType.Media;
            }
            if (mediaAsset.contextType == null) {
                mediaAsset.contextType = APIDefines.PlaybackContextType.Playback;
            }
        }

        return error;
    }


    class Loader extends BECallableLoader {

        private MediaAsset mediaAsset;


        public Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, MediaAsset mediaAsset, OnMediaLoadCompletion completion) {
            super(PhoenixMediaProvider.TAG + "#Loader", requestsExecutor, sessionProvider, completion);

            this.mediaAsset = mediaAsset;

            PKLog.v(TAG, loadId + ": construct new Loader");
        }

        @Override
        protected ErrorElement validateKs(String ks) { // enable anonymous session creation
            return EnableEmptyKs || !TextUtils.isEmpty(ks) ? null :
                    ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token");
        }


        private RequestBuilder getPlaybackContextRequest(String baseUrl, String ks, MediaAsset mediaAsset) {
            AssetService.KalturaPlaybackContextOptions contextOptions = new AssetService.KalturaPlaybackContextOptions(mediaAsset.contextType);
            if (mediaAsset.mediaFileIds != null) { // else - will fetch all available sources
                contextOptions.setMediaFileIds(mediaAsset.mediaFileIds);
            }

            contextOptions.setMediaProtocol(mediaAsset.protocol);

            return AssetService.getPlaybackContext(baseUrl, ks, mediaAsset.assetId,
                    mediaAsset.assetType, contextOptions);
        }

        private RequestBuilder getRemoteRequest(String baseUrl, String ks, MediaAsset mediaAsset) {

            if (TextUtils.isEmpty(ks)) {
                MultiRequestBuilder multiRequestBuilder = (MultiRequestBuilder) PhoenixService.getMultirequest(baseUrl, ks)
                        .tag("asset-play-data-multireq");
                String multiReqKs = "{1:result:ks}";
                return multiRequestBuilder.add(OttUserService.anonymousLogin(baseUrl, sessionProvider.partnerId(), null),
                        getPlaybackContextRequest(baseUrl, multiReqKs, mediaAsset));
            }

            return getPlaybackContextRequest(baseUrl, ks, mediaAsset);
        }

        /**
         * Builds and passes to the executor, the Asset info fetching request.
         *
         * @param ks
         * @throws InterruptedException
         */
        @Override
        protected void requestRemote(String ks) throws InterruptedException {
            final RequestBuilder requestBuilder = getRemoteRequest(getApiBaseUrl(), ks, mediaAsset)
                    .completion(new OnRequestCompletion() {
                        @Override
                        public void onComplete(ResponseElement response) {
                            PKLog.v(TAG, loadId + ": got response to [" + loadReq + "]");
                            loadReq = null;

                            try {
                                onAssetGetResponse(response/*, requestBuilder instanceof MultiRequestBuilder*/);

                            } catch (InterruptedException e) {
                                interrupted();
                            }
                        }
                    });

            synchronized (syncObject) {
                loadReq = requestQueue.queue(requestBuilder.build());
                PKLog.d(TAG, loadId + ": request queued for execution [" + loadReq + "]");
            }
            waitCompletion();
        }

        private String getApiBaseUrl() {
            return sessionProvider.baseUrl();
        }


        /**
         * Parse and create a {@link PKMediaEntry} object from the API response.
         *
         * @param response
         * @throws InterruptedException
         */
        private void onAssetGetResponse(final ResponseElement response) throws InterruptedException {
            ErrorElement error = null;
            PKMediaEntry mediaEntry = null;

            if (isCanceled()) {
                PKLog.v(TAG, loadId + ": i am canceled, exit response parsing ");
                return;
            }

            if (response != null && response.isSuccess()) {
                KalturaMediaAsset asset = null;

                try {
                    //**************************

                /* ways to parse the AssetInfo from response string:

                    1. <T> T PhoenixParser.parseObject: parse json string to a single object, according to a specific type - returns an object of the specific type
                            asset = PhoenixParser.parseObject(response.getResponse(), KalturaMediaAsset.class);

                    2. Object PhoenixParser.parse(String response, Class...types): parse json string according to 1 or more types (dynamic types array) - returns Object since can
                       be single or an array of objects. cast is needed, can be used for multiple response
                            asset = (KalturaMediaAsset) PhoenixParser.parse(response.getResponse(), KalturaMediaAsset.class);

                        in case of an error - the error will be passed over the returned object (should extend BaseResult) */

                    //*************************

                    PKLog.d(TAG, loadId + ": parsing response  [" + Loader.this.toString() + "]");
                    /* 3. <T> T PhoenixParser.parse(String response): parse json string to an object of dynamically parsed type.
                       type defined by the value of "objectType" property provided in the response objects, if type wasn't found or in
                       case of error object in the response, will be parsed to BaseResult object (error if occurred will be accessible from this object)*/

                    Object parsedResponses = PhoenixParser.parse(response.getResponse());
                    BaseResult playbackContextResult = parsedResponses instanceof BaseResult ? (BaseResult) parsedResponses : ((List<BaseResult>) parsedResponses).get(1);

                    if (playbackContextResult.error != null) {
                        //error = ErrorElement.LoadError.message("failed to get multirequest responses on load request for asset "+mediaAsset.assetId);
                        error = PhoenixErrorHelper.getErrorElement(playbackContextResult.error); // get predefined error if exists for this error code

                    } else {

                        KalturaPlaybackContext kalturaPlaybackContext = (KalturaPlaybackContext) playbackContextResult;

                        if ((error = kalturaPlaybackContext.hasError()) == null) { // check for error message

                            mediaEntry = ProviderParser.getMedia(mediaAsset.assetId,
                                    mediaAsset.formats != null ? mediaAsset.formats : mediaAsset.mediaFileIds,
                                    kalturaPlaybackContext.getSources());

                            if (mediaEntry.getSources().size() == 0) { // makes sure there are sources available for play
                                error = ErrorElement.NotFound.message("Content can't be played due to lack of sources");
                            }
                        }
                    }
                } catch (JsonParseException | InvalidParameterException ex) {
                    error = ErrorElement.LoadError.message("failed parsing remote response: " + ex.getMessage());
                } catch (IndexOutOfBoundsException ex) {
                    error = ErrorElement.GeneralError.message("responses list doesn't contain the expected responses number: " + ex.getMessage());
                }

            } else {
                error = response != null && response.getError() != null ? response.getError() : ErrorElement.LoadError;
            }

            PKLog.i(TAG, loadId + ": load operation " + (isCanceled() ? "canceled" : "finished with " + (error == null ? "success" : "failure")));

            if (!isCanceled() && completion != null) {
                completion.onComplete(Accessories.buildResult(mediaEntry, error));
            }

            notifyCompletion();

        }
    }


    static class ProviderParser {

        public static PKMediaEntry getMedia(String assetId, final List<String> sourcesFilter, ArrayList<KalturaPlaybackSource> playbackSources) {

            PKMediaEntry mediaEntry = new PKMediaEntry();
            mediaEntry.setId("" + assetId);

            // until the response will be delivered in the right order:
            playbackSourcesSort(sourcesFilter, playbackSources);

            ArrayList<PKMediaSource> sources = new ArrayList<>();

            long maxDuration = 0;

            if (playbackSources != null) {

                // if provided, only the "formats" matching MediaFiles should be parsed and added to the PKMediaEntry media sources
                for (KalturaPlaybackSource playbackSource : playbackSources) {

                    boolean inSourceFilter = sourcesFilter != null &&
                            (sourcesFilter.contains(playbackSource.getType()) ||
                                    sourcesFilter.contains(playbackSource.getId()+""));

                    if (sourcesFilter != null && !inSourceFilter) { // if specific formats/fileIds were requested, only those will be added to the sources.
                        continue;
                    }

                    PKMediaFormat mediaFormat = FormatsHelper.getPKMediaFormat(playbackSource.getFormat(), playbackSource.hasDrmData());

                    if (mediaFormat == null) {
                        continue;
                    }

                    PKMediaSource pkMediaSource = new PKMediaSource()
                            .setId(playbackSource.getId() + "")
                            .setUrl(playbackSource.getUrl())
                            .setMediaFormat(mediaFormat);

                    List<KalturaDrmPlaybackPluginData> drmData = playbackSource.getDrmData();
                    if (drmData != null) {
                        List<PKDrmParams> drmParams = new ArrayList<>();
                        for (KalturaDrmPlaybackPluginData drm : drmData) {
                            drmParams.add(new PKDrmParams(drm.getLicenseURL(), getScheme(drm.getScheme())));
                        }
                        pkMediaSource.setDrmData(drmParams);
                    }

                    sources.add(pkMediaSource);
                    maxDuration = Math.max(playbackSource.getDuration(), maxDuration);
                }
            }
            return mediaEntry.setDuration(maxDuration).setSources(sources).setMediaType(MediaTypeConverter.toMediaEntryType(""));
        }

        //TODO: check why we get all sources while we asked for 4 specific formats

        // needed to sort the playback source result to be in the same order as in the requested list.
        private static void playbackSourcesSort(final List<String> sourcesFilter, ArrayList<KalturaPlaybackSource> playbackSources) {
            Collections.sort(playbackSources, new Comparator<KalturaPlaybackSource>() {
                @Override
                public int compare(KalturaPlaybackSource o1, KalturaPlaybackSource o2) {

                    int valueIndex1 = -1;
                    int valueIndex2 = -1;
                    if(sourcesFilter != null) {
                        valueIndex1 = sourcesFilter.indexOf(o1.getType());
                        if (valueIndex1 == -1) {
                            valueIndex1 = sourcesFilter.indexOf(o1.getId() + "");
                            valueIndex2 = sourcesFilter.indexOf(o2.getId() + "");
                        } else {
                            valueIndex2 = sourcesFilter.indexOf(o2.getType());
                        }
                    }
                    return valueIndex1 - valueIndex2;
                }
            });
        }
    }

    public static PKDrmParams.Scheme getScheme(String scheme) {

        switch (scheme) {
            case "WIDEVINE_CENC":
                return widevine_cenc;
            case "PLAYREADY_CENC":
                return playready_cenc;
            case "WIDEVINE":
                return widevine_classic;
            case "PLAYREADY":
                return playready;
            default:
                return null;
        }
    }

    static class MediaTypeConverter {

        public static PKMediaEntry.MediaEntryType toMediaEntryType(String mediaType) {
            switch (mediaType) {
                default:
                    return PKMediaEntry.MediaEntryType.Unknown;
            }
        }
    }

}
