package com.kaltura.playkit.plugin.mediaprovider.ovp;

import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.plugin.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.core.OnCompletion;
import com.kaltura.playkit.plugin.connect.RequestQueue;
import com.kaltura.playkit.plugin.connect.ResponseElement;
import com.kaltura.playkit.plugin.mediaprovider.base.ProviderBuilder;
import com.kaltura.playkit.plugin.mediaprovider.ovp.data.KalturaEntryContextDataResult;
import com.kaltura.playkit.plugin.mediaprovider.ovp.data.KalturaMediaEntry;

/**
 * Created by tehilarozin on 30/10/2016.
 */

public class KalturaOvpMediaProvider implements MediaEntryProvider {

    KalturaOvpRequestsHandler requestsHandler;
    String entryId;
    String ks;
    int partnerId = 0;
    int maxBitrate;


    private KalturaOvpMediaProvider(String baseUrl, RequestQueue requestQueue, String ks, int partnerId, String entryId) {
        requestsHandler = new KalturaOvpRequestsHandler(baseUrl, (requestQueue != null ? requestQueue : APIOkRequestsExecutor.getSingleton()));
        this.ks = ks;
        this.partnerId = partnerId;
        this.entryId = entryId;
    }

    @Override
    public PKMediaEntry getMediaEntry() {
        return null;
    }

    @Override
    public void load(OnCompletion callback) {
        requestsHandler.listEntry(ks, partnerId, entryId, new OnCompletion<ResponseElement>() {
            @Override
            public void onComplete(ResponseElement response) {
                //TODO: parse responses for Entry media objects and contextdata :
                if(response != null && response.isSuccess()){

                }
            }
        });
    }

    public static class Builder implements ProviderBuilder {
        String baseUrl;
        String entryId;
        String ks;
        int maxBitrate;
        int partnerId;
        RequestQueue requestQueue;

        public Builder setEntryId(String entryId) {
            this.entryId = entryId;
            return this;
        }

        public Builder setKs(String ks) {
            this.ks = ks;
            return this;
        }

        public Builder setMaxBitrate(int maxBitrate) {
            this.maxBitrate = maxBitrate;
            return this;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public void setRequestQueue(RequestQueue requestQueue) {
            this.requestQueue = requestQueue;
        }

        public Builder setPartnerId(int partnerId) {
            this.partnerId = partnerId;
            return this;
        }

        @Override
        public MediaEntryProvider build() {
            return new KalturaOvpMediaProvider(baseUrl, requestQueue, ks, partnerId, entryId);
        }


        static class KalturaOvpParser {

            public static PKMediaEntry parseMediaEntry(KalturaMediaEntry entry, KalturaEntryContextDataResult contextData){

                PKMediaEntry mediaEntry = new PKMediaEntry();
                mediaEntry.setId(entry.getId()).setSources(contextData.getSources()).setDuration(entry.getMsDuration());
                return mediaEntry;

                // how do we interact sources with flavors in case user set maxbitrate
            }


        }
    }
}
