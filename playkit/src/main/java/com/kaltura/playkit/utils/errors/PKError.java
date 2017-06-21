package com.kaltura.playkit.utils.errors;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 13/06/2017.
 */

public class PKError implements PKEvent {

    public final String message;
    public final Throwable cause;
    public final PKErrorType errorType;

    public PKError(PKErrorType errorType, String message, Throwable cause) {
        this.errorType = errorType;
        this.message = message;
        this.cause = cause;
    }

    @Override
    public Enum eventType() {
        return errorType.eventType();
    }


}
