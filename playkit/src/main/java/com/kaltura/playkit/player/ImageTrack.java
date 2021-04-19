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
public class ImageTrack extends BaseTrack {

    private String label;
    private long bitrate;
    private float width;
    private float height;
    private int cols;
    private int rows;
    private long duration;
    private String url;

    ImageTrack(String uniqueId,
               String label,
               long bitrate,
               float width,
               float height,
               int cols,
               int rows,
               long duration,
               String url
              ) {
        super(uniqueId, 0, false);
        this.label = label;
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
        this.cols = cols;
        this.rows = rows;
        this.duration = duration;
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public long getBitrate() {
        return bitrate;
    }
    
    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public long getDuration() {
        return duration;
    }

    public String getUrl() {
        return url;
    }
}
