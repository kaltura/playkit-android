package com.kaltura.playkit;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Container for all available track info.
 * Created by anton.afanasiev on 17/11/2016.
 */

public class PKTracks {

    private List<VideoTrack> videoTracks;
    private List<AudioTrack> audioTracks;
    private List<TextTrack> textTracks;

    public PKTracks(List<VideoTrack> videoTracks, List<AudioTrack> audioTracks, List<TextTrack> textTracks) {
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
    public List<VideoTrack> getVideoTracks() {
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
    public List<AudioTrack> getAudioTracks() {
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
    public List<TextTrack> getTextTracks() {
        return textTracks;
    }

}

