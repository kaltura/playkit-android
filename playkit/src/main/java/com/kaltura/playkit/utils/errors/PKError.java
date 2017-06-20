package com.kaltura.playkit.utils.errors;

/**
 * Created by anton.afanasiev on 13/06/2017.
 */

public class PKError {

    public static final int PLAYER_ERROR = 0;
    public static final int ANALYTICS_PLUGIN_ERROR = 1;
    public static final int IMA_ERROR = 2;
    public static final int OFFLINE_ERROR = 3;
    public static final int DRM_ERROR = 4;


    public final String message;
    public final Throwable cause;
    public final PKErrorType errorType;

    public PKError(PKErrorType errorType, String message, Throwable cause) {
        this.errorType = errorType;
        this.message = message;
        this.cause = cause;
    }

    public enum PKErrorType {
        //Player errors:
        SOURCE_ERROR(PLAYER_ERROR, 7000),
        RENDERER_ERROR(PLAYER_ERROR, 7001),
        UNEXPECTED(PLAYER_ERROR, 7002),
        TRACKS_ERROR(PLAYER_ERROR, 7003),

        //Ads errors:
        INTERNAL_ERROR(IMA_ERROR, 2000),
        VAST_MALFORMED_RESPONSE(IMA_ERROR, 2001),
        UNKNOWN_AD_RESPONSE(IMA_ERROR, 2002),
        VAST_LOAD_TIMEOUT(IMA_ERROR, 2003),
        VAST_TOO_MANY_REDIRECTS(IMA_ERROR, 2004),
        VIDEO_PLAY_ERROR(IMA_ERROR, 2005),
        VAST_MEDIA_LOAD_TIMEOUT(IMA_ERROR, 2006),
        VAST_LINEAR_ASSET_MISMATCH(IMA_ERROR, 2007),
        OVERLAY_AD_PLAYING_FAILED(IMA_ERROR, 2008),
        OVERLAY_AD_LOADING_FAILED(IMA_ERROR, 2009),
        VAST_NONLINEAR_ASSET_MISMATCH(IMA_ERROR, 2010),
        COMPANION_AD_LOADING_FAILED(IMA_ERROR, 2011),
        UNKNOWN_ERROR(IMA_ERROR, 2012),
        VAST_EMPTY_RESPONSE(IMA_ERROR, 2013),
        FAILED_TO_REQUEST_ADS(IMA_ERROR, 2014),
        VAST_ASSET_NOT_FOUND(IMA_ERROR, 2015),
        ADS_REQUEST_NETWORK_ERROR(IMA_ERROR, 2016),
        INVALID_ARGUMENTS(IMA_ERROR, 2017),
        PLAYLIST_NO_CONTENT_TRACKING(IMA_ERROR, 2018),
        QUIET_LOG_ERROR(IMA_ERROR, 2019),

        //Analytics errors:
        INVALID_INIT_OBJECT(ANALYTICS_PLUGIN_ERROR, 2100),

        //Offline errors:
        NO_NETWORK_CONNECTION(OFFLINE_ERROR, 2300),
        NO_SOURCE_FOUND(OFFLINE_ERROR, 2301),
        ASSET_REGISTRATION_FAILED(OFFLINE_ERROR, 2302),
        LOCAL_ASSET_NOT_FOUND(OFFLINE_ERROR, 2303),

        //Widevine classic DRM errors:
        DRM_RIGHTS_NOT_INSTALLED(DRM_ERROR, 2400),
        DRM_RIGHTS_RENEWAL_NOT_ALLOWED(DRM_ERROR, 2401),
        DRM_NOT_SUPPORTED(DRM_ERROR, 2402),
        DRM_OUT_OF_MEMORY(DRM_ERROR, 2403),
        DRM_NO_INTERNET_CONNECTION(DRM_ERROR, 2404),
        PROCESS_DRM_INFO_FAILED(DRM_ERROR, 2405),
        DRM_REMOVE_ALL_RIGHTS_FAILED(DRM_ERROR, 2406),
        ACQUIRE_DRM_INFO_FAILED(DRM_ERROR, 2407),

        NO_WIDEVINE_PSSH(DRM_ERROR, 2450),
        DRM_FAILED_TO_OPEN_SESSION(DRM_ERROR, 2451);


        public final int code;
        public final int group;

        PKErrorType(int group, int code) {
            this.group = group;
            this.code = code;
        }
    }
}
