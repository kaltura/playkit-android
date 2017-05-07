package com.kaltura.playkit;

/**
 * Data object that holds information about currently playing media.
 * Created by anton.afanasiev on 14/12/2016.
 */

public class PlaybackParamsInfo {

    private String mediaUrl;
    private long videoBitrate;
    private long audioBitrate;
    private long videoThroughput;


    public PlaybackParamsInfo(String mediaUrl, long videoBitrate, long audioBitrate, long videoThroughput) {
        this.mediaUrl = mediaUrl;
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
        this.videoThroughput = videoThroughput;
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

    /**
     *
     * @return - the current playing video throughput.
     */
    public long getVideoThroughput() {
        return videoThroughput;
    }
}
