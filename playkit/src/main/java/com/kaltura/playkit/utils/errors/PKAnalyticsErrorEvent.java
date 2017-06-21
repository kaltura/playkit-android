package com.kaltura.playkit.utils.errors;

/**
 * Created by anton.afanasiev on 21/06/2017.
 */

public class PKAnalyticsErrorEvent extends PKError {

    public enum Type {
        ERROR
    }

    public PKAnalyticsErrorEvent(PKErrorType errorType, String message, Throwable cause) {
        super(errorType, message, cause);
    }
}
