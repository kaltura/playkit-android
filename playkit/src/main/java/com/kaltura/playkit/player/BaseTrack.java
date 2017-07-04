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

/**
 * Base track info object that is common to all the trackInfo types.
 * Holds the uniqueId which is unique for all of the TrackInfo objects.
 * Also have boolean description, if this info object is Adaptive.
 * TrackInfo objects that have isAdaptive field set to true, represents an adaptive(also known as Auto)
 * playback option. When this type of track is selected,
 * the system will adjust the bitrate of the playing track to the bandwidth
 * capabilities of  the device.
 * Created by anton.afanasiev on 27/11/2016.
 */

public abstract class BaseTrack {

    private String uniqueId;
    private int selectionFlag;
    private boolean isAdaptive;

     BaseTrack(String uniqueId, int selectionFlag, boolean isAdaptive) {
        this.uniqueId = uniqueId;
        this.selectionFlag = selectionFlag;
        this.isAdaptive = isAdaptive;
    }

    /**
     * Getter for uniqueId
     * @return - the uniqueId of the current track.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Getter for isAdaptive field.
     * If isAdaptive return true, that means that current track should be used as
     * "Auto" playback option. So the system will adjust the
     * bitrate of the playing track to the bandwidth  capabilities of the device.
     * @return - true if current track is adaptive, otherwise - false.
     */
    public boolean isAdaptive() {
        return isAdaptive;
    }

    int getSelectionFlag() {
        return selectionFlag;
    }
}
