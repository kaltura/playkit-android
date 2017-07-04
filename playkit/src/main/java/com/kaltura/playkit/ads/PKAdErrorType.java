/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.ads;


/**
 * Created by anton.afanasiev on 20/06/2017.
 */

public enum PKAdErrorType {

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
    PLAYLIST_NO_CONTENT_TRACKING(2018),
    QUIET_LOG_ERROR(2019), ;

    public final int errorCode;

    PKAdErrorType(int errorCode) {
        this.errorCode = errorCode;
    }

}
