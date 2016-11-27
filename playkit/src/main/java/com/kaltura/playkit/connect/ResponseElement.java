package com.kaltura.playkit.connect;

/**
 * Created by tehilarozin on 06/09/2016.
 */
public interface ResponseElement extends ResultElement<String> {

    public static String Ok = "200";

    String getCode();

    String getRequestId();

}
