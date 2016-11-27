package com.kaltura.playkit.connect;

import java.net.SocketTimeoutException;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class ErrorElement {

    public static ErrorElement GeneralError = new ErrorElement("Something went wrong", 666);
    public static ErrorElement MediaNotFound = new ErrorElement("Requested Media could not be located", 404);
    public static ErrorElement LoadError = new ErrorElement("Failed to load data from source", 500);
    public static ErrorElement ConnectionError = new ErrorElement("Failed to connect to source", 408);
    public static ErrorElement BadRequestError = new ErrorElement("Invalid or missing request params", 400);

    private String message;
    private String code;
    private Object extra;

    public ErrorElement(String message, String code, Object extra) {
        this(message, code);
        this.extra = extra;
    }

    public ErrorElement(String message, int code) {
        this(message, code+"");
    }

    public ErrorElement(String message, String code) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * enable user to change default message with his own
     * @param message
     * @return
     */
    public ErrorElement message(String message){
        this.message = message;
        return this;
    }

    public ErrorElement addMessage(String message){
        this.message += "; " + message;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Object getExtra() {
        return extra;
    }

    public static ErrorElement fromException(Exception exception) {
        if(exception instanceof SocketTimeoutException) return ErrorElement.ConnectionError;

        return ErrorElement.GeneralError;
    }
}
