package com.kaltura.playkit.addon.cast;



import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.cast.TextTrackStyle;
import com.kaltura.playkit.addon.cast.BasicCastBuilder.StreamType;
import java.util.List;

/**
 * Created by itanbarpeled on 13/12/2016.
 */

class CastInfo {

    private String mAdTagUrl;
    private String mMediaEntryId;
    private String mKs;
    private String mPartnerId;
    private String mUiConfId;
    private String mInitObject;
    private String mFormat;
    private MediaMetadata mMediaMetadata;
    private TextTrackStyle mTextTrackStyle;
    private String mMwEmbedUrl;
    private StreamType mStreamType;


    CastInfo() {
    }


    void setFormat(String format) {
        this.mFormat = format;
    }

    void setInitObject(String initObject) {
        this.mInitObject = initObject;
    }

    void setKs(String ks) {
        this.mKs = ks;
    }

    void setPartnerId(String partnerId) {
        this.mPartnerId = partnerId;
    }

    void setUiConfId(String uiConfId) {
        this.mUiConfId = uiConfId;
    }

    void setAdTagUrl(String adTagUrl) {
        this.mAdTagUrl = adTagUrl;
    }

    void setMediaEntryId(String mediaEntryId) {
        this.mMediaEntryId = mediaEntryId;
    }

    void setMetadata(MediaMetadata mediaMetadata) {
        mMediaMetadata = mediaMetadata;
    }

    void setTextTrackStyle(TextTrackStyle textTrackStyle) {
        mTextTrackStyle = textTrackStyle;
    }

    void setMwEmbedUrl(String mwEmbedUrl) {
        mMwEmbedUrl = mwEmbedUrl;
    }

    void setStreamType(StreamType streamType) {
        mStreamType = streamType;
    }

    String getAdTagUrl() {
        return mAdTagUrl;
    }

    String getFormat() {
        return mFormat;
    }

    String getInitObject() {
        return mInitObject;
    }

    String getKs() {
        return mKs;
    }

    String getMediaEntryId() {
        return mMediaEntryId;
    }

    MediaMetadata getMetadata() {
        return mMediaMetadata;
    }

    String getPartnerId() {
        return mPartnerId;
    }

    TextTrackStyle getTextTrackStyle() {
        return mTextTrackStyle;
    }

    String getUiConfId() {
        return mUiConfId;
    }

    String getMwEmbedUrl() {
        return mMwEmbedUrl;
    }

    StreamType getStreamType() {
        return mStreamType;
    }

}
