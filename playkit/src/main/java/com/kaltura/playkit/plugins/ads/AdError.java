package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPublicAPI;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

@PKPublicAPI
public class AdError implements PKEvent {

    public Type errorType;
    public int  errorCode;
    public String message;

    public AdError(Type type, String message) {
        this.errorType = type;
        this.message = message;
    }

    public enum Type {
        INTERNAL_ERROR(2000),
        VAST_MALFORMED_RESPONSE(2001),
        UNKNOWN_AD_RESPONSE(2002),
        VAST_LOAD_TIMEOUT(2003),
        VAST_TOO_MANY_REDIRECTS(2004),
        VIDEO_PLAY_ERROR(2005),
        VAST_MEDIA_LOAD_TIMEOUT(2006),
        VAST_LINEAR_ASSET_MISMATCH(2007),
        OVERLAY_AD_PLAYING_FAILED(2008),
        OVERLAY_AD_LOADING_FAILED(2009),
        VAST_NONLINEAR_ASSET_MISMATCH(2010),
        COMPANION_AD_LOADING_FAILED(2011),
        UNKNOWN_ERROR(2012),
        VAST_EMPTY_RESPONSE(2013),
        FAILED_TO_REQUEST_ADS(2014),
        VAST_ASSET_NOT_FOUND(2015),
        ADS_REQUEST_NETWORK_ERROR(2016),
        INVALID_ARGUMENTS(2017),
        PLAYLIST_NO_CONTENT_TRACKING(2018);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    public Enum eventType() {
        return this.errorType;
    }
}
