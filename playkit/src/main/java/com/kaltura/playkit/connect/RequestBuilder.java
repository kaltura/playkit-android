package com.kaltura.playkit.connect;

import android.support.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tehilarozin on 09/11/2016.
 */

public class RequestBuilder {

    protected String service = null;
    protected String action = null;
    protected JsonObject params;

    private String baseUrl;
    private String method;
    private String id;
    private String tag = null;
    private Map<String, String> headers;
    private RequestConfiguration configuration = null;
    private OnRequestCompletion completion;

    public RequestBuilder(){
        headers = new HashMap();
        headers.put("ContentType", "application/json");
    }

    public RequestBuilder url(String url){
        this.baseUrl = url;
        return this;
    }

    public RequestBuilder method(String method){
        this.method = method;
        return this;
    }

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

    public MultiRequestBuilder add(RequestBuilder... requestBuilder){
        return new MultiRequestBuilder(this).add(requestBuilder);
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
                if(baseUrl == null){

                }
                StringBuilder urlBuilder = new StringBuilder(baseUrl);
                if(service != null){
                    urlBuilder.append("service/").append(service);
                }
                if(action != null){
                    urlBuilder.append("/action/").append(action);
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
            public Map getHeaders() {
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

    public RequestBuilder addParams(JsonObject others){
        if(others == null){
            return this;
        }
        if(this.params == null){
            this.params = new JsonObject();
        }
        for(Map.Entry<String, JsonElement> entry : others.entrySet()) {
            this.params.add(entry.getKey(), entry.getValue());
        }

        return this;
    }

    public RequestBuilder addParam(String key, String value) {
        if(this.params == null){
            this.params = new JsonObject();
        }
        this.params.addProperty(key, value);
        return this;
    }

    public RequestBuilder removeParams(@NonNull String... keys){
        if(params == null) return this;

        for (String key : keys){
            this.params.remove(key);
        }

        return this;
    }
}
