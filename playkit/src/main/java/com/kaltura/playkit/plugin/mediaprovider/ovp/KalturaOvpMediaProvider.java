package com.kaltura.playkit.plugin.mediaprovider.ovp;

import com.kaltura.playkit.MediaEntry;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.plugin.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.plugin.connect.OnCompletion;
import com.kaltura.playkit.plugin.connect.ResponseElement;

/**
 * Created by tehilarozin on 30/10/2016.
 */

public class KalturaOvpMediaProvider implements MediaEntryProvider {

    KalturaOvpRequestsHandler requestsHandler;

    public KalturaOvpMediaProvider(String beAddress){
        requestsHandler = new KalturaOvpRequestsHandler(beAddress, new APIOkRequestsExecutor());
    }

    @Override
    public MediaEntry getMediaEntry() {
        return null;
    }

    @Override
    public void load(OnCompletion callback, String id, Object... extraArgs) {
        load(callback, id, extraArgs[0], extraArgs[1], extraArgs[2]);
    }

    /*public void load(String ks, int partnerId, String entryId){
        load(ks, partnerId, entryId, -1);
    }*/

    /**
     *
     * @param ks - if not provided - should be created with startWidgetSession and added to the multirequest as first request
     * @param partnerId
     * @param entryId
     */
    public void load(OnCompletion completion, String entryId, String ks, int partnerId, int maxBitrate){
        requestsHandler.listEntry(ks, partnerId, entryId, new OnCompletion<ResponseElement>() {
            @Override
            public void onComplete(ResponseElement response) {
                //TODO: parse responses for Entry media objects and contextdata :

            }
        });
    }

    public void load(int partnerId, String entryId){

    }

    public void load(int partnerId, String entryId, int maxBitrate){

    }
}
