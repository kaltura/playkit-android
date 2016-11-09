package com.kaltura.playkit.connect;


/**
 * Created by tehilarozin on 08/08/2016.
 */
public interface RequestQueue {

    String queue(RequestElement action);

    String queue(ParamsRequestElement action);

    ResponseElement execute(RequestElement action);

    void cancelAction(String actionId);

    void clearActions();

    boolean isEmpty();
}
