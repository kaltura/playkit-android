package com.kaltura.playkit;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Container for all available track info.
 * Created by anton.afanasiev on 17/11/2016.
 */

public class PKTracks {

    private List<BaseTrack> videoTracksInfo;
    private List<BaseTrack> audioTracksInfo;
    private List<BaseTrack> textTracksInfo;

    public PKTracks(List<BaseTrack> videoTracksInfo, List<BaseTrack> audioTracksInfo, List<BaseTrack> textTracksInfo) {
        this.audioTracksInfo = audioTracksInfo;
        this.videoTracksInfo = videoTracksInfo;
        this.textTracksInfo = textTracksInfo;
    }

    /**
     * Getter for videoTracksInfo list.
     * Before use, the list entry's should be casted to {@link VideoTrack} in order to receive the
     * full track info of that type.
     * Can be empty, if no tracks available.
     * @return - the list of all available Video tracks, that can be played on the device.
     */
    @NonNull
    public List<BaseTrack> getVideoTracksInfo() {
        return videoTracksInfo;
    }

    /**
     * Getter for audioTracksInfo list.
     * Before use, the list entry's should be casted to {@link AudioTrack} in order to receive the
     * full track info of that type.
     * Can be empty, if no tracks available.
     * @return - the list of all available Audio tracks, that can be played on the device.
     */
    @NonNull
    public List<BaseTrack> getAudioTracksInfo() {
        return audioTracksInfo;
    }

    /**
     * Getter for textTracksInfo list.
     * Before use, the list entry's should be casted to {@link TextTrack} in order to receive the
     * full track info of that type.
     * Can be empty, if no tracks available.
     * @return - the list of all available Text tracks, that can be played on the device.
     */
    @NonNull
    public List<BaseTrack> getTextTracksInfo() {
        return textTracksInfo;
    }

}

