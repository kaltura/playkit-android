package com.kaltura.playkit.plugin.mediaprovider;

import com.kaltura.playkit.plugin.connect.RequestQueue;
import com.kaltura.playkit.plugin.connect.ResponseElement;

/**
 * Created by tehilarozin on 30/10/2016.
 */
// ????
public abstract class RequestsHandler {

    protected String baseUrl;
    protected RequestQueue requestsExecutor;

    protected RequestsHandler(){}

    protected RequestsHandler(String baseUrl, RequestQueue executor){
        this.requestsExecutor = executor;
        this.baseUrl = baseUrl;
    }


    protected ResponseElement generateErrorResponse(final String requestId, final String message, final int code){
        return new ResponseElement() {
            @Override
            public int getCode() {
                return code;
            }

            @Override
            public String getResponse() {
                return message;
            }

            @Override
            public boolean isSuccess() {
                return false;
            }

            @Override
            public String getRequestId() {
                return requestId;
            }
        };
    }
}
