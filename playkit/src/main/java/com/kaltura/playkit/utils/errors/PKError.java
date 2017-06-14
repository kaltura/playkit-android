package com.kaltura.playkit.utils.errors;

/**
 * Created by anton.afanasiev on 13/06/2017.
 */

public class PKError {

    public final String message;
    public final Throwable cause;
    public final int errorCode;

    public PKError(int errorCode, String message, Throwable cause) {
        this.errorCode = errorCode;
        this.message = message;
        this.cause = cause;
    }

    public class PlayerError {

        public static final int SOURCE_ERROR = 7000;
        public static final int RENDERER_ERROR = 7001;
        public static final int UNEXPECTED = 7002;
        public static final int TRACKS_ERROR = 7003;

    }

    public class AnalyticsPluginError {

        public static final int INVALID_INIT_OBJECT = 2100;
    }
}
