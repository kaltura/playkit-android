package com.kaltura.playkit.utils.errors;

/**
 * Created by anton.afanasiev on 20/06/2017.
 */

public enum PKPlayerErrorType {

    SOURCE_ERROR(7000),
    RENDERER_ERROR(7001),
    UNEXPECTED(7002),
    SOURCE_SELECTION_FAILED(7003);

    public final int errorCode;

    PKPlayerErrorType(int errorCode) {
        this.errorCode = errorCode;
    }

}
