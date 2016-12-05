package com.kaltura.playkit;

import java.util.List;

/**
 * Container for all available track info.
 * Created by anton.afanasiev on 17/11/2016.
 */

public class TracksInfo {

    private List<BaseTrackInfo> videoTracksInfo;
    private List<BaseTrackInfo> audioTracksInfo;
    private List<BaseTrackInfo> textTracksInfo;

    public TracksInfo(List<BaseTrackInfo> videoTracksInfo, List<BaseTrackInfo> audioTracksInfo, List<BaseTrackInfo> textTracksInfo) {
        this.audioTracksInfo = audioTracksInfo;
        this.videoTracksInfo = videoTracksInfo;
        this.textTracksInfo = textTracksInfo;
    }

    /**
     * Getter for videoTracksInfo list.
     * Before use, the list entry's should be casted to {@link VideoTrackInfo} in order to receive the
     * full track info of that type.
     * @return - the list of all available Video tracks, that can be played on the device.
     */
    public List<BaseTrackInfo> getVideoTracksInfo() {
        return videoTracksInfo;
    }

    /**
     * Getter for audioTracksInfo list.
     * Before use, the list entry's should be casted to {@link AudioTrackInfo} in order to receive the
     * full track info of that type.
     * @return - the list of all available Audio tracks, that can be played on the device.
     */
    public List<BaseTrackInfo> getAudioTracksInfo() {
        return audioTracksInfo;
    }

    /**
     * Getter for textTracksInfo list.
     * Before use, the list entry's should be casted to {@link TextTrackInfo} in order to receive the
     * full track info of that type.
     * @return - the list of all available Text tracks, that can be played on the device.
     */
    public List<BaseTrackInfo> getTextTracksInfo() {
        return textTracksInfo;
    }

}

