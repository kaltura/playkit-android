package com.kaltura.playkit.connect;

/**
 * @hide
 */
public class ExecutedRequest implements ResponseElement {

    String requestId;
    int code = -1;
    String response = "";
    boolean isSuccess = false;
    ErrorElement error = null;

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

    public ExecutedRequest error(ErrorElement error) {
        this.error = error;
        return this;
    }

    public ExecutedRequest error(Exception exception) {
        this.error = ErrorElement.fromException(exception);
        return this;
    }

    @Override
    public String getCode() {
        return code+"";
    }

    @Override
    public String getResponse() {
        return response;
    }

    @Override
    public ErrorElement getError() {
        return error;
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

