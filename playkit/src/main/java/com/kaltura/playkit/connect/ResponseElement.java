package com.kaltura.playkit.connect;

/**
 * Created by tehilarozin on 06/09/2016.
 */
public interface ResponseElement extends ResultElement<String> {

    int getCode();

    String getRequestId();

}
