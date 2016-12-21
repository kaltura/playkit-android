package com.kaltura.playkit.addon.cast;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.cast.TextTrackStyle;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by itanbarpeled on 14/12/2016.
 */

public abstract class BasicCastBuilder<T extends BasicCastBuilder> {


    private static final String MOCK_DATA = "MOCK_DATA";
    CastInfo mCastInfo;


    public BasicCastBuilder() {
        mCastInfo = new CastInfo();
    }


    public T setPartnerId(@NonNull String partnerId) {
        mCastInfo.setPartnerId(partnerId);
        return (T) this;
    }


    public T setUiConfId(@NonNull String uiConfId) {
        mCastInfo.setUiConfId(uiConfId);
        return (T) this;
    }


    public T setAdTagUrl(@NonNull String adTagUrl) {
        mCastInfo.setAdTagUrl(adTagUrl);
        return (T) this;
    }

    public T setMediaEntryId(@NonNull String mediaEntryId) {
        mCastInfo.setMediaEntryId(mediaEntryId);
        return (T) this;
    }

    public T setMetadata(@NonNull MediaMetadata mediaMetadata) {
        mCastInfo.setMetadata(mediaMetadata);
        return (T) this;
    }

    public T setMediaTrackList(@NonNull List<MediaTrack> mediaTrackList) {
        mCastInfo.setMediaTrackList(mediaTrackList);
        return (T) this;
    }

    public T setTextTrackStyle(@NonNull TextTrackStyle textTrackStyle) {
        mCastInfo.setTextTrackStyle(textTrackStyle);
        return (T) this;
    }


    public T setMwEmbedUrl(@NonNull String mwEmbedUrl) {
        mCastInfo.setMwEmbedUrl(mwEmbedUrl);
        return (T) this;
    }


    public MediaInfo build() {
        return getMediaInfo(mCastInfo);
    }



    private MediaInfo getMediaInfo(CastInfo castInfo) {

        validate(castInfo);

        CastConfigHelper castConfigHelper = getCastHelper();

        JSONObject customData = castConfigHelper.getCustomData(castInfo);

        MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(MOCK_DATA)
                .setStreamType(MediaInfo.STREAM_TYPE_NONE)
                .setContentType(MOCK_DATA)
                .setCustomData(customData);

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


        List<MediaTrack> mediaTrackList = castInfo.getMediaTrackList();
        if (mediaTrackList != null) {
            mediaInfoBuilder.setMediaTracks(mediaTrackList);
        }


        TextTrackStyle textTrackStyle = castInfo.getTextTrackStyle();
        if (textTrackStyle != null) {
            mediaInfoBuilder.setTextTrackStyle(textTrackStyle);
        }

    }




    protected void validate(CastInfo castInfo) throws IllegalArgumentException {

        if (TextUtils.isEmpty(castInfo.getUiConfId())) {
            throw new IllegalArgumentException();
        }

        if (TextUtils.isEmpty(castInfo.getMediaEntryId())) {
            throw new IllegalArgumentException();
        }

        // adTagUrl isn't mandatory, but if you set adTagUrl it must be valid
        String adTagUrl = castInfo.getAdTagUrl();
        if (adTagUrl != null && TextUtils.isEmpty(adTagUrl)) {
            throw new IllegalArgumentException();
        }

    }


    protected abstract CastConfigHelper getCastHelper();


}
