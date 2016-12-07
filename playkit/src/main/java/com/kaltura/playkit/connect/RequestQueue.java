package com.kaltura.playkit.connect;


/**
 * Created by tehilarozin on 08/08/2016.
 */
public interface RequestQueue {

    String queue(RequestElement request);

    ResponseElement execute(RequestElement request);

    void cancelRequest(String reqId);

    //boolean hasRequest(String reqId);

    void clearRequests();

    boolean isEmpty();
}
