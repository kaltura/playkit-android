package com.kaltura.playkit.connect;


/**
 */
public interface RequestQueue {

    String queue(RequestElement request);

    ResponseElement execute(RequestElement request);

    void cancelRequest(String reqId);

    //boolean hasRequest(String reqId);

    void clearRequests();

    boolean isEmpty();
}
