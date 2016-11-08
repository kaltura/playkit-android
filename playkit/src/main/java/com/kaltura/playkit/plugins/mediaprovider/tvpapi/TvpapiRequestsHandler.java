package com.kaltura.playkit.plugins.mediaprovider.tvpapi;

import com.google.gson.JsonObject;
import com.kaltura.playkit.plugins.connect.OnRequestCompletion;
import com.kaltura.playkit.plugins.connect.RequestConfiguration;
import com.kaltura.playkit.plugins.connect.RequestElement;
import com.kaltura.playkit.plugins.connect.RequestQueue;
import com.kaltura.playkit.plugins.connect.ResponseElement;
import com.kaltura.playkit.plugins.mediaprovider.RequestsHandler;

import java.util.HashMap;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class TvpapiRequestsHandler extends RequestsHandler {

    public TvpapiRequestsHandler(String address, RequestQueue executor){
        super(address, executor);
    }

    public void getMediaInfo(final JsonObject initObj, final String mediaId, final OnRequestCompletion completion){

        RequestElement requestElement = new RequestElement() {

            @Override
            public String getMethod() {
                return "POST";
            }

            @Override
            public String getUrl() {
                return baseUrl + "SearchAssets";
            }

            @Override
            public String getBody() {

                JsonObject body = new JsonObject();
                body.add("initObj", initObj);
               // body.addProperty("filter_types", "["+mediaTypeId+"]");
                body.addProperty("filter", "media_id="+mediaId);
                body.addProperty("with", "[files]");
                body.addProperty("page_index", 0);
                body.addProperty("page_size", 1);

                return body.toString();
            }

            @Override
            public String getTag() {
                return "SearchAssets";
            }

            @Override
            public HashMap<String, String> getHeaders() {
                return null;
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public RequestConfiguration config() {
                return null;
            }

            @Override
            public void onComplete(ResponseElement responseElement) {
                if(completion != null){
                    completion.onComplete(responseElement);
                }
            }
        };

        requestsExecutor.queue(requestElement);
    }



    /*public static void getMediaLicensedLink(final OnCompletion completion, ){

    }

    public void getProgramLicensedLink(final OnCompletion completion, ){

    }

    public static void getRecordingLicensedLink(final OnCompletion completion, ){

    }*/


}
