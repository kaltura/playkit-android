package com.kaltura.playkit.backend.ovp;

import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
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
import com.kaltura.playkit.backend.ovp.data.KalturaMetadata;
import com.kaltura.playkit.backend.ovp.data.KalturaMetadataListResponse;
import com.kaltura.playkit.backend.ovp.data.KalturaPlaybackContext;
import com.kaltura.playkit.backend.ovp.data.KalturaPlaybackSource;
import com.kaltura.playkit.backend.ovp.services.BaseEntryService;
import com.kaltura.playkit.backend.ovp.services.MetaDataService;
import com.kaltura.playkit.backend.ovp.services.OvpService;
import com.kaltura.playkit.backend.ovp.services.OvpSessionService;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.MultiRequestBuilder;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Retention;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by tehilarozin on 30/10/2016.
 */

public class KalturaOvpMediaProvider extends BEMediaProvider {

    private static final String TAG = KalturaOvpMediaProvider.class.getSimpleName();
    public static final boolean CanBeEmpty = true;

    private String entryId;
    private String uiConfId;

    private int maxBitrate;
    private Map<String, Object> flavorsFilter;


    public  KalturaOvpMediaProvider() {
        super(KalturaOvpMediaProvider.TAG);
    }

    /**
     * MANDATORY! provides the baseUrl and the session token(ks) for the API calls.
     * @param sessionProvider
     * @return
     */
    public KalturaOvpMediaProvider setSessionProvider(SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
        return this;
    }

