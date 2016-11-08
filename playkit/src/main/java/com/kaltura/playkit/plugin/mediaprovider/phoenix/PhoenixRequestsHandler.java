package com.kaltura.playkit.plugin.mediaprovider.phoenix;

import com.google.gson.JsonObject;
import com.kaltura.playkit.plugin.connect.OnCompletion;
import com.kaltura.playkit.plugin.connect.RequestConfiguration;
import com.kaltura.playkit.plugin.connect.RequestElement;
import com.kaltura.playkit.plugin.connect.RequestQueue;
import com.kaltura.playkit.plugin.connect.ResponseElement;
import com.kaltura.playkit.plugin.mediaprovider.RequestsHandler;

import java.util.HashMap;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class PhoenixRequestsHandler extends RequestsHandler {

    public PhoenixRequestsHandler(String address, RequestQueue executor){
        super(address, executor);
    }

    public void getMediaInfo(final String ks, final String assetId, final String assetReferenceType, final OnCompletion<ResponseElement> completion){

        RequestElement requestElement = new RequestElement() {

            @Override
            public String getMethod() {
                return "POST";
            }

            @Override
            public String getUrl() {
                return baseUrl + "service/asset/action/get";
            }

            @Override
            public String getBody() {

                JsonObject body = new JsonObject();
                body.addProperty("ks", ks);
                body.addProperty("id", assetId);
                body.addProperty("assetReferenceType", assetReferenceType);

                return body.toString();
            }

            @Override
            public String getTag() {
                return "asset-get";
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
