package com.kaltura.playkit.backend.ovp;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.data.KalturaEntryContextDataResult;
import com.kaltura.playkit.backend.ovp.data.KalturaEntryList;
import com.kaltura.playkit.backend.ovp.data.KalturaMediaEntry;
import com.kaltura.playkit.backend.ovp.data.KalturaSource;
import com.kaltura.playkit.backend.ovp.data.OvpResultAdapter;
import com.kaltura.playkit.backend.ovp.services.BaseEntryService;
import com.kaltura.playkit.backend.phoenix.data.BaseResult;
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
import java.util.List;

/**
 * Created by tehilarozin on 30/10/2016.
 */

public class KalturaOvpMediaProvider implements MediaEntryProvider {

    private RequestQueue requestsExecutor;
    private String entryId;
    private SessionProvider sessionProvider;
    private int maxBitrate;


    public KalturaOvpMediaProvider() {
        requestsExecutor =  APIOkRequestsExecutor.getSingleton();
    }


    public KalturaOvpMediaProvider setSessionProvider(SessionProvider sessionProvider){
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

        final RequestBuilder entryRequest = (MultiRequestBuilder) BaseEntryService.entryInfo(sessionProvider.baseUrl(), sessionProvider.getKs(), /*sessionProvider.partnerId(),*/ entryId)
                .completion( new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                onEntryInfoMultiResponse(response, completion);
            }
        });
        requestsExecutor.queue(entryRequest.build());
    }

    private ErrorElement validateKs() {
        return TextUtils.isEmpty(this.sessionProvider.getKs()) ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError +": SessionProvider should provide a valid KS token") :
                null;
    }

    private ErrorElement validateEntry() {
        return TextUtils.isEmpty(this.entryId) ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": Missing required parameters, entryId") :
                null;
    }


    private void onEntryInfoMultiResponse(ResponseElement response, OnMediaLoadCompletion completion) {
        ErrorElement error = null;
        PKMediaEntry mediaEntry = null;

        if(response != null && response.isSuccess()){

            try{
                //convert response to responses array
                List<Object> responses = KalturaOvpParser.parseMultiresponse(response.getResponse(), TextUtils.isEmpty(sessionProvider.getKs()) ? 1 : 0, KalturaEntryList.class, KalturaEntryContextDataResult.class);

                if(!(responses.get(0) instanceof KalturaEntryList)){
                    error = ErrorElement.LoadError.message("baseEntry/list request failed");
                }
                if(!(responses.get(1) instanceof KalturaEntryContextDataResult)){
                    error = ErrorElement.LoadError.message("baseEntry/getContextData request failed");
                }

                if(error == null) {
                    mediaEntry = KalturaOvpParser.getMediaEntry(((KalturaEntryList) responses.get(0)).objects.get(0), (KalturaEntryContextDataResult) responses.get(1));
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



    private static class KalturaOvpParser {

        static List<Object> parseMultiresponse(String response, int parseFromIdx, @NonNull Class...types) throws JsonSyntaxException {

            JsonParser parser = new JsonParser();
            JsonElement responseElement = parser.parse(response);
            ArrayList<Object> responsesObjects = new ArrayList<>();
            if(responseElement.isJsonArray()){
                JsonArray responses = responseElement.getAsJsonArray();
                Gson gson = new GsonBuilder().registerTypeAdapter(BaseResult.class, new OvpResultAdapter()).create();
                int tIdx = 0;
                for(int i = parseFromIdx; i <= responses.size() && tIdx < types.length; i++, tIdx++){
                    responsesObjects.add(gson.fromJson(responses.get(i), types[tIdx]));
                }
            }
            return responsesObjects;
        }

        /**
         * creates {@link PKMediaEntry} from entry's data and contextData
         * @param entry
         * @param contextData
         * @return
         * (in case of restriction on maxbitrate, filtering should be done by considering the flavors provided to the
         * source- if none meets the restriction, source should not be added to the mediaEntrys sources.)
         */
        public static PKMediaEntry getMediaEntry(KalturaMediaEntry entry, KalturaEntryContextDataResult contextData){

            PKMediaEntry mediaEntry = new PKMediaEntry();
            ArrayList<KalturaSource> kalturaSources = contextData.getSources();
            ArrayList<PKMediaSource> sources = new ArrayList<>();

            //sources with multiple drm data should be split to mediasource per drm
            for(KalturaSource kalturaSource : kalturaSources){
                List<KalturaSource.Drm> drmData = kalturaSource.getDrmData();
                if(drmData != null && drmData.size() > 0){
                    for(KalturaSource.Drm drm : drmData) {
                        PKMediaSource pkMediaSource = new PKMediaSource().setUrl(kalturaSource.getUrl()).setId(kalturaSource.getId()+""); //!! source in mock doesn't have id - if source is per drm data - what will be the id
                        sources.add(pkMediaSource.setDrmData(new PKDrmParams(drm.getLicenseURL())));
                    }
                } else {
                    sources.add(new PKMediaSource().setUrl(kalturaSource.getUrl()).setId(kalturaSource.getId()+""));
                }
            }

            return mediaEntry.setId(entry.getId()).setSources(sources).setDuration(entry.getMsDuration());
        }

    }
}
