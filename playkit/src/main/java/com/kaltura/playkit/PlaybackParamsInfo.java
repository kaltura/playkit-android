package com.kaltura.playkit;

/**
 * Data object that holds information about currently playing media.
 * Created by anton.afanasiev on 14/12/2016.
 */

@PKPublicAPI
public class PlaybackParamsInfo {

    private long videoBitrate;
    private long audioBitrate;
    private String mediaUrl;

    public PlaybackParamsInfo(String mediaUrl, long videoBitrate, long audioBitrate) {
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
        this.mediaUrl = mediaUrl;
    }

    /**
     * @return - the current playing media url.
     */
    public String getMediaUrl() {
        return mediaUrl;
    }

    /**
     *
     * @return - the current playing video track bitrate.
     */
    public long getVideoBitrate() {
        return videoBitrate;
    }

    /**
     *
     * @return - the current playing audio track bitrate.
     */
    public long getAudioBitrate() {
        return audioBitrate;
    }
}
