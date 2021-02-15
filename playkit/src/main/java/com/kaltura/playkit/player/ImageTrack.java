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

import androidx.annotation.Nullable;

import com.kaltura.playkit.PKAudioCodec;

/**
 * Image track data holder.
 *
 */
public class ImageTrack extends BaseTrack {


    private String label;
    private long bitrate;
    private String structure;
    private int tilesHorizontal;
    private int tilesVertical;
    private int width;
    private int height;
    private long segmentDuration;
    private long presentationTimeOffset;
    private long timeScale;
    private long startNumber;
    private long endNumber;
    private String imageTemplateUrl;

    ImageTrack(String uniqueId,
               String label,
               long bitrate,
               int width,
               int height,
               int tilesHorizontal,
               int tilesVertical,
               long segmentDuration,
               long startNumber,
               long endtNumber,
               long presentationTimeOffset,
               long timeScale,
               String imageTemplateUrl
              ) {
        super(uniqueId, 0, false);
        this.label = label;
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
        this.tilesHorizontal = tilesHorizontal;
        this.tilesVertical = tilesVertical;
        this.segmentDuration = segmentDuration;
        this.startNumber = startNumber;
        this.endNumber = endtNumber;
        this.presentationTimeOffset = presentationTimeOffset;
        this.timeScale = timeScale;
        this.imageTemplateUrl = imageTemplateUrl;
    }

    public String getLabel() {
        return label;
    }

    public long getBitrate() {
        return bitrate;
    }

    public String getStructure() {
        return structure;
    }

    public int getTilesHorizontal() {
        return tilesHorizontal;
    }

    public int getTilesVertical() {
        return tilesVertical;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getSegmentDuration() {
        return segmentDuration;
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

    public String getImageTemplateUrl() {
        return imageTemplateUrl;
    }
}
