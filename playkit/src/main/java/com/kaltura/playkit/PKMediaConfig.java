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

package com.kaltura.playkit;

/**
 * Created by Noam Tamim @ Kaltura on 22/02/2017.
 */
public class PKMediaConfig {
    private long startPosition = 0;
    private PKMediaEntry mediaEntry;

    /**
     * Getter for start position. Default is 0.
     * Note, that start position is in seconds.
     *
     * @return - the start position
     */
    public long getStartPosition() {
        return startPosition;
    }

    /**
     * Setter for start position.
     * Note, that start position is in seconds.
     *
     * @param startPosition - the position from which the media should start.
     * @return - the config object.
     */
    public PKMediaConfig setStartPosition(long startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public PKMediaEntry getMediaEntry() {
        return mediaEntry;
    }

    public PKMediaConfig setMediaEntry(PKMediaEntry mediaEntry) {
        this.mediaEntry = mediaEntry;
        return this;
    }
}

