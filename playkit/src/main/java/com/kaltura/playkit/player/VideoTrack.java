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

import com.kaltura.playkit.PKVideoCodec;


/**
 * Video track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
public class VideoTrack extends BaseTrack implements Comparable<VideoTrack> {

    private int width;
    private int height;
    private long bitrate;
    private PKVideoCodec codecType;
    private String codecName;

    VideoTrack(String uniqueId, long bitrate, int width, int height, int selectionFlag, boolean isAdaptive, PKVideoCodec codecType, String codecName) {
        super(uniqueId, selectionFlag, isAdaptive);
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
        this.codecType = codecType;
        this.codecName = codecName;
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
     * Getter for the track width.
     * Can be -1 if unknown or not applicable.
     *
     * @return - the width of the track.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Getter for the track height.
     * Can be -1 if unknown or not applicable.
     *
     * @return - the height of the track.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Getter for the track codecType.
     * Can be null if unknown or not applicable.
     *
     * @return - the codecType of the track.
     */
    @Nullable public PKVideoCodec getCodecType() {
        return codecType;
    }

    /**
     * Getter for the track codecName.
     * Can be null if unknown or not applicable.
     *
     * @return - the codec name of the track.
     */

    @Nullable public String getCodecName() {
        return codecName;
    }

    @Override
    public int compareTo(@NonNull VideoTrack track) {
        return Integer.compare((int)this.getBitrate(), (int)track.getBitrate());
    }
}
