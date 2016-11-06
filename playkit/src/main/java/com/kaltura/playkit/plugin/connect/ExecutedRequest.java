package com.kaltura.playkit.plugin.connect;

/**
 * Created by tehilarozin on 06/09/2016.
 */
public class ExecutedRequest implements ResponseElement {

    String requestId;
    int code = -1;
    String response = "";
    boolean isSuccess = false;


    public ExecutedRequest requestId(String id) {
        this.requestId = id;
        return this;
    }

    public ExecutedRequest code(int code) {
        this.code = code;
        return this;
    }

    public ExecutedRequest response(String response) {
        this.response = response;
        return this;
    }

    public ExecutedRequest success(boolean success) {
        this.isSuccess = success;
        return this;
    }

        @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getResponse() {
        return response;
    }

    @Override
    public boolean isSuccess() {
        return isSuccess;
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

}

