package com.kaltura.playkit.connect;

/**
 * Created by tehilarozin on 06/11/2016.
 */

public class ErrorElement {

    public static ErrorElement GeneralError = new ErrorElement("Something went wrong", 666);
    public static ErrorElement NotFound = new ErrorElement("Resource not found", 404);
    public static ErrorElement LoadError = new ErrorElement("Failed to load data from source", 500);
    public static ErrorElement ServiceUnavailableError = new ErrorElement("Requested service is unavailable", 503);
    public static ErrorElement ConnectionError = new ErrorElement("Failed to connect to source", 408);
    public static ErrorElement BadRequestError = new ErrorElement("Invalid or missing request params", 400);
    public static ErrorElement SessionError = new ErrorElement("Failed to obtain session", 601);

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
        switch (exception.getClass().getSimpleName()){
            case "SocketTimeoutException":
            case "UnknownHostException":
                return ErrorElement.ConnectionError;

            default:
                return ErrorElement.GeneralError;

        }
    }
}
