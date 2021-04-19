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
 * Image track data holder.
 *
 */
public class DashImageTrack extends ImageTrack {

    private long presentationTimeOffset;
    private long timeScale;
    private long startNumber;
    private long endNumber;

    DashImageTrack(String uniqueId,
                   String label,
                   long bitrate,
                   float width,
                   float height,
                   int cols,
                   int rows,
                   long duration,
                   String url,
                   long presentationTimeOffset,
                   long timeScale,
                   long startNumber,
                   long endtNumber
              ) {
        super(uniqueId, label, bitrate, width, height, cols, rows, duration, url);

        this.presentationTimeOffset = presentationTimeOffset;
        this.timeScale = timeScale;
        this.startNumber = startNumber;
        this.endNumber = endtNumber;
    }

    public long getPresentationTimeOffset() {
        return presentationTimeOffset;
    }

    public long getTimeScale() {
        return timeScale;
    }

    public long getStartNumber() {
        return startNumber;
    }

    public long getEndNumber() {
        return endNumber;
    }

}
