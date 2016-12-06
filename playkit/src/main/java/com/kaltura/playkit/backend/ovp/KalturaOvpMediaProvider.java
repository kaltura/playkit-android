package com.kaltura.playkit.backend.ovp;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.backend.SessionProvider;
import com.kaltura.playkit.backend.base.BECallableLoader;
import com.kaltura.playkit.backend.base.BEMediaProvider;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.data.FlavorAssetsFilter;
import com.kaltura.playkit.backend.ovp.data.KalturaBaseEntryListResponse;
import com.kaltura.playkit.backend.ovp.data.KalturaEntryContextDataResult;
import com.kaltura.playkit.backend.ovp.data.KalturaFlavorAsset;
import com.kaltura.playkit.backend.ovp.data.KalturaMediaEntry;
import com.kaltura.playkit.backend.ovp.data.KalturaPlayingResult;
import com.kaltura.playkit.backend.ovp.data.KalturaPlayingSource;
import com.kaltura.playkit.backend.ovp.services.BaseEntryService;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.text.TextUtils.isEmpty;

/**
 * Created by tehilarozin on 30/10/2016.
 */

public class KalturaOvpMediaProvider extends BEMediaProvider /*implements MediaEntryProvider*/ {

    private static final String TAG = KalturaOvpMediaProvider.class.getSimpleName();

    //private RequestQueue requestsExecutor;
    private String entryId;
    //private SessionProvider sessionProvider;
    private String uiConfId;

    private int maxBitrate;
    private Map<String, Object> flavorsFilter;

    //private final Object syncObject = new Object();
    //private Future<Void> currentLoad;
    //private ExecutorService exSvc;


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

    /*@Override
    public void load(final OnMediaLoadCompletion completion) {

        ErrorElement error = validateEntry();
        if (error != null) {
            if (completion != null) {
                completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, error));
            }
            return;
        }

        cancel(); // cancel prev load if has any

        //synchronized (syncObject) {
        currentLoad = exSvc.submit(factorNewLoader(completion));
        PKLog.i(TAG, "new loader started "+currentLoad.toString());
            //currentLoad.run();
        //}
    }*/

    @Override
    protected Loader factorNewLoader(OnMediaLoadCompletion completion) {
        return new Loader(requestsExecutor, sessionProvider, entryId, uiConfId, completion);
    }

    /*@Override
    public synchronized void cancel() {
        if (currentLoad != null && !currentLoad.isDone() && !currentLoad.isCancelled()) {
            PKLog.i(TAG, "has running load operation, canceling current load operation - " + currentLoad.toString());
            currentLoad.cancel(true);
        } else {
            PKLog.i(TAG, (currentLoad != null ? currentLoad.toString() : "") + ": no need to cancel operation," + (currentLoad == null ? "operation is null" : (currentLoad.isDone() ? "operation done" : "operation canceled")));
        }
    }*/

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
            super(KalturaOvpMediaProvider.TAG+"#Loader", requestsExecutor, sessionProvider, completion);

            this.entryId = entryId;
            this.uiConfId = uiConfId;

