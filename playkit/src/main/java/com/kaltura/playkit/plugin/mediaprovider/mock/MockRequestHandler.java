package com.kaltura.playkit.plugin.mediaprovider.mock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.playkit.plugin.connect.OnCompletion;
import com.kaltura.playkit.plugin.connect.ResponseElement;
import com.kaltura.playkit.plugin.mediaprovider.RequestsHandler;

import java.io.FileReader;
import java.io.IOException;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class MockRequestHandler extends RequestsHandler {

    MockRequestHandler(){
    }

    public void getMockMedia(String fileName, OnCompletion completion){
        JsonParser parser = new JsonParser();
        try {
            final JsonElement element = (JsonObject) parser.parse(new FileReader(fileName));
            ResponseElement response = new ResponseElement() {
                @Override
                public int getCode() {
                    return 200;
                }

                @Override
                public String getResponse() {
                    return element.toString();
                }

                @Override
                public boolean isSuccess() {
                    return true;
                }

                @Override
                public String getRequestId() {
                    return null;
                }
            };

            if (completion != null) {
                completion.onComplete(response);
            }
        } catch (IOException e){
            if (completion != null) {
                completion.onComplete(generateErrorResponse(null, "failed to load Media from input file: "+fileName, 500));
            }
        }
    }

}
