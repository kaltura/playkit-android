package com.kaltura.playkit.backend.ovp;

import android.support.annotation.NonNull;

import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.backend.SessionProvider;
import com.kaltura.playkit.backend.base.BECallableLoader;
import com.kaltura.playkit.backend.base.BEMediaProvider;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.data.FlavorAssetsFilter;
import com.kaltura.playkit.backend.ovp.data.KalturaBaseEntryListResponse;
import com.kaltura.playkit.backend.ovp.data.KalturaEntryContextDataResult;
import com.kaltura.playkit.backend.ovp.data.KalturaEntryType;
import com.kaltura.playkit.backend.ovp.data.KalturaFlavorAsset;
import com.kaltura.playkit.backend.ovp.data.KalturaMediaEntry;
import com.kaltura.playkit.backend.ovp.data.KalturaPlaybackContext;
import com.kaltura.playkit.backend.ovp.data.KalturaPlaybackSource;
import com.kaltura.playkit.backend.ovp.services.BaseEntryService;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.text.TextUtils.isEmpty;

/**
 * Created by tehilarozin on 30/10/2016.
 */

public class KalturaOvpMediaProvider extends BEMediaProvider {

    private static final String TAG = KalturaOvpMediaProvider.class.getSimpleName();

    private String entryId;
    private String uiConfId;

    private int maxBitrate;
    private Map<String, Object> flavorsFilter;


    public KalturaOvpMediaProvider() {
        super(KalturaOvpMediaProvider.TAG);
    }


    public KalturaOvpMediaProvider setSessionProvider(SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
        return this;
    }

