package com.kaltura.playkit.plugin.mediaprovider.tvpapi;

import android.os.Bundle;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.core.OnCompletion;
import com.kaltura.playkit.plugin.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.plugin.connect.ResponseElement;
import com.kaltura.playkit.plugin.mediaprovider.base.OnMediaLoadCompletion;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class TvpapiMediaProvider implements MediaEntryProvider {

    private TvpapiRequestsHandler requestsHandler;
    private JsonObject initObj; //!! should be provided per user login, can't be used cross users.

    public TvpapiMediaProvider(String tvpapiAddress){
        requestsHandler = new TvpapiRequestsHandler(tvpapiAddress, new APIOkRequestsExecutor());
    }

    public void load(String id, Bundle args, OnCompletion completion){
        load((JsonObject)args.get("initObj"), id, args.getInt("typeId"), args.getString("formatName"), completion);
    }

    public void load(JsonObject initObj, String id, int typeId, String format, OnCompletion completion){
        this.initObj = initObj;
        loadInfo(id, typeId, format, completion);
    }

    private void loadInfo(String id, int typeId, final String format, final OnCompletion completion){
        requestsHandler.getMediaInfo(initObj, id, typeId, new OnCompletion<ResponseElement>() {
            @Override
            public void onComplete(ResponseElement response) {
                if(response != null && response.isSuccess()){
                    //-> we have here a string representing the assetinfo
                    //?? do we need to parse only the actual source to play? do we need a bacup source
                    //?? DRM according to what? - only to the actual source

                    PKMediaEntry mediaEntry = TvpapiParser.parseMediaEntry(response.getResponse(), format);
                    if(completion != null){
                        completion.onComplete(mediaEntry);
                    }
                }
            }
        });
    }


    public void setInitObj(JsonObject initObj) {
        this.initObj = initObj;
    }


    @Override
    public void load(OnMediaLoadCompletion completion) {

    }
}
