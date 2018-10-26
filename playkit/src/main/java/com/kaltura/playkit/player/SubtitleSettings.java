/*
 * ============================================================================
 * Copyright (C) 2018 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.player;

import android.graphics.Color;

public class SubtitleSettings {

    private int subtitleTextColor = Color.BLACK; // Subtitle Text Color, Default is Black
    private int subtitleBackgroundColor = Color.TRANSPARENT; // Subtitle Background Color, Default is Transparent

    /**
     * Recommended fraction values is  1f < subtitleTextSizeFraction < 2.5f with 0.25f Multiplier
     * Subtitle TextSize fraction, Default is 0.0533f ; {@link com.google.android.exoplayer2.ui.SubtitleView}
     */
    private float subtitleTextSizeFraction = 0.0533f;


    public int getSubtitleTextColor() {
        return subtitleTextColor;
    }

    public void setSubtitleTextColor(int subtitleTextColor) {
        this.subtitleTextColor = subtitleTextColor;
    }

    public int getSubtitleBackgroundColor() {
        return subtitleBackgroundColor;
    }

    public void setSubtitleBackgroundColor(int subtitleBackgroundColor) {
        this.subtitleBackgroundColor = subtitleBackgroundColor;
    }

    public float getSubtitleTextSizeFraction() {
        return subtitleTextSizeFraction;
    }

    public void setSubtitleTextSizeFraction(float subtitleTextSizeFraction) {
        this.subtitleTextSizeFraction = subtitleTextSizeFraction;
    }

}