    public KalturaOvpMediaProvider setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }

    public KalturaOvpMediaProvider setRequestExecutor(RequestQueue executor) {
        this.requestsExecutor = executor;
        return this;
    }

    /**
     * optional parameter
     * will be used in media sources url
     *
     * @param uiConfId
     * @return
     */
    public KalturaOvpMediaProvider setUiConfId(String uiConfId) {
        this.uiConfId = uiConfId;
        return this;
    }

    @Override
    protected Loader factorNewLoader(OnMediaLoadCompletion completion) {
        return new Loader(requestsExecutor, sessionProvider, entryId, uiConfId, completion);
    }

    @Override
    protected ErrorElement validateParams() {
        return isEmpty(this.entryId) ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": Missing required parameters, entryId") :
                null;
    }


    class Loader extends BECallableLoader {

        private String entryId;
        private String uiConfId;

        Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, String entryId, String uiConfId, OnMediaLoadCompletion completion) {
            super(KalturaOvpMediaProvider.TAG + "#Loader", requestsExecutor, sessionProvider, completion);

            this.entryId = entryId;
            this.uiConfId = uiConfId;

            PKLog.v(TAG, loadId + ": construct new Loader");
        }

        @Override
        protected ErrorElement validateKs(String ks) {
            return isEmpty(ks) ?
                    ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token") :
                    null;
        }

        @Override
        protected void requestRemote(final String ks) throws InterruptedException {
            final RequestBuilder entryRequest = BaseEntryService.entryInfo(getApiBaseUrl(), ks, entryId)
                    .completion(new OnRequestCompletion() {
                        @Override
                        public void onComplete(ResponseElement response) {
                            PKLog.v(TAG, loadId + ": got response to [" + loadReq + "]" + " isCanceled = "+isCanceled);
                            loadReq = null;

                            try {
                                onEntryInfoMultiResponse(ks, response, (OnMediaLoadCompletion) completion);
                            } catch (InterruptedException e) {
                                interrupted();
                            }
                        }
                    });

            synchronized (syncObject) {
                loadReq = requestQueue.queue(entryRequest.build());
                PKLog.d(TAG, loadId + ": request queued for execution [" + loadReq + "]");
            }
            waitCompletion();
        }

        private String getApiBaseUrl() {
            return sessionProvider.baseUrl()+OvpConfigs.ApiPrefix;
        }


        private void onEntryInfoMultiResponse(String ks, ResponseElement response, OnMediaLoadCompletion completion) throws InterruptedException {
            ErrorElement error = null;
            PKMediaEntry mediaEntry = null;

            if (isCanceled()) {
                PKLog.v(TAG, loadId+": i am canceled, exit response parsing ");
                return;
            }

            if (response != null && response.isSuccess()) {

                try {
                    //parse multi response from request response

                /* in this option, in case of error response, the type of the parsed response will be BaseResult, and not the expected object type,
                   since we parse the type dynamically from the result and we get "KalturaAPIException" objectType */
                    List<BaseResult> responses = KalturaOvpParser.parse(response.getResponse());//, TextUtils.isEmpty(sessionProvider.getSessionToken()) ? 1 : 0, KalturaBaseEntryListResponse.class, KalturaEntryContextDataResult.class);
                /* in this option, responses types will always be as expected, and in case of an error, the error can be reached from the typed object, since
                * all response objects should extend BaseResult */
                    //  List<BaseResult> responses = (List<BaseResult>) KalturaOvpParser.parse(response.getResponse(), KalturaBaseEntryListResponse.class, KalturaEntryContextDataResult.class);

                    if (responses.get(0).error != null) {
                        error = responses.get(0).error.addMessage("baseEntry/list request failed");
                    }
                    if (error == null && responses.get(1).error != null) {
                        error = responses.get(1).error.addMessage("baseEntry/getPlaybackContext request failed");
                        /*if(responses.get(2).error != null) {
                            error = responses.get(1).error.addMessage("baseEntry/getPlaybackContext request failed");
                        }*/
                    }

                    if (error == null) {

                        KalturaPlaybackContext kalturaPlaybackContext = (KalturaPlaybackContext) responses.get(1);
                        /*KalturaPlaybackContext kalturaPlaybackContext = responses.get(1) instanceof KalturaPlaybackContext ?
                                (KalturaPlaybackContext) responses.get(1) :
                                new KalturaPlaybackContext((KalturaEntryContextDataResult) responses.get(2));*/

                        if ((error = hasError(kalturaPlaybackContext.getMessages())) == null) { // check for error message
                            mediaEntry = ProviderParser.getMediaEntry(sessionProvider.baseUrl(), ks, sessionProvider.partnerId() + "", uiConfId,
                                    ((KalturaBaseEntryListResponse) responses.get(0)).objects.get(0), kalturaPlaybackContext);

                            if (mediaEntry.getSources().size() == 0) { // makes sure there are sources available for play
                                error = ErrorElement.RestrictionError.message("Content can't be played due to lack of sources");
                            }
                        }
                    }

                } catch (JsonSyntaxException ex) {
                    error = ErrorElement.LoadError.message("failed parsing remote response: " + ex.getMessage());
                } catch (InvalidParameterException ex) {
                    error = ErrorElement.LoadError.message("failed to create PKMediaEntry: " + ex.getMessage());
                }

            } else {
                error = response != null && response.getError() != null ? response.getError() : ErrorElement.LoadError;
            }

            PKLog.v(TAG, loadId + ": load operation " + (isCanceled() ? "canceled" : "finished with " + (error == null ? "success" : "failure: " + error)));


            if (!isCanceled() && completion != null) {
                completion.onComplete(Accessories.buildResult(mediaEntry, error));
            }

            notifyCompletion();

        }

    }


    // in case we'll want to gather errors or priorities message, loop over messages. Currently returns the first error
    private ErrorElement hasError(ArrayList<KalturaPlaybackContext.KalturaAccessControlMessage> messages) {
        ErrorElement error = null;
        for (KalturaPlaybackContext.KalturaAccessControlMessage message : messages) {
            error = KalturaOvpErrorHelper.getErrorElement(message.getCode(), message.getMessage());
            if (error != null) {
                return error;
            }
        }

        return null;
    }


    private static class ProviderParser {


        /**
         * creates {@link PKMediaEntry} from entry's data and contextData
         *
         *
         * @param baseUrl
         * @param entry
         * @param playbackContext
         * @return (in case of restriction on maxbitrate, filtering should be done by considering the flavors provided to the
         *source- if none meets the restriction, source should not be added to the mediaEntrys sources.)
         */
        public static PKMediaEntry getMediaEntry(String baseUrl, String ks, String partnerId, String uiConfId, KalturaMediaEntry entry, KalturaPlaybackContext playbackContext) throws InvalidParameterException {

            PKMediaEntry mediaEntry = new PKMediaEntry();
            ArrayList<KalturaPlaybackSource> kalturaSources = playbackContext.getSources();
            List<PKMediaSource> sources;

            if (kalturaSources != null && kalturaSources.size() > 0) {
                sources = parseFromSources(baseUrl, ks, partnerId, uiConfId, entry, playbackContext);
            } else {
                sources = new ArrayList<>();
            }
            /*
            in case we need default sources creation:
            else {
                PKLog.e(TAG, "failed to receive sources to play");
                //throw new InvalidParameterException("Could not create sources for media entry");
                sources = parseFromFlavors(ks, partnerId, uiConfId, entry, playbackContext);
            }*/

            return mediaEntry.setId(entry.getId()).setSources(sources).setDuration(entry.getMsDuration()).setMediaType(MediaTypeConverter.toMediaEntryType(entry.getType()));
        }

        private static Map<String, Object> getMediaMetadata(KalturaMediaEntry entry) {
           // if(entry.getType() == )
            return null;
        }
//!! PKMediaSource id is not unique - if contains more than 1 drm each of the drm will be converted to a source
//!! AndroidCharacter will have the same id.


        @NonNull
        static List<PKMediaSource> parseFromFlavors(String ks, String partnerId, String uiConfId, KalturaMediaEntry entry, KalturaEntryContextDataResult contextData) {

            ArrayList<PKMediaSource> sources = new ArrayList<>();

            if (contextData != null) {
                //-> filter a list for flavors correspond to the list of "flavorParamsId"s received on the entry data response.
                List<KalturaFlavorAsset> matchingFlavorAssets = FlavorAssetsFilter.filter(contextData.getFlavorAssets(), "flavorParamsId", entry.getFlavorParamsIdsList());

                //-> construct a string of "ids" from the filtered KalturaFlavorAsset list.
                StringBuilder flavorIds = new StringBuilder(matchingFlavorAssets.size() > 0 ? matchingFlavorAssets.get(0).getId() : "");
                for (int i = 1; i < matchingFlavorAssets.size(); i++) {
                    flavorIds.append(",").append(matchingFlavorAssets.get(i).getId());
                }

                if (flavorIds.length() > 0) {
                    //-> create PKMediaSource for every predefine extension:
                    Collection<String> extensions = FormatsHelper.getSupportedExtensions();

                    for (String ext : extensions) {
                        String format = FormatsHelper.getFormatByExtension(ext);
                        String playUrl = new PlaySourceUrlBuilder()
                                .setEntryId(entry.getId())
                                .setFlavorIds(flavorIds.toString())
                                .setKs(ks)
                                .setPartnerId(partnerId)
                                .setUiConfId(uiConfId)
                                .setExtension(ext)
                                .setFormat(format == null ? "mp4" : format).build(); //"mp4" - code not in use

                        PKMediaSource mediaSource = new PKMediaSource().setId(entry.getId() + "_" + ext);
                        mediaSource.setUrl(playUrl);
                        sources.add(mediaSource);
                    }
                }
            }

            return sources;
        }

    }
    //          "url":"http://cdnapi.kaltura.com/p/2209591/sp/0/playManifest/entryId/1_1h1vsv3z/format/url/protocol/http/a.mp4/flavorIds/0_,1_ude4l5pb,1_izgi81qa,1_3l6wh2jz,1_fafwf2t7,1_k6gs4dju,1_nzen8kfl"

    @NonNull
    private static List<PKMediaSource> parseFromSources(String baseUrl, String ks, String partnerId, String uiConfId, KalturaMediaEntry entry, KalturaPlaybackContext playbackContext) {
        ArrayList<PKMediaSource> sources = new ArrayList<>();

        //-> create PKMediaSource-s according to sources list provided in "getContextData" response
        for (KalturaPlaybackSource playbackSource : playbackContext.getSources()) {

            if (!FormatsHelper.validateFormat(playbackSource)) { // only validated formats will be added to the sources.
                continue;
            }

            String playUrl = null;

            // in case playbackSource doesn't have flavors we don't need to build the url and we'll use the provided one.(exp: live content)
            if (playbackSource.hasFlavorIds()) {

                PlaySourceUrlBuilder playUrlBuilder = new PlaySourceUrlBuilder()
                        .setBaseUrl(baseUrl)
                        .setEntryId(entry.getId())
                        .setFlavorIds(playbackSource.getFlavorIds())
                        .setFormat(playbackSource.getFormat())
                        .setKs(ks)
                        .setPartnerId(partnerId)
                        .setUiConfId(uiConfId)
                        .setProtocol(playbackSource.getProtocol(OvpConfigs.PreferredHttpProtocol));

                String extension;
                //-> find out what should be the extension: if format doesn't have mapped value, the extension will be fetched from the flavorAssets.
                if ((extension = FormatsHelper.getExtByFormat(playbackSource.getFormat())) == null) {
                    List<KalturaFlavorAsset> flavorAssets = FlavorAssetsFilter.filter(playbackContext.getFlavorAssets(), "id", playbackSource.getFlavorIdsList());
                    extension = flavorAssets.size() > 0 ? flavorAssets.get(0).getFileExt() : FormatsHelper.getExtByFormat(playbackSource.getFormat());
                }
                playUrlBuilder.setExtension(extension);

                playUrl = playUrlBuilder.build();

            } else {
                playUrl = playbackSource.getUrl();
            }

            if (playUrl == null) {
                PKLog.w(TAG, "failed to create play url from source, discarding source:" + (entry.getId() + "_" + playbackSource.getDeliveryProfileId()) + ", " + playbackSource.getFormat());
                continue;
            }

            //!! we don't have id for the media source
            PKMediaSource pkMediaSource = new PKMediaSource().setUrl(playUrl).setId(entry.getId() + "_" + playbackSource.getDeliveryProfileId());
            //-> sources with multiple drm data are split to PKMediaSource per drm
            List<KalturaPlaybackSource.KalturaDrmEntryPlayingPluginData> drmData = playbackSource.getDrmData();
            if (drmData != null) {
                List<PKDrmParams> drmParams = new ArrayList<>();
                for (KalturaPlaybackSource.KalturaDrmEntryPlayingPluginData drm : drmData) {
                    drmParams.add(new PKDrmParams(drm.getLicenseURL()));
                }
                pkMediaSource.setDrmData(drmParams);
            }

            sources.add(pkMediaSource);

        }

        return sources;
    }


    static class FormatsHelper {

        private static final Map<String, String> SupportedFormats = new HashMap<String, String>() {{
            put("mpegdash", "mpd");
            put("applehttp", "m3u8"); // without drm
            put("url", "mp4"); //if format is "url it can be mp4 or wvm - this is the default
        }};

        public static String getExtByFormat(@NonNull String format) {
            return SupportedFormats.get(format);
        }

        public static String getFormatByExtension(@NonNull String format) {
            for (Map.Entry<String, String> entry : SupportedFormats.entrySet()) {
                if (entry.getValue().equals(format)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        /**
         * Check if format is not empty and it's supported.
         * <p>
         * "applehttp" format is supported only if doesn't have drm.
         *
         * @param source
         * @return - true, if format is valid and supported
         */
        public static boolean validateFormat(KalturaPlaybackSource source) {
            String format = source.getFormat();
            return !isEmpty(format) && SupportedFormats.keySet().contains(format) &&
                    (!format.equals("applehttp") || Utils.isNullOrEmpty(source.getDrmData()));
        }

        public static Collection<String> getSupportedExtensions() {
            return SupportedFormats.values();
        }

    }

    public static class MediaTypeConverter {

        public static PKMediaEntry.MediaEntryType toMediaEntryType(KalturaEntryType type){
            switch (type){
                case MEDIA_CLIP:
                    return PKMediaEntry.MediaEntryType.Vod;
                case LIVE_STREAM:
                    return PKMediaEntry.MediaEntryType.Live;
                default:
                    return PKMediaEntry.MediaEntryType.Unknown;
            }
        }
    }

}

