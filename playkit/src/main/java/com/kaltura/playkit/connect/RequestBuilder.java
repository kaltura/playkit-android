package com.kaltura.playkit.connect;

import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * Created by tehilarozin on 09/11/2016.
 */

public class RequestBuilder {

    protected String service = null;
    protected String action = null;
    protected JsonObject params;

    private String baseUrl;
    private String method;
    //String body;
    private String id;
    private String tag = null;
    private HashMap<String, String> headers = null;
    private RequestConfiguration configuration = null;
    private OnRequestCompletion completion;

    public RequestBuilder url(String url){
        this.baseUrl = url;
        return this;
    }

    public RequestBuilder method(String method){
        this.method = method;
        return this;
    }

    /*public RequestBuilder body(String body){
        this.body = body;
        return this;
    }*/

    public RequestBuilder id(String id){
        this.id = id;
        return this;
    }

    public RequestBuilder tag(String tag){
        this.tag = tag;
        return this;
    }

    public RequestBuilder completion(OnRequestCompletion completion){
        this.completion = completion;
        return this;
    }

    public RequestBuilder params(JsonObject params) {
        this.params = params;
        return this;
    }

    public RequestBuilder service(String service) {
        this.service = service;
        return this;
    }

    public RequestBuilder action(String action) {
        this.action = action;
        return this;
    }

    public MultiRequestBuilder add(RequestBuilder requestBuilder){
        return new MultiRequestBuilder(this, requestBuilder);
    }

    public RequestElement build(){
        return new RequestElement() {

            @Override
            public String getMethod() {
                return method;
            }

            @Override
            public String getUrl() {
                //return TextUtils.join("/", new String[]{baseUrl,service,action});

                StringBuilder urlBuilder = new StringBuilder(baseUrl);
                if(service != null){
                    urlBuilder.append("/").append(service);
                }
                if(action != null){
                    urlBuilder.append("/").append(action);
                }

                return urlBuilder.toString();
            }

            @Override
            public String getBody() {
                return params.toString();
            }

            @Override
            public String getTag() {
                return tag;
            }

            @Override
            public HashMap<String, String> getHeaders() {
                return headers;
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public RequestConfiguration config() {
                return configuration;
            }

            @Override
            public void onComplete(final ResponseElement response) {
                if(completion != null){
                    completion.onComplete(response);
                }
            }
        };

    }


}
