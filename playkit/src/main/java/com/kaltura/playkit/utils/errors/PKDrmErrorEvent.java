package com.kaltura.playkit.utils.errors;

/**
 * Created by anton.afanasiev on 21/06/2017.
 */

public class PKDrmErrorEvent extends PKError {

    public enum Type {
        ERROR
    }

    public PKDrmErrorEvent(PKErrorType errorType, String message, Throwable cause) {
        super(errorType, message, cause);
    }
}
