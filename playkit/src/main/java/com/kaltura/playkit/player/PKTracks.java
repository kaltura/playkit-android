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

package com.kaltura.playkit.player;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Container for all available track info.
 * Created by anton.afanasiev on 17/11/2016.
 */

public class PKTracks {

    private int defaultVideoTrackIndex;
    private int defaultAudioTrackIndex;
    private int defaultTextTrackIndex;

    private List<VideoTrack> videoTracks;
    private List<AudioTrack> audioTracks;
    private List<TextTrack> textTracks;

    public PKTracks(List<VideoTrack> videoTracks, List<AudioTrack> audioTracks, List<TextTrack> textTracks,
        int defaultVideoTrackIndex, int defaultAudioTrackIndex, int defaultTextTrackIndex) {

        this.audioTracks = audioTracks;
        this.videoTracks = videoTracks;
        this.textTracks = textTracks;

        this.defaultVideoTrackIndex = defaultVideoTrackIndex;
        this.defaultAudioTrackIndex = defaultAudioTrackIndex;
        this.defaultTextTrackIndex = defaultTextTrackIndex;
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

    /**
     * Getter for default video track index.
     * The one that will be selected by player based on the media manifest.
     * If no default available in the manifest, the index will be 0.
     * @return - the index of the track that is set by default.
     */
    public int getDefaultVideoTrackIndex() {
        return defaultVideoTrackIndex;
    }

    /**
     * Getter for default audio track index.
     * The one that will be selected by player based on the media manifest.
     * If no default available in the manifest, the index will be 0.
     * @return - the index of the track that is set by default.
     */
    public int getDefaultAudioTrackIndex() {
        return defaultAudioTrackIndex;
    }

    /**
     * Getter for default text track index.
     * The one that will be selected by player based on the media manifest.
     * If no default available in the manifest, the index will be 0.
     * @return - the index of the track that is set by default.
     */
    public int getDefaultTextTrackIndex() {
        return defaultTextTrackIndex;
    }
}

