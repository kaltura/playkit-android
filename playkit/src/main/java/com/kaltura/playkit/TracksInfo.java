package com.kaltura.playkit;

import java.util.List;

/**
 * Container for all available track info.
 * Created by anton.afanasiev on 17/11/2016.
 */

public class TracksInfo {

    private List<BaseTrackInfo> audioTrackInfo;
    private List<BaseTrackInfo> videoTrackInfo;
    private List<BaseTrackInfo> subtitleTrackInfo;

    public TracksInfo(List<BaseTrackInfo> videoTrackInfo, List<BaseTrackInfo> audioTrackInfo, List<BaseTrackInfo> subtitleTrackInfo) {
        this.audioTrackInfo = audioTrackInfo;
        this.videoTrackInfo = videoTrackInfo;
        this.subtitleTrackInfo = subtitleTrackInfo;
    }

    public List<BaseTrackInfo> getAudioTrackInfo() {
        return audioTrackInfo;
    }

    public List<BaseTrackInfo> getVideoTrackInfo() {
        return videoTrackInfo;
    }

    public List<BaseTrackInfo> getSubtitleTrackInfo() {
        return subtitleTrackInfo;
    }

}

