package com.kaltura.playkit.plugins.mediaproviders.ovp;

import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.plugins.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.plugins.connect.OnRequestCompletion;
import com.kaltura.playkit.plugins.connect.ResponseElement;
import com.kaltura.playkit.plugins.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.plugins.mediaproviders.ovp.data.KalturaEntryContextDataResult;
import com.kaltura.playkit.plugins.mediaproviders.ovp.data.KalturaMediaEntry;

/**
 * Created by tehilarozin on 30/10/2016.
 */

public class KalturaOvpMediaProvider implements MediaEntryProvider {

    private KalturaOvpRequestsHandler requestsHandler;
    private String entryId;
    private String ks;
    private int partnerId = 0;
    private int maxBitrate;


    public KalturaOvpMediaProvider(String baseUrl, String ks, String entryId) {
        requestsHandler = new KalturaOvpRequestsHandler(baseUrl, APIOkRequestsExecutor.getSingleton());
        this.ks = ks;
        this.entryId = entryId;
    }

    public KalturaOvpMediaProvider(String baseUrl, int partnerId, String entryId) {
        requestsHandler = new KalturaOvpRequestsHandler(baseUrl, APIOkRequestsExecutor.getSingleton());
        this.partnerId = partnerId;
        this.entryId = entryId;
    }

    public KalturaOvpMediaProvider ks(String ks){
        this.ks = ks;
        return this;
    }

    public KalturaOvpMediaProvider partnerId(int partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    @Override
    public void load(OnMediaLoadCompletion callback) {
        requestsHandler.listEntry(ks, partnerId, entryId, new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                //TODO: parse responses for Entry media objects and contextdata :
                if(response != null && response.isSuccess()){

                }
            }
        });
    }


    private static class KalturaOvpParser {

        public static PKMediaEntry parseMediaEntry(KalturaMediaEntry entry, KalturaEntryContextDataResult contextData){

            PKMediaEntry mediaEntry = new PKMediaEntry();
            mediaEntry.setId(entry.getId()).setSources(contextData.getSources()).setDuration(entry.getMsDuration());
            return mediaEntry;

            // how do we interact sources with flavors in case user set maxbitrate
        }

    }
}
