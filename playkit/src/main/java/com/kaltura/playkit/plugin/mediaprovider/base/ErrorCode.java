package com.kaltura.playkit.plugin.mediaprovider.base;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class ErrorCode {

    public static ErrorCode GeneralError = new ErrorCode("something went wrong", 666);
    public static ErrorCode MediaNotFound = new ErrorCode("Requested Media could not be located", 404);
    public static ErrorCode LoadError = new ErrorCode("Failed to load Media from source", 500);
    public static ErrorCode ConnectionError = new ErrorCode("Failed to connect to source", 408);

    private String message;
    private int code;

    public ErrorCode(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}
