package com.connect.utils;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class MultiRequestBuilder extends RequestBuilder {

    private LinkedHashMap<String, RequestBuilder> calls;
    private int lastId = 0;


    public MultiRequestBuilder(RequestBuilder... builders){
        add(builders);
    }

    @Override
    public MultiRequestBuilder add(RequestBuilder... builders) {
        if (calls == null) {
            calls = new LinkedHashMap<>();
        }
        if (params == null) {
            params = new JsonObject();
        }

        for (RequestBuilder builder : builders) {
            if(builder instanceof MultiRequestBuilder){
                add((MultiRequestBuilder)builder);
                continue;
            }
            lastId++;
            String reqId = lastId + "";
            builder.params.addProperty("service", builder.service);
            builder.params.addProperty("action", builder.action);
            params.add(reqId, builder.params); // add single request params as an object of the multirequest reqId is the key
            calls.put(reqId, builder);
            builder.id(reqId);
        }

        return this;
    }

    /**
     * Adds the requests from kalturaMultiRequestBuilder parameter to the end of the current requests list
     * @param multiRequestBuilder - another multirequests to copy requests from
     * @return
     */
    public MultiRequestBuilder add(MultiRequestBuilder multiRequestBuilder) {
        for (String reqId : multiRequestBuilder.calls.keySet()) {
            RequestBuilder request = multiRequestBuilder.calls.get(reqId);
            int last = calls.size() + 1;
            calls.put(last + "", request);
            params.add(last + "", request.params);
        }

        return this;
    }

    public int getRequestsCount(){
        return calls != null ? calls.size() : 0;
    }
}
