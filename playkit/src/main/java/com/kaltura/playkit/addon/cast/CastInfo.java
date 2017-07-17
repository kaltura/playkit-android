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



import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.TextTrackStyle;
import com.kaltura.playkit.addon.cast.BasicCastBuilder.StreamType;

/**
 * Created by itanbarpeled on 13/12/2016.
 */

class CastInfo {

    private String adTagUrl;
    private String mediaEntryId;
    private String ks;
    private String partnerId;
    private String uiConfId;
    private String initObject;
    private String format;
    private MediaMetadata mediaMetadata;
    private TextTrackStyle textTrackStyle;
    private String mwEmbedUrl;
    private StreamType streamType;


    CastInfo() {
    }


    void setFormat(String format) {
        this.format = format;
    }

    void setInitObject(String initObject) {
        this.initObject = initObject;
    }

    void setKs(String ks) {
        this.ks = ks;
    }

    void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    void setUiConfId(String uiConfId) {
        this.uiConfId = uiConfId;
    }

    void setAdTagUrl(String adTagUrl) {
        this.adTagUrl = adTagUrl;
    }

    void setMediaEntryId(String mediaEntryId) {
        this.mediaEntryId = mediaEntryId;
    }

    void setMetadata(MediaMetadata mediaMetadata) {
        this.mediaMetadata = mediaMetadata;
    }

    void setTextTrackStyle(TextTrackStyle textTrackStyle) {
        this.textTrackStyle = textTrackStyle;
    }

    void setMwEmbedUrl(String mwEmbedUrl) {
        this.mwEmbedUrl = mwEmbedUrl;
    }

    void setStreamType(StreamType streamType) {
        this.streamType = streamType;
    }

    String getAdTagUrl() {
        return adTagUrl;
    }

    String getFormat() {
        return format;
    }

    String getInitObject() {
        return initObject;
    }

    String getKs() {
        return ks;
    }

    String getMediaEntryId() {
        return mediaEntryId;
    }

    MediaMetadata getMetadata() {
        return mediaMetadata;
    }

    String getPartnerId() {
        return partnerId;
    }

    TextTrackStyle getTextTrackStyle() {
        return textTrackStyle;
    }

    String getUiConfId() {
        return uiConfId;
    }

    String getMwEmbedUrl() {
        return mwEmbedUrl;
    }

    StreamType getStreamType() {
        return streamType;
    }

}
