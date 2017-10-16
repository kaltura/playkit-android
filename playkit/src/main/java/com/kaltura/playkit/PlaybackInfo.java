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

package com.kaltura.playkit;

/**
 * Data object that holds information about currently playing media.
 * Created by anton.afanasiev on 14/12/2016.
 */

public class PlaybackInfo {

    private long videoBitrate;
    private long audioBitrate;
    private long videoThroughput;
    private long videoWidth;
    private long videoHeight;


    public PlaybackInfo(long videoBitrate, long audioBitrate, long videoThroughput, long videoWidth, long videoHeight) {
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
        this.videoThroughput = videoThroughput;
        this.videoWidth   = videoWidth;
        this.videoHeight  = videoHeight;
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

    /**
     *
     * @return - the current playing video width.
     */
    public long getVideoWidth() {
        return videoWidth;
    }

    /**
     *
     * @return - the current playing video height.
     */
    public long getVideoHeight() {
        return videoHeight;
    }

    @Override
    public String toString() {
        String sb = "videoBitrate =" + videoBitrate + System.getProperty("line.separator") +
                "audioBitrate =" + audioBitrate + System.getProperty("line.separator") +
                "videoThroughput =" + videoThroughput + System.getProperty("line.separator") +
                "videoWidth =" + videoWidth + System.getProperty("line.separator") +
                "videoHeight =" + videoHeight + System.getProperty("line.separator");
        return sb;
    }
}
