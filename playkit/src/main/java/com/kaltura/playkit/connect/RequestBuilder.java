package com.kaltura.playkit.connect;

import android.support.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A tool for creating a remote request that can be passed over http executor.
 * By activating the {@link RequestBuilder#build method}, we create a {@link RequestElement} object
 * This object can be queued or executed by a {@link RequestQueue} implementing component, such as
 * {@link APIOkRequestsExecutor}.
 *
 * The request is built in Builder style by setting the needed properties with "set" methods.
 *
 *
 *
 */

public class RequestBuilder<T extends RequestBuilder> {

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

    public T url(String url){
        this.baseUrl = url;
        return (T) this;
    }

    public T method(String method){
        this.method = method;
        return (T) this;
    }

    public T id(String id){
        this.id = id;
        return (T) this;
    }

    public T tag(String tag){
        this.tag = tag;
        return (T) this;
    }

    public T completion(OnRequestCompletion completion){
        this.completion = completion;
        return (T) this;
    }

    public T params(JsonObject params) {
        this.params = params;
        return (T) this;
    }

    public T service(String service) {
        this.service = service;
        return (T) this;
    }

    public T action(String action) {
        this.action = action;
        return (T) this;
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
                if (params != null) {
                    return params.toString();
                } else {
                    return null;
                }
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

    public T addParams(JsonObject others){
        if(others == null){
            return (T) this;
        }
        if(this.params == null){
            this.params = new JsonObject();
        }
        for(Map.Entry<String, JsonElement> entry : others.entrySet()) {
            this.params.add(entry.getKey(), entry.getValue());
        }

        return (T) this;
    }

    public T addParam(String key, String value) {
        if(this.params == null){
            this.params = new JsonObject();
        }
        this.params.addProperty(key, value);
        return (T) this;
    }

    public T removeParams(@NonNull String... keys){
        if(params == null) return (T) this;

        for (String key : keys){
            this.params.remove(key);
        }

        return (T) this;
    }
}
