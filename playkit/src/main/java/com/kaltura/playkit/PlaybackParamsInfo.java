package com.kaltura.playkit;

/**
 * Created by anton.afanasiev on 14/12/2016.
 */

public class PlaybackParamsInfo {

    private long videoBitrate;
    private long audioBitrate;
    private String mediaUrl;

    public PlaybackParamsInfo(String mediaUrl, long videoBitrate, long audioBitrate) {
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
        this.mediaUrl = mediaUrl;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public long getVideoBitrate() {
        return videoBitrate;
    }

    public long getAudioBitrate() {
        return audioBitrate;
    }
}
