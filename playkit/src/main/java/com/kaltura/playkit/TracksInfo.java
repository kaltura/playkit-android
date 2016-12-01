package com.kaltura.playkit;

import java.util.List;

/**
 * Container for all available track info.
 * Created by anton.afanasiev on 17/11/2016.
 */

public class TracksInfo {

    private List<BaseTrackInfo> audioTrackInfo;
    private List<BaseTrackInfo> videoTrackInfo;
    private List<BaseTrackInfo> textTrackInfo;

    public TracksInfo(List<BaseTrackInfo> videoTrackInfo, List<BaseTrackInfo> audioTrackInfo, List<BaseTrackInfo> textTrackInfo) {
        this.audioTrackInfo = audioTrackInfo;
        this.videoTrackInfo = videoTrackInfo;
        this.textTrackInfo = textTrackInfo;
    }

    public List<BaseTrackInfo> getAudioTrackInfo() {
        return audioTrackInfo;
    }

    public List<BaseTrackInfo> getVideoTrackInfo() {
        return videoTrackInfo;
    }

    public List<BaseTrackInfo> getTextTrackInfo() {
        return textTrackInfo;
    }

}

