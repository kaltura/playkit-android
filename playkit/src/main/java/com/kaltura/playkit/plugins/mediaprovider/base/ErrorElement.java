package com.kaltura.playkit.plugins.mediaprovider.base;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class ErrorElement {

    public static ErrorElement GeneralError = new ErrorElement("something went wrong", 666);
    public static ErrorElement MediaNotFound = new ErrorElement("Requested Media could not be located", 404);
    public static ErrorElement LoadError = new ErrorElement("Failed to load Media from source", 500);
    public static ErrorElement ConnectionError = new ErrorElement("Failed to connect to source", 408);

    private String message;
    private int code;
    private Object extra;

    public ErrorElement(String message, int code, Object extra) {
        this(message, code);
        this.extra = extra;
    }

    public ErrorElement(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public Object getExtra() {
        return extra;
    }
}
