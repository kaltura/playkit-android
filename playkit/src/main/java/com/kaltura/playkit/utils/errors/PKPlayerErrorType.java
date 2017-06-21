package com.kaltura.playkit.utils.errors;

import com.kaltura.playkit.PlayerEvent;

/**
 * Created by anton.afanasiev on 20/06/2017.
 */

public enum PKPlayerErrorType implements PKErrorType {

    SOURCE_ERROR(7000),
    RENDERER_ERROR(7001),
    UNEXPECTED(7002),
    TRACKS_ERROR(7003);

    public final int errorCode;

    PKPlayerErrorType(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public Enum eventType() {
        return PlayerEvent.Type.ERROR;
    }
}
