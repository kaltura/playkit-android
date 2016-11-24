package com.kaltura.playkit;

import java.util.List;

/**
 * Created by anton.afanasiev on 17/11/2016.
 */

public class TrackData {

    private List<AudioTrackData> audioTrackData;
    private List<VideoTrackData> videoTrackData;
    private List<SubtitleTrackData> subtitleTrackData;

    public TrackData(List<VideoTrackData> videoTrackData, List<AudioTrackData> audioTrackData, List<SubtitleTrackData> subtitleTrackData) {
        this.audioTrackData = audioTrackData;
        this.videoTrackData = videoTrackData;
        this.subtitleTrackData = subtitleTrackData;
    }

    public List<AudioTrackData> getAudioTrackData() {
        return audioTrackData;
    }

    public List<VideoTrackData> getVideoTrackData() {
        return videoTrackData;
    }

    public List<SubtitleTrackData> getSubtitleTrackData() {
        return subtitleTrackData;
    }
}
