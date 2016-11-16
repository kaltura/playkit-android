package com.kaltura.playkit.connect;

/**
 * Created by tehilarozin on 13/11/2016.
 */

public class MultiRequestBuilder extends RequestBuilder {

    private int lastId = 0;


    public MultiRequestBuilder(RequestBuilder... builders){
        add(builders);
    }

    public MultiRequestBuilder add(RequestBuilder... builders) {
        /*if (calls == null) {
            calls = new LinkedHashMap<>();
        }*/

        for (RequestBuilder builder : builders) {
            lastId++;
            String reqId = lastId + "";
            builder.params.addProperty("service", builder.service);
            builder.params.addProperty("action", builder.action);
            params.add(reqId, builder.params); // add single request params as an object of the multirequest reqId is the key
            //calls.put(reqId, builder);
            //builder.setId(reqId);
        }

        return this;
    }
}
