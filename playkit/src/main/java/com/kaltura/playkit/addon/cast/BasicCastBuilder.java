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

package com.kaltura.playkit.addon.cast;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.TextTrackStyle;
import com.kaltura.playkit.PKLog;

import org.json.JSONObject;

/**
 * Created by itanbarpeled on 14/12/2016.
 */

public abstract class BasicCastBuilder<T extends BasicCastBuilder> {


    private static final PKLog log = PKLog.get("BasicCastBuilder");



    public enum StreamType {
        VOD,
        LIVE
    }

    private static final String MOCK_DATA = "MOCK_DATA";
    CastInfo castInfo;


    public BasicCastBuilder() {
        castInfo = new CastInfo();
    }


    public T setPartnerId(@NonNull String partnerId) {
        castInfo.setPartnerId(partnerId);
        return (T) this;
    }


    public T setUiConfId(@NonNull String uiConfId) {
        castInfo.setUiConfId(uiConfId);
        return (T) this;
    }


    public T setAdTagUrl(@NonNull String adTagUrl) {
        castInfo.setAdTagUrl(adTagUrl);
        return (T) this;
    }

    public T setMediaEntryId(@NonNull String mediaEntryId) {
        castInfo.setMediaEntryId(mediaEntryId);
        return (T) this;
    }

    public T setMetadata(@NonNull MediaMetadata mediaMetadata) {
        castInfo.setMetadata(mediaMetadata);
        return (T) this;
    }

    public T setTextTrackStyle(@NonNull TextTrackStyle textTrackStyle) {
        castInfo.setTextTrackStyle(textTrackStyle);
        return (T) this;
    }


    public T setMwEmbedUrl(@NonNull String mwEmbedUrl) {
        castInfo.setMwEmbedUrl(mwEmbedUrl);
        return (T) this;
    }


    public T setStreamType(@NonNull StreamType streamType) {
        castInfo.setStreamType(streamType);
        return (T) this;
    }


    public MediaInfo build() {
        return getMediaInfo(castInfo);
    }



    private MediaInfo getMediaInfo(CastInfo castInfo) {

        validate(castInfo);

        CastConfigHelper castConfigHelper = getCastHelper();

        JSONObject customData = castConfigHelper.getCustomData(castInfo);

        MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(MOCK_DATA)
                .setContentType(MOCK_DATA)
                .setCustomData(customData);

        setStreamType(mediaInfoBuilder, castInfo);
        setOptionalData(mediaInfoBuilder, castInfo);

        return mediaInfoBuilder.build();

    }



    /*
    This method sets data that isn't mandatory, and the developer may not provide
     */
    private void setOptionalData(MediaInfo.Builder mediaInfoBuilder, CastInfo castInfo) {

        MediaMetadata mediaMetadata = castInfo.getMetadata();
        if (mediaMetadata != null) {
            mediaInfoBuilder.setMetadata(mediaMetadata);
        }

        TextTrackStyle textTrackStyle = castInfo.getTextTrackStyle();
        if (textTrackStyle != null) {
            mediaInfoBuilder.setTextTrackStyle(textTrackStyle);
        }

    }


    private void setStreamType(MediaInfo.Builder mediaInfoBuilder, CastInfo castInfo) {

        StreamType streamType = castInfo.getStreamType();
        int castStreamType;

        switch (streamType) {

            case VOD:
                castStreamType = MediaInfo.STREAM_TYPE_BUFFERED;
                break;

            case LIVE:
                castStreamType = MediaInfo.STREAM_TYPE_LIVE;
                break;

            default:
                castStreamType = MediaInfo.STREAM_TYPE_BUFFERED;
                break;
        }

        mediaInfoBuilder.setStreamType(castStreamType);

    }



    protected void validate(CastInfo castInfo) throws IllegalArgumentException {

        if (TextUtils.isEmpty(castInfo.getUiConfId())) {
            log.e("you should set the uiConfId using the CastBuilder or via the google cast console");
        }

        if (TextUtils.isEmpty(castInfo.getMediaEntryId())) {
            throw new IllegalArgumentException();
        }

        // adTagUrl isn't mandatory, but if you set adTagUrl it must be valid
        String adTagUrl = castInfo.getAdTagUrl();
        if (adTagUrl != null && TextUtils.isEmpty(adTagUrl)) {
            throw new IllegalArgumentException();
        }


        if (castInfo.getStreamType() == null) {
            throw new IllegalArgumentException();
        }

    }


    protected abstract CastConfigHelper getCastHelper();


}
