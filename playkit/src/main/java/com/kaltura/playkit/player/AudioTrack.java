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

import android.support.annotation.Nullable;

/**
 * Audio track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
public class AudioTrack extends BaseTrack {

    private long bitrate;
    private String label;
    private String language;
    private int channelCount;

    AudioTrack(String uniqueId, String language, String label, long bitrate, int channelCount, int selectionFlag, boolean isAdaptive) {
        super(uniqueId, selectionFlag, isAdaptive);
        this.label = label;
        this.bitrate = bitrate;
        this.language = language;
        this.channelCount = channelCount;
    }

    /**
     * Getter for the track language.
     * Can be null if the language is unknown.
     *
     * @return - the language of the track.
     */
    @Override
    public @Nullable
    String getLanguage() {
        return language;
    }

    /**
     * Getter for the track bitrate.
     * Can be -1 if unknown or not applicable.
     *
     * @return - the bitrate of the track.
     */
    public long getBitrate() {
        return bitrate;
    }

    /**
     * Getter for the track label.
     * Can be null if the label is unknown.
     *
     * @return - the label of the track.
     */
    public @Nullable
    String getLabel() {
        return label;
    }

    /**
     * Getter for the track channels count.
     * Can be -1 if unknown or not applicable.
     *
     * @return - the number of channels of the track.
     */
    public int getChannelCount() {
        return channelCount;
    }
}