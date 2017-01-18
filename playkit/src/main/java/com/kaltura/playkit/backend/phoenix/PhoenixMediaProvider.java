package com.kaltura.playkit.backend.phoenix;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonParseException;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.backend.SessionProvider;
import com.kaltura.playkit.backend.base.BECallableLoader;
import com.kaltura.playkit.backend.base.BEMediaProvider;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.phoenix.data.KalturaMediaAsset;
import com.kaltura.playkit.backend.phoenix.data.KalturaMediaFile;
import com.kaltura.playkit.backend.phoenix.data.PhoenixParser;
import com.kaltura.playkit.backend.phoenix.services.AssetService;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by tehilarozin on 27/10/2016.
 */

public class PhoenixMediaProvider extends BEMediaProvider {

    private static final String TAG = "PhoenixMediaProvider";

    private MediaAsset mediaAsset;


    private class MediaAsset {
        String assetId;
        @APIDefines.AssetReferenceType
        String referenceType;
        List<String> formats;
        /*consider remove the following 3 members if the license links are not rertreived by the provider*/
        String epgId;
        long startDate;
        String streamType;

        boolean hasFormats() {
            return formats != null && formats.size() > 0;
        }
    }

    //!! add parameter for streamType - catchup/startOver/...

    public PhoenixMediaProvider() {
        super(PhoenixMediaProvider.TAG);
        this.mediaAsset = new MediaAsset();
    }

    /**
     * MANDATORY! provides the baseUrl and the session token(ks) for the API calls.
     * @param sessionProvider
     * @return
     */
    public PhoenixMediaProvider setSessionProvider(@NonNull SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
        return this;
    }

    /**
     * MANDATORY! the media asset id, to fetch the data for.
     * @param assetId
     * @return
     */
    public PhoenixMediaProvider setAssetId(@NonNull String assetId) {
        this.mediaAsset.assetId = assetId;
        return this;
    }

    /**
     * MANDATORY! defines the sources to be used for the PKMediaSource objects creation.
     * @param formats 1 or more content format definition. can be: Hd, Sd, Download, Trailer etc
     * @return
     */
    public PhoenixMediaProvider setFormats(@NonNull String... formats) {
        this.mediaAsset.formats = new ArrayList<>(Arrays.asList(formats));
        return this;
    }

    /**
     * @param referenceType - can be one of the {@link com.kaltura.playkit.backend.phoenix.APIDefines.AssetReferenceType} values
     * @return
     */
    public PhoenixMediaProvider setReferenceType(@NonNull @APIDefines.AssetReferenceType String referenceType) {
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

    public PhoenixMediaProvider setLiveStreamType(@APIDefines.LiveStreamType String streamType) {
        this.mediaAsset.streamType = streamType;
        return this;
    }

    /**
     * optional parameter.
     * Defaults to {@link com.kaltura.playkit.connect.APIOkRequestsExecutor} implementation.
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

    public static PKMediaEntry getMediaEntry(KalturaMediaAsset assetInfo, List<String> formats) {
        return ProviderParser.getMedia(assetInfo, formats);
    }

    /**
     * Checks for non empty value on the mandatory parameters.
     * @return - error in case of at least 1 invalid mandatory parameter.
     */
    @Override
    protected ErrorElement validateParams() {
        String error = TextUtils.isEmpty(this.mediaAsset.assetId) ? ": Missing required parameters, assetId" :
                (!this.mediaAsset.hasFormats() ? ": at least 1 media file format is required!" : null);
        return error != null ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + error) :
                null;
    }


    class Loader extends BECallableLoader {

        private MediaAsset mediaAsset;


        public Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, MediaAsset mediaAsset, OnMediaLoadCompletion completion) {
            super(PhoenixMediaProvider.TAG + "#Loader", requestsExecutor, sessionProvider, completion);

            this.mediaAsset = mediaAsset;

            PKLog.v(TAG, loadId + ": construct new Loader");
        }

        @Override
        protected ErrorElement validateKs(String ks) {
            return TextUtils.isEmpty(ks) ?
                    ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token") :
                    null;
        }

        /**
         * Builds and passes to the executor, the Asset info fetching request.
         * @param ks
         * @throws InterruptedException
         */
        @Override
        protected void requestRemote(String ks) throws InterruptedException {
            PhoenixRequestBuilder requestBuilder = AssetService.assetGet(getApiBaseUrl(), ks, mediaAsset.assetId, mediaAsset.referenceType)
                    .completion(new OnRequestCompletion() {
                        @Override
                        public void onComplete(ResponseElement response) {
                            PKLog.v(TAG, loadId + ": got response to [" + loadReq + "]");
                            loadReq = null;

                            try {
                                onAssetGetResponse(response);

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
         * @param response
         * @throws InterruptedException
         */
        private void onAssetGetResponse(final ResponseElement response) throws InterruptedException {
            ErrorElement error = null;
            PKMediaEntry mediaEntry = null;

            if (isCanceled()) {
                PKLog.v(TAG, loadId+": i am canceled, exit response parsing ");
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
                    BaseResult assetResult = PhoenixParser.parse(response.getResponse());
                    if (assetResult != null) {
                        if (assetResult.error == null) {
                            asset = (KalturaMediaAsset) assetResult;
                        } else {
                            error = PhoenixErrorHelper.getErrorElement(assetResult.error); // get predefined error if exists for this error code
                        }
                    } else { // response parsed to null but request to the server returned a "valid" response
                        throw new JsonParseException("missing response object");
                    }

                } catch (JsonParseException ex) {
                    error = ErrorElement.LoadError.message("failed parsing remote response: " + ex.getMessage());
                }

                if (asset != null) {
                    mediaEntry = ProviderParser.getMedia(asset, this.mediaAsset.formats);
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

        static PKMediaEntry getMedia(KalturaMediaAsset assetInfo, List<String> formats) {
            return getMedia(assetInfo.getId() + "", assetInfo.getFiles(), formats);
        }

        static PKMediaEntry getMedia(String id, List<KalturaMediaFile> mediaFiles) {
            return getMedia(id, mediaFiles, null);
        }

        private synchronized static PKMediaEntry getMedia(String assetId, List<KalturaMediaFile> mediaFiles, List<String> formats) {
            PKMediaEntry mediaEntry = new PKMediaEntry();
            mediaEntry.setId("" + assetId);

            ArrayList<PKMediaSource> sources = new ArrayList<>();
            long maxDuration = 0;
            if (mediaFiles != null) {
                // if provided, only the "formats" matching MediaFiles should be parsed and added to the PKMediaEntry media sources
                for (KalturaMediaFile file : mediaFiles) {
                    if (formats == null || formats.contains(file.getType())) {
                        sources.add(new PKMediaSource().setId(file.getId() + "").setUrl(file.getUrl()).setMediaFormat(PKMediaFormat.valueOfUrl(file.getUrl())));
                        maxDuration = Math.max(file.getDuration(), maxDuration);
                    }
                }
            }
            return mediaEntry.setDuration(maxDuration).setSources(sources).setMediaType(MediaTypeConverter.toMediaEntryType(""));
        }
    }

    static class MediaTypeConverter{

        public static PKMediaEntry.MediaEntryType toMediaEntryType(String mediaType){
            switch (mediaType){
                default:
                    return PKMediaEntry.MediaEntryType.Unknown;
            }
        }
    }

}
