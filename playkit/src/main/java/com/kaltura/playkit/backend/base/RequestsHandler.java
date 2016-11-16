package com.kaltura.playkit.backend.base;

import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.RequestQueue;

/**
 * Created by tehilarozin on 30/10/2016.
 */
@Deprecated
public abstract class RequestsHandler {

    protected String baseUrl;
    protected RequestQueue requestsExecutor;

    protected RequestsHandler(){}

    protected RequestsHandler(String baseUrl, RequestQueue executor){
        this.requestsExecutor = executor;
        this.baseUrl = baseUrl;
    }


    protected ErrorElement generateErrorResponse(final String requestId, final String message, final int code){

        return new ErrorElement(message, code, requestId);

        /*return new ResponseElement() {
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
        };*/
    }
}