    /**
     * MANDATORY! the entry id, to fetch the data for.
     * @param entryId
     * @return
     */
    public KalturaOvpMediaProvider setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }

    /**
     * optional parameter.
     * Defaults to {@link com.kaltura.playkit.connect.APIOkRequestsExecutor} implementation.
     * @param executor
     * @return
     */
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
        return TextUtils.isEmpty(this.entryId) ?
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
            if(TextUtils.isEmpty(ks)){
                if(CanBeEmpty) {
                    PKLog.w(TAG, "provided ks is empty, Anonymous session will be used.");
                } else {
                    return ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token");
                }
            }
            return null;
        }

        private RequestBuilder getEntryInfo(String baseUrl, String ks, int partnerId, String entryId){
            MultiRequestBuilder multiRequestBuilder = (MultiRequestBuilder) OvpService.getMultirequest(baseUrl, ks, partnerId)
                    .tag("entry-info-multireq");

            if(TextUtils.isEmpty(ks)){
                multiRequestBuilder.add(OvpSessionService.anonymousSession(baseUrl, partnerId));

                ks = "{1:result:ks}";
            }

            return multiRequestBuilder.add(BaseEntryService.list(baseUrl, ks, entryId),
                    BaseEntryService.getPlaybackContext(baseUrl, ks, entryId),
                    MetaDataService.list(baseUrl, ks, entryId));
        }

        /**
         * Builds and passes to the executor, the multirequest for entry info and playback info fetching.
         * @param ks
         * @throws InterruptedException
         */
        @Override
        protected void requestRemote(final String ks) throws InterruptedException {
            final RequestBuilder entryRequest = getEntryInfo(getApiBaseUrl(), ks, sessionProvider.partnerId(), entryId)
                    .completion(new OnRequestCompletion() {
                        @Override
                        public void onComplete(ResponseElement response) {
                            PKLog.v(TAG, loadId + ": got response to [" + loadReq + "]" + " isCanceled = " + isCanceled);
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
            String sep = sessionProvider.baseUrl().endsWith("/") ? "" : "/";
            return sessionProvider.baseUrl() + sep + OvpConfigs.ApiPrefix;
        }

        /**
         * Parse and create a {@link PKMediaEntry} object from the multirequest call sent to the BE.
         * @param ks
         * @param response
         * @param completion - A callback to pass the constructed {@link PKMediaEntry} object on.
         * @throws InterruptedException - in case the load operation canceled.
         */
        private void onEntryInfoMultiResponse(String ks, ResponseElement response, OnMediaLoadCompletion completion) throws InterruptedException {
            ErrorElement error = null;
            PKMediaEntry mediaEntry = null;


            if (isCanceled()) {
                PKLog.v(TAG, loadId + ": i am canceled, exit response parsing ");
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

                    int entryListResponseIdx = responses.size() > 3 ? 1 : 0;
                    int playbackResponseIdx = entryListResponseIdx + 1;
                    int metadataResposneIdx = playbackResponseIdx + 1;

                    if (responses.get(entryListResponseIdx).error != null) {
                        error = responses.get(entryListResponseIdx).error.addMessage("baseEntry/list request failed");
                    }
                    if (error == null && responses.get(playbackResponseIdx).error != null) {
                        error = responses.get(playbackResponseIdx).error.addMessage("baseEntry/getPlaybackContext request failed");
                    }

                    if (error == null) {

                        KalturaPlaybackContext kalturaPlaybackContext = (KalturaPlaybackContext) responses.get(playbackResponseIdx);
                        KalturaMetadataListResponse metadataList = (KalturaMetadataListResponse) responses.get(metadataResposneIdx);

                        if ((error = hasError(kalturaPlaybackContext.getMessages())) == null) { // check for error message
                            mediaEntry = ProviderParser.getMediaEntry(sessionProvider.baseUrl(), ks, sessionProvider.partnerId() + "", uiConfId,
                                    ((KalturaBaseEntryListResponse) responses.get(entryListResponseIdx)).objects.get(0), kalturaPlaybackContext, metadataList);
 
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

    /**
     * checks if messages list contains at least 1 error message
     * @param messages
     * @return
     */
    private ErrorElement hasError(ArrayList<KalturaPlaybackContext.KalturaAccessControlMessage> messages) {
        ErrorElement error = null;
        // in case we'll want to gather errors or priorities message, loop over messages. Currently returns the first error
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
         * @param baseUrl
         * @param entry
         * @param playbackContext
         * @return (in case of restriction on maxbitrate, filtering should be done by considering the flavors provided to the
         *source- if none meets the restriction, source should not be added to the mediaEntrys sources.)
         */
        public static PKMediaEntry getMediaEntry(String baseUrl, String ks, String partnerId, String uiConfId, KalturaMediaEntry entry, KalturaPlaybackContext playbackContext,KalturaMetadataListResponse metadataList) throws InvalidParameterException {

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

            Map<String,String> metadata = parseMetadata(metadataList);

            return mediaEntry.setId(entry.getId()).setSources(sources)
                    .setDuration(entry.getMsDuration()).setMetadata(metadata)
                    .setMediaType(MediaTypeConverter.toMediaEntryType(entry.getType()));
        }

        private static Map<String, String> parseMetadata(KalturaMetadataListResponse metadataList) {
            Map<String, String> metadata = new HashMap<>();

            if (metadataList != null && metadataList.objects != null && metadataList.objects.size() > 0){
                for (KalturaMetadata metadataItem :metadataList.objects) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = null;
                    try {
                        builder = factory.newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    }
                    InputSource is = new InputSource(new StringReader(metadataItem.xml));
                    Document doc = null;
                    try {
                        doc = builder.parse(is);
                    } catch (SAXException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (doc != null){
                        NodeList list = doc.getElementsByTagName("metadata");
                        if (list!= null && list.getLength() ==1){
                            NodeList childList = list.item(0).getChildNodes();
                            for (int i = 0 ; i < childList.getLength() ; i++){
                                Node currentItem = childList.item(i);
                                metadata.put(currentItem.getNodeName(),currentItem.getFirstChild().getNodeValue());
                            }
                        }
                    }
                }
            }
            return metadata;
        }


        /**
         * Parse PKMediaSource objects from the getPlaybackContext API response.
         * Goes over the sources and creates for each supported source (supported format) a correlating
         * PKMediaSource item, initiate with the relevant data.
         *
         * @param baseUrl - baseUrl for the playing source construction
         * @param ks - if not empty, will be added to the playing url path
         * @param partnerId
         * @param uiConfId - if not empty, will be added to the playing url path
         * @param entry
         * @param playbackContext - the response object of the "baseEntry/getPlaybackContext" API.
         * @return - list of PKMediaSource created from sources list
         */
        @NonNull
        private static List<PKMediaSource> parseFromSources(String baseUrl, String ks, String partnerId, String uiConfId, KalturaMediaEntry entry, KalturaPlaybackContext playbackContext) {
            ArrayList<PKMediaSource> sources = new ArrayList<>();

            //-> create PKMediaSource-s according to sources list provided in "getContextData" response
            for (KalturaPlaybackSource playbackSource : playbackContext.getSources()) {

                if (!FormatsHelper.validateFormat(playbackSource)) { // only validated formats will be added to the sources.
                    continue;
                }

                String playUrl = null;
                PKMediaFormat mediaFormat = FormatsHelper.getPKMediaFormat(playbackSource.getFormat(), playbackSource.hasDrmData());

                // in case playbackSource doesn't have flavors we don't need to build the url and we'll use the provided one.
                if (playbackSource.hasFlavorIds()) {

                    String baseProtocol = null;
                    try {
                        baseProtocol = new URL(baseUrl).getProtocol();

                    } catch (MalformedURLException e){
                        PKLog.e(TAG, "Provided base url is wrong");
                        baseProtocol = OvpConfigs.DefaultHttpProtocol;
                    }

                    PlaySourceUrlBuilder playUrlBuilder = new PlaySourceUrlBuilder()
                            .setBaseUrl(baseUrl)
                            .setEntryId(entry.getId())
                            .setFlavorIds(playbackSource.getFlavorIds())
                            .setFormat(playbackSource.getFormat())
                            .setKs(ks)
                            .setPartnerId(partnerId)
                            .setUiConfId(uiConfId)
                            .setProtocol(playbackSource.getProtocol(baseProtocol)); //get protocol from base url

                    String extension = "";
                    //-> find out what should be the extension: if playbackSource format doesn't have mapped value, mediaFormat is null,
                    //->  the extension will be fetched from the flavorAssets.
                    if (mediaFormat == null) {
                        // filter the flavors that the playbackSource supports
                        List<KalturaFlavorAsset> flavorAssets = FlavorAssetsFilter.filter(playbackContext.getFlavorAssets(), "id", playbackSource.getFlavorIdsList());
                        if(flavorAssets.size() > 0) {
                            extension = flavorAssets.get(0).getFileExt();
                        }
                    } else {
                        extension = mediaFormat.pathExt;
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

                PKMediaSource pkMediaSource = new PKMediaSource().setUrl(playUrl).setId(entry.getId() + "_" + playbackSource.getDeliveryProfileId()).setMediaFormat(mediaFormat);
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
                    //Collection<PKMediaFormat> extensions = FormatsHelper.getSupportedExtensions();

                    for (Map.Entry<String,PKMediaFormat> mediaFormatEntry : FormatsHelper.SupportedFormats.entrySet()/*extensions*/) {
                        String formatName = mediaFormatEntry.getKey();//FormatsHelper.getFormatNameByMediaFormat(mediaFormat);
                        String playUrl = new PlaySourceUrlBuilder()
                                .setEntryId(entry.getId())
                                .setFlavorIds(flavorIds.toString())
                                .setKs(ks)
                                .setPartnerId(partnerId)
                                .setUiConfId(uiConfId)
                                .setExtension(mediaFormatEntry.getValue().pathExt)
                                .setFormat(formatName).build();

                        PKMediaSource mediaSource = new PKMediaSource().setId(entry.getId() + "_" + mediaFormatEntry.getValue().pathExt).setMediaFormat(mediaFormatEntry.getValue());
                        mediaSource.setUrl(playUrl);
                        sources.add(mediaSource);
                    }
                }
            }

            return sources;
        }

    }


    static class FormatsHelper {

        @Retention(SOURCE)
        @StringDef(value = {FormatName.MpegDash, FormatName.MpegDashDrm, FormatName.AppleHttp,
                FormatName.Url, FormatName.UrlDrm})
        public @interface FormatName {
            String MpegDash = "mpegdash";
            String MpegDashDrm = "mpegdash+drm";
            String AppleHttp = "applehttp";
            String Url = "url";
            String UrlDrm = "url+drm";
        }

        /**
         * to map BE format name to the matching format element in the {@link PKMediaFormat} enumeration.
         */
        private static final Map<String, PKMediaFormat> SupportedFormats = new HashMap<String, PKMediaFormat>() {{
            put(FormatName.MpegDash, PKMediaFormat.dash_clear);
            put(FormatName.MpegDashDrm, PKMediaFormat.dash_widevine);
            put(FormatName.AppleHttp, PKMediaFormat.hls_clear);
            put(FormatName.Url, PKMediaFormat.mp4_clear);
            put(FormatName.UrlDrm, PKMediaFormat.wvm_widevine);
        }};

        private static PKMediaFormat getPKMediaFormat(String format, boolean hasDrm) {
            switch (format) {
                case FormatName.MpegDash:
                    return hasDrm ? SupportedFormats.get(FormatName.MpegDashDrm) : SupportedFormats.get(FormatName.MpegDash);
                case FormatName.Url:
                    return hasDrm ? SupportedFormats.get(FormatName.UrlDrm) : SupportedFormats.get(FormatName.Url);
                case FormatName.AppleHttp:
                    return hasDrm ? null : SupportedFormats.get(FormatName.AppleHttp);
            }
            return null;
        }


        /**
         * checks if the format name from the source parameter has a matching supported {@link PKMediaFormat}
         * element.
         * @param source - playback source item
         * @return - true, if format is valid and supported
         */
        public static boolean validateFormat(KalturaPlaybackSource source) {
            PKMediaFormat format = getPKMediaFormat(source.getFormat(), source.hasDrmData());
            return format != null;
        }

    }


    public static class MediaTypeConverter {

        public static PKMediaEntry.MediaEntryType toMediaEntryType(KalturaEntryType type) {
            switch (type) {
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

