package com.kaltura.playkit.connect;

import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Created by tehilarozin on 20/11/2016.
 */

public class BaseRequestBuilder {
    protected JsonObject params;
    private String baseUrl = "";
    private String method;
    private String id;
    private String tag = null;
    protected Map<String, String> headers;
    private RequestConfiguration configuration = null;
    protected OnRequestCompletion completion;


    /*public RequestBuilder url(String url){
        this.baseUrl = url;
        return this;
    }

    public RequestBuilder method(String method){
        this.method = method;
        return this;
    }*/

}
