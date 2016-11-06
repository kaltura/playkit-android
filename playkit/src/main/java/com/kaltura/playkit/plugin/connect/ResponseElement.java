package com.kaltura.playkit.plugin.connect;

/**
 * Created by tehilarozin on 06/09/2016.
 */
public interface ResponseElement {

    //String getContentType();

    int getCode();

    String getResponse();

    boolean isSuccess();

    String getRequestId();

}
