package com.kaltura.playkit.backend.ovp;

import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.data.KalturaBaseEntryListResponse;
import com.kaltura.playkit.backend.ovp.data.KalturaEntryContextDataResult;
import com.kaltura.playkit.backend.ovp.data.KalturaMediaEntry;
import com.kaltura.playkit.backend.ovp.data.KalturaSource;
import com.kaltura.playkit.backend.ovp.services.BaseEntryService;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.connect.SessionProvider;

import java.util.ArrayList;
import java.util.List;

import static android.text.TextUtils.isEmpty;

/**
 * Created by tehilarozin on 30/10/2016.
 */

public class KalturaOvpMediaProvider implements MediaEntryProvider {

    private RequestQueue requestsExecutor;
    private String entryId;
    private SessionProvider sessionProvider;
    private int maxBitrate;


    public KalturaOvpMediaProvider() {
        requestsExecutor = APIOkRequestsExecutor.getSingleton();
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

    @Override
    public void load(final OnMediaLoadCompletion completion) {

        ErrorElement error = validateKs();
        if (error != null || (error = validateEntry()) != null) {
            if (completion != null) {
                completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, error));
            }
            return;
        }

        final RequestBuilder entryRequest = BaseEntryService.entryInfo(sessionProvider.baseUrl(), sessionProvider.getKs(), /*sessionProvider.partnerId(),*/ entryId)
                .completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {
                        onEntryInfoMultiResponse(response, completion);
                    }
                });
        requestsExecutor.queue(entryRequest.build());
    }

    private ErrorElement validateKs() {
        return isEmpty(this.sessionProvider.getKs()) ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token") :
                null;
    }

    private ErrorElement validateEntry() {
        return isEmpty(this.entryId) ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": Missing required parameters, entryId") :
                null;
    }


    private void onEntryInfoMultiResponse(ResponseElement response, OnMediaLoadCompletion completion) {
        ErrorElement error = null;
        PKMediaEntry mediaEntry = null;

        if(response != null && response.isSuccess()){

            try{
                //parse multi response from request respone

                /* in this option, in case of error response, the type of the parsed response will be BaseResult, and not the expected object type,
                   since we parse the type dynamically from the result and we get "KalturaAPIException" objectType */
                List<BaseResult> responses = KalturaOvpParser.parseArray(response.getResponse());//, TextUtils.isEmpty(sessionProvider.getKs()) ? 1 : 0, KalturaBaseEntryListResponse.class, KalturaEntryContextDataResult.class);
                /* in this option, responses types will always be as expected, and in case of an error, the error can be reached from the typed object, since
                * all response objects should extend BaseResult */
                //  List<BaseResult> responses = (List<BaseResult>) KalturaOvpParser.parse(response.getResponse(), KalturaBaseEntryListResponse.class, KalturaEntryContextDataResult.class);

                if(responses.get(0).error != null){
                    error = responses.get(0).error.addMessage("baseEntry/list request failed");//ErrorElement.LoadError.message("baseEntry/list request failed");
                }
                if(error == null && responses.get(1).error != null){
                    error = responses.get(1).error.addMessage("baseEntry/getContextData request failed");;//ErrorElement.LoadError.message("baseEntry/getContextData request failed");
                }

                if(error == null) {
                    mediaEntry = ProviderParser.getMediaEntry(((KalturaBaseEntryListResponse) responses.get(0)).objects.get(0), (KalturaEntryContextDataResult) responses.get(1));
                }

            } catch (JsonSyntaxException ex){
              error = ErrorElement.LoadError.message("failed parsing remote response: " + ex.getMessage());
            }

        } else {
            error = response != null && response.getError() != null ? response.getError() : ErrorElement.LoadError;
        }

        if(completion != null){
            completion.onComplete(Accessories.buildResult(mediaEntry, error));
        }
    }



    private static class ProviderParser {

        /**
         * creates {@link PKMediaEntry} from entry's data and contextData
         *
         * @param entry
         * @param contextData
         * @return (in case of restriction on maxbitrate, filtering should be done by considering the flavors provided to the
         *source- if none meets the restriction, source should not be added to the mediaEntrys sources.)
         */
        public static PKMediaEntry getMediaEntry(KalturaMediaEntry entry, KalturaEntryContextDataResult contextData) {

            PKMediaEntry mediaEntry = new PKMediaEntry();
            ArrayList<KalturaSource> kalturaSources = contextData.getSources();
            ArrayList<PKMediaSource> sources = new ArrayList<>();

            //sources with multiple drm data should be split to mediasource per drm
            for (KalturaSource kalturaSource : kalturaSources) {
                List<KalturaSource.Drm> drmData = kalturaSource.getDrmData();
                if (drmData != null && drmData.size() > 0) {
                    for (KalturaSource.Drm drm : drmData) {
                        PKMediaSource pkMediaSource = new PKMediaSource().setUrl(kalturaSource.getUrl()).setId(kalturaSource.getId() + ""); //!! source in mock doesn't have id - if source is per drm data - what will be the id
                        sources.add(pkMediaSource.setDrmData(new PKDrmParams(drm.getLicenseURL())));
                    }
                } else {
                    sources.add(new PKMediaSource().setUrl(kalturaSource.getUrl()).setId(kalturaSource.getId() + ""));
                }
            }

            return mediaEntry.setId(entry.getId()).setSources(sources).setDuration(entry.getMsDuration());
        }
//!! PKMediaSource id is not unique - if contains more than 1 drm each of the drm will be converted to a source
//!! AndroidCharacter will have the same id.
    }
}
