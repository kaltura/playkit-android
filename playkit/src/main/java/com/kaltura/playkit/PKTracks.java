package com.kaltura.playkit;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Container for all available track info.
 * Created by anton.afanasiev on 17/11/2016.
 */

public class PKTracks {

    private List<BaseTrack> videoTracks;
    private List<BaseTrack> audioTracks;
    private List<BaseTrack> textTracks;

    public PKTracks(List<BaseTrack> videoTracks, List<BaseTrack> audioTracks, List<BaseTrack> textTracks) {
        this.audioTracks = audioTracks;
        this.videoTracks = videoTracks;
        this.textTracks = textTracks;
    }

    /**
     * Getter for videoTracks list.
     * Before use, the list entry's should be casted to {@link VideoTrack} in order to receive the
     * full track info of that type.
     * Can be empty, if no tracks available.
     * @return - the list of all available Video tracks, that can be played on the device.
     */
    @NonNull
    public List<BaseTrack> getVideoTracks() {
        return videoTracks;
    }

    /**
     * Getter for audioTracks list.
     * Before use, the list entry's should be casted to {@link AudioTrack} in order to receive the
     * full track info of that type.
     * Can be empty, if no tracks available.
     * @return - the list of all available Audio tracks, that can be played on the device.
     */
    @NonNull
    public List<BaseTrack> getAudioTracks() {
        return audioTracks;
    }

    /**
     * Getter for textTracks list.
     * Before use, the list entry's should be casted to {@link TextTrack} in order to receive the
     * full track info of that type.
     * Can be empty, if no tracks available.
     * @return - the list of all available Text tracks, that can be played on the device.
     */
    @NonNull
    public List<BaseTrack> getTextTracks() {
        return textTracks;
    }

}

