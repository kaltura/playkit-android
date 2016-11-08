package com.kaltura.playkit.plugins.mediaproviders.tvpapi;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.core.OnCompletion;
import com.kaltura.playkit.plugins.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.plugins.connect.OnRequestCompletion;
import com.kaltura.playkit.plugins.connect.ResponseElement;
import com.kaltura.playkit.plugins.connect.ResultElement;
import com.kaltura.playkit.plugins.mediaproviders.base.ErrorElement;
import com.kaltura.playkit.plugins.mediaproviders.base.OnMediaLoadCompletion;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class TvpapiMediaProvider implements MediaEntryProvider {

    private TvpapiRequestsHandler requestsHandler;
    private JsonObject initObj; //!! should be provided per user login, can't be used cross users.
    private String assetId;
    private String format;


    public TvpapiMediaProvider(String baseUrl, JsonObject initObj, String assetId){
        requestsHandler = new TvpapiRequestsHandler(baseUrl, APIOkRequestsExecutor.getSingleton());
        this.initObj = initObj;
        this.assetId = assetId;
    }

    public void format(String format) {
        this.format = format;
    }

    private void loadInfo(String id, int typeId, final String format, final OnCompletion completion){

    }


    public void setInitObj(JsonObject initObj) {
        this.initObj = initObj;
    }


    @Override
    public void load(final OnMediaLoadCompletion completion) {
        requestsHandler.getMediaInfo(initObj, assetId, new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if(response != null && response.isSuccess()){
                    //-> we have here a string representing the assetinfo
                    //?? do we need to parse only the actual source to play? do we need a bacup source
                    //?? DRM according to what? - only to the actual source

                    final PKMediaEntry mediaEntry = TvpapiParser.parseMediaEntry(response.getResponse(), format);
                    if(completion != null){
                        completion.onComplete(new ResultElement<PKMediaEntry>() {
                            @Override
                            public PKMediaEntry getResponse() {
                                return mediaEntry;
                            }

                            @Override
                            public boolean isSuccess() {
                                return mediaEntry != null;
                            }

                            @Override
                            public ErrorElement getError() {
                                return mediaEntry == null ? ErrorElement.LoadError : null; // TODO: add error handling
                            }
                        });
                    }
                }
            }
        });
    }
}
