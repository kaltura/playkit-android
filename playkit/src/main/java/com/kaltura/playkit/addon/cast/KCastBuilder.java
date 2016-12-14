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

public abstract class KCastBuilder<T extends KCastBuilder> {


    private static final String MOCK_DATA = "MOCK_DATA";
    KCastInfo mKCastInfo;


    public KCastBuilder() {
        mKCastInfo = new KCastInfo();
    }


    public T setPartnerId(@NonNull String partnerId) {
        mKCastInfo.setPartnerId(partnerId);
        return (T) this;
    }


    public T setUiConfId(@NonNull String uiConfId) {
        mKCastInfo.setUiConfId(uiConfId);
        return (T) this;
    }


    public T setAdTagUrl(@NonNull String adTagUrl) {
        mKCastInfo.setAdTagUrl(adTagUrl);
        return (T) this;
    }

    public T setMediaEntryId(@NonNull String mediaEntryId) {
        mKCastInfo.setMediaEntryId(mediaEntryId);
        return (T) this;
    }

    public T setMetadata(@NonNull MediaMetadata mediaMetadata) {
        mKCastInfo.setMetadata(mediaMetadata);
        return (T) this;
    }

    public T setMediaTrackList(@NonNull List<MediaTrack> mediaTrackList) {
        mKCastInfo.setMediaTrackList(mediaTrackList);
        return (T) this;
    }

    public T setTextTrackStyle(@NonNull TextTrackStyle textTrackStyle) {
        mKCastInfo.setTextTrackStyle(textTrackStyle);
        return (T) this;
    }


    public T setMwEmbedUrl(@NonNull String mwEmbedUrl) {
        mKCastInfo.setMwEmbedUrl(mwEmbedUrl);
        return (T) this;
    }


    public MediaInfo build() {
        return getMediaInfo(mKCastInfo);
    }



    private MediaInfo getMediaInfo(KCastInfo kCastInfo) {

        validate(kCastInfo);

        CastConfigHelper castConfigHelper = getCastHelper();

        JSONObject customData = castConfigHelper.getCustomData(kCastInfo);

        MediaInfo.Builder mediaInfoBuilder = new MediaInfo.Builder(MOCK_DATA)
                .setStreamType(MediaInfo.STREAM_TYPE_NONE)
                .setContentType(MOCK_DATA)
                .setCustomData(customData);

        setOptionalData(mediaInfoBuilder, kCastInfo);

        return mediaInfoBuilder.build();

    }



    /*
    This method sets data that isn't mandatory, and the developer may not provide
     */
    private void setOptionalData(MediaInfo.Builder mediaInfoBuilder, KCastInfo kCastInfo) {

        MediaMetadata mediaMetadata = kCastInfo.getMetadata();
        if (mediaMetadata != null) {
            mediaInfoBuilder.setMetadata(mediaMetadata);
        }


        List<MediaTrack> mediaTrackList = kCastInfo.getMediaTrackList();
        if (mediaTrackList != null) {
            mediaInfoBuilder.setMediaTracks(mediaTrackList);
        }


        TextTrackStyle textTrackStyle = kCastInfo.getTextTrackStyle();
        if (textTrackStyle != null) {
            mediaInfoBuilder.setTextTrackStyle(textTrackStyle);
        }

    }




    protected void validate(KCastInfo kCastInfo) throws IllegalArgumentException{

        if (TextUtils.isEmpty(kCastInfo.getMwEmbedUrl())) {
            throw new IllegalArgumentException();
        }

        if (TextUtils.isEmpty(kCastInfo.getUiConfId())) {
            throw new IllegalArgumentException();
        }

        if (TextUtils.isEmpty(kCastInfo.getMediaEntryId())) {
            throw new IllegalArgumentException();
        }

        // adTagUrl isn't mandatory, but if you set adTagUrl it must be valid
        String adTagUrl = kCastInfo.getAdTagUrl();
        if (adTagUrl != null && TextUtils.isEmpty(adTagUrl)) {
            throw new IllegalArgumentException();
        }

    }


    protected abstract CastConfigHelper getCastHelper();


}
