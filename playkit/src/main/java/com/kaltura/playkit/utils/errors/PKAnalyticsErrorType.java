package com.kaltura.playkit.utils.errors;

/**
 * Created by anton.afanasiev on 20/06/2017.
 */

public enum PKAnalyticsErrorType implements PKErrorType {

    INVALID_INIT_OBJECT(2100);

    public final int errorCode;

    PKAnalyticsErrorType(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public Enum eventType() {
        return PKAnalyticsErrorEvent.Type.ERROR;
    }
}
