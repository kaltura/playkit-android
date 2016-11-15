package com.kaltura.playkit.connect;

import java.util.HashMap;

/**
 * Created by tehilarozin on 09/11/2016.
 */

public class RequestBuilder {

    String url;
    String method;
    String body;
    String id;
    String tag = null;
    HashMap<String, String> headers = null;
    RequestConfiguration configuration = null;

    OnRequestCompletion completion;

    public RequestBuilder url(String url){
        this.url = url;
        return this;
    }

    public RequestBuilder method(String method){
        this.method = method;
        return this;
    }

    public RequestBuilder body(String body){
        this.body = body;
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


    public RequestElement build(){
        return new RequestElement() {

            @Override
            public String getMethod() {
                return method;
            }

            @Override
            public String getUrl() {
                return url;
            }

            @Override
            public String getBody() {
                return body;
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