            PKLog.i(TAG, loadId + ": construct new Loader");
        }

        @Override
        protected ErrorElement validateKs(String ks) {
            return isEmpty(ks) ?
                    ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token") :
                    null;
        }

        @Override
        protected void requestRemote(final String ks) throws InterruptedException {
            final RequestBuilder entryRequest = BaseEntryService.entryInfo(sessionProvider.baseUrl(), ks, entryId)
            .completion(new OnRequestCompletion() {
                @Override
                public void onComplete(ResponseElement response) {
                    onEntryInfoMultiResponse(ks, response, (OnMediaLoadCompletion) completion);
                }
            });
            requestQueue.queue(entryRequest.build());
        }

        /*@Override
        protected void load() {
            PKLog.i(TAG, loadId + ": load: start on get ks ");

            sessionProvider.getKs(new OnCompletion<String>() {
                @Override
                public void onComplete(String response) {
                    ErrorElement error = validateKs(response);
                    if (error == null) {
                        final String ks = response;
                        final RequestBuilder entryRequest = BaseEntryService.entryInfo(sessionProvider.baseUrl(), ks, *//*sessionProvider.partnerId(),*//* entryId)
                                .completion(new OnRequestCompletion() {
                                    @Override
                                    public void onComplete(ResponseElement response) {
                                        onEntryInfoMultiResponse(ks, response, (OnMediaLoadCompletion) completion);
                                    }
                                });
                        requestsExecutor.queue(entryRequest.build());

                    } else {
                        if (completion != null) {
                            completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, error));
                        }
                    }
                }
            });

        }*/

        private void onEntryInfoMultiResponse(String ks, ResponseElement response, OnMediaLoadCompletion completion) {
            ErrorElement error = null;
            PKMediaEntry mediaEntry = null;

            if (response != null && response.isSuccess()) {

                try {
                    //parse multi response from request respone

                /* in this option, in case of error response, the type of the parsed response will be BaseResult, and not the expected object type,
                   since we parse the type dynamically from the result and we get "KalturaAPIException" objectType */
                    List<BaseResult> responses = KalturaOvpParser.parse(response.getResponse());//, TextUtils.isEmpty(sessionProvider.getKs()) ? 1 : 0, KalturaBaseEntryListResponse.class, KalturaEntryContextDataResult.class);
                /* in this option, responses types will always be as expected, and in case of an error, the error can be reached from the typed object, since
                * all response objects should extend BaseResult */
                    //  List<BaseResult> responses = (List<BaseResult>) KalturaOvpParser.parse(response.getResponse(), KalturaBaseEntryListResponse.class, KalturaEntryContextDataResult.class);

                    if (responses.get(0).error != null) {
                        error = responses.get(0).error.addMessage("baseEntry/list request failed");//ErrorElement.LoadError.message("baseEntry/list request failed");
                    }
                    if (error == null && responses.get(1).error != null) {
                        error = responses.get(1).error.addMessage("baseEntry/getPlayingData request failed");
                    }

                    if (error == null) {
                        mediaEntry = ProviderParser.getMediaEntry(ks, sessionProvider.partnerId() + "", uiConfId,
                                ((KalturaBaseEntryListResponse) responses.get(0)).objects.get(0), (KalturaPlayingResult) responses.get(1));
                    }

                } catch (JsonSyntaxException ex) {
                    error = ErrorElement.LoadError.message("failed parsing remote response: " + ex.getMessage());
                } catch (InvalidParameterException ex) {
                    error = ErrorElement.LoadError.message("failed to create PKMediaEntry: " + ex.getMessage());
                }

            } else {
                error = response != null && response.getError() != null ? response.getError() : ErrorElement.LoadError;
            }

            PKLog.i(TAG, loadId + ": load operation "+(isCanceled() ? "canceled" : "finished with " + (error == null ? "success" : "failure")));


            if (!isCanceled() && completion != null) {
                completion.onComplete(Accessories.buildResult(mediaEntry, error));
            }

            notifyCompletion();

        }

    }


    private static class ProviderParser {


        /**
         * creates {@link PKMediaEntry} from entry's data and contextData
         *
         * @param entry
         * @param playingResult
         * @return (in case of restriction on maxbitrate, filtering should be done by considering the flavors provided to the
         *source- if none meets the restriction, source should not be added to the mediaEntrys sources.)
         */
        public static PKMediaEntry getMediaEntry(String ks, String partnerId, String uiConfId, KalturaMediaEntry entry, KalturaPlayingResult playingResult) throws InvalidParameterException {

            PKMediaEntry mediaEntry = new PKMediaEntry();
            ArrayList<KalturaPlayingSource> kalturaSources = playingResult.getSources();
            List<PKMediaSource> sources;

            if (kalturaSources != null && kalturaSources.size() > 0) {
                sources = parseFromSources(ks, partnerId, uiConfId, entry, playingResult);

            } else {
                PKLog.e(TAG, "failed to receive sources to play");
                throw new InvalidParameterException("Could not create sources for media entry");
                //sources = parseFromFlavors(ks, partnerId, uiConfId, entry, contextData);
            }

            return mediaEntry.setId(entry.getId()).setSources(sources).setDuration(entry.getMsDuration());
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
                    Set<String> extensions = PlaySourceUrlBuilder.getExtensions();

                    for (String ext : extensions) {

                        String playUrl = new PlaySourceUrlBuilder()
                                .setEntryId(entry.getId())
                                .setFlavorIds(flavorIds.toString())
                                .setKs(ks)
                                .setPartnerId(partnerId)
                                .setUiConfId(uiConfId)
                                .setExtension(ext)
                                .setFormat(/*PlaySourceUrlBuilder.getFormatByExt(ext)*/"mp4").build(); //"mp4" - code not in use

                        PKMediaSource mediaSource = new PKMediaSource().setId(entry.getId() + "_ext" + ext);
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
    private static List<PKMediaSource> parseFromSources(String ks, String partnerId, String uiConfId, KalturaMediaEntry entry, KalturaPlayingResult playingResult) {
        ArrayList<PKMediaSource> sources = new ArrayList<>();

        //-> create PKMediaSource-s according to sources list provided in "getContextData" response
        for (KalturaPlayingSource kalturaSource : playingResult.getSources()) {
            PlaySourceUrlBuilder playUrlBuilder = new PlaySourceUrlBuilder()
                    .setEntryId(entry.getId())
                    .setFlavorIds(TextUtils.join(",", kalturaSource.getFlavors()))
                    .setFormat(kalturaSource.getFormat())
                    .setKs(ks)
                    .setPartnerId(partnerId)
                    .setUiConfId(uiConfId)
                    .setProtocol(kalturaSource.getProtocol(OvpConfigs.PreferredHttpProtocol));

            String extension;
            //-> find out what should be the extension: if format doesn't have mapped value, the extension will be fetched from the flavorAssets.
            if((extension = PlaySourceUrlBuilder.getExtByFormat(kalturaSource.getFormat())) == null) {
                List<KalturaFlavorAsset> flavorAssets = FlavorAssetsFilter.filter(playingResult.getFlavorAssets(), "id", kalturaSource.getFlavors());
                extension = flavorAssets.size() > 0 ? flavorAssets.get(0).getFileExt() : PlaySourceUrlBuilder.getExtByFormat(kalturaSource.getFormat());
            }

            playUrlBuilder.setExtension(extension);

            String playUrl = playUrlBuilder.build();
            if(playUrl == null){
                PKLog.w(TAG, "failed to create play url from source, discarding source:" + (entry.getId()+"_"+kalturaSource.getDeliveryProfileId())+", "+kalturaSource.getFormat());
                continue;
            }

            //!! we don't have id for the media source
            PKMediaSource pkMediaSource = new PKMediaSource().setUrl(playUrl).setId(entry.getId()+"_"+kalturaSource.getDeliveryProfileId());
            //-> sources with multiple drm data are split to PKMediaSource per drm
            List<KalturaPlayingSource.Drm> drmData = kalturaSource.getDrmData();
            if (drmData != null) {
                List<PKDrmParams> drmParams = new ArrayList<>();
                for (KalturaPlayingSource.Drm drm : drmData) {
                    drmParams.add(new PKDrmParams(drm.getLicenseURL()));
                }
                pkMediaSource.setDrmData(drmParams);
            }

            sources.add(pkMediaSource);

        }

        /*TODO: !! comment out the parse with no source and raise some error (??)
        !! before using the context response for source creationm, check the error field on the contextrresponse (add this filed)
         !! format mapps to extension (not bidi)
         !! with format set the extension - in case the format is "url" use the flavor extension definition (flavor at 0)


         TODO:add cancel option on the provider
         in case load activated again before previous request ends - raise error.

         TODO:MediaSource will have list of DrmData - not single. (instead of creating source per drm element)



         */

        return sources;
    }


}

