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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.playkit.PKAudioCodec;

/**
 * Audio track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
public class AudioTrack extends BaseTrack implements Comparable<AudioTrack> {

    private long bitrate;
    private String label;
    private String language;
    private int channelCount;
    private PKAudioCodec codecType;
    private String codecName;

    AudioTrack(String uniqueId, String language, String label, long bitrate, int channelCount, int selectionFlag, boolean isAdaptive, PKAudioCodec codecType, String codecName) {
        super(uniqueId, selectionFlag, isAdaptive);
        this.label = label;
        this.bitrate = bitrate;
        this.language = language;
        this.channelCount = channelCount;
        this.codecType = codecType;
        this.codecName = codecName;
    }

    /**
     * Getter for the track language.
     * Can be null if the language is unknown.
     *
     * @return - the language of the track.
     */
    @Override
    @Nullable public String getLanguage() {
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
    @Nullable public String getLabel() {
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

    /**
     * Getter for the track codec type.
     *
     * @return - the audio codec type.
     */
    @Nullable public PKAudioCodec getCodecType() {
        return codecType;
    }

    /**
     * Getter for the track codec name.
     *
     * @return - the audio codec name
     */

    @Nullable public String getCodecName() {
        return codecName;
    }

    @Override
    public int compareTo(@NonNull AudioTrack track) {
        return Long.compare(this.getBitrate(), track.getBitrate());
    }
}
