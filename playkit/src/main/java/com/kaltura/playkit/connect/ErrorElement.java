package com.kaltura.playkit.connect;

/**
 */

public class ErrorElement {

    public static ErrorElement GeneralError = new ErrorElement("GeneralError", "Something went wrong", 666);
    public static ErrorElement NotFound = new ErrorElement("NotFound", "Resource not found", 404);
    public static ErrorElement LoadError = new ErrorElement("LoadError", "Failed to load data from source", 500);
    public static ErrorElement ServiceUnavailableError = new ErrorElement("ServiceUnavailableError", "Requested service is unavailable", 503);
    public static ErrorElement ConnectionError = new ErrorElement("ConnectionError", "Failed to connect to source", 408);
    public static ErrorElement BadRequestError = new ErrorElement("BadRequestError", "Invalid or missing request params", 400);
    public static ErrorElement SessionError = new ErrorElement("SessionError", "Failed to obtain session", 601);

    public String name;
    private String message;
    private String code;
    protected Object extra;

    public ErrorElement(String name, String message, int code) {
        this(message, code);
        this.name = name;
    }

    public ErrorElement(String message, String code, Object extra) {
        this.message = message;
        this.code = code;
        this.extra = extra;
    }

    public ErrorElement(String message, int code) {
        this(message, code + "");
    }

    public ErrorElement(String message, String code) {
        this(message, code, null);
    }

    public String getMessage() {
        return message;
    }

    /**
     * enable user to change default message with his own
     *
     * @param message
     * @return
     */
    public ErrorElement message(String message) {
        this.message = message;
        return this;
    }

    public ErrorElement addMessage(String message) {
        this.message += "; " + message;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Object getExtra() {
        return extra;
    }

    public static ErrorElement fromCode(int code, String message) {
        switch (code) {
            case 404:
                return ErrorElement.NotFound.message(message);
            case 400:
                return ErrorElement.BadRequestError.message(message);
            default:
                return new ErrorElement(message, code);
        }
    }

    public static ErrorElement fromException(Exception exception) {
        switch (exception.getClass().getSimpleName()) {
            case "SocketTimeoutException":
            case "UnknownHostException":
                return ErrorElement.ConnectionError;

            default:
                return ErrorElement.GeneralError;

        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(name != null){
            builder.append("Error: ").append(name).append("; ");
        }
        builder.append("code:").append(code).append(", Message:").append(message);
        return builder.toString();
    }
}
