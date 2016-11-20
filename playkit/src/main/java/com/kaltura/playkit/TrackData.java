package com.kaltura.playkit;

/**
 * Created by anton.afanasiev on 17/11/2016.
 */

public class TrackData {

    private AudioTrackData audioTrackData;
    private VideoTrackData videoTrackData;
    private SubtitleTrackData subtitleTrackData;

    public TrackData(AudioTrackData audioTrackData, VideoTrackData videoTrackData, SubtitleTrackData subtitleTrackData) {
        this.audioTrackData = audioTrackData;
        this.videoTrackData = videoTrackData;
        this.subtitleTrackData = subtitleTrackData;
    }

    public AudioTrackData getAudioTrackData() {
        return audioTrackData;
    }

    public VideoTrackData getVideoTrackData() {
        return videoTrackData;
    }

    public SubtitleTrackData getSubtitleTrackData() {
        return subtitleTrackData;
    }
}
