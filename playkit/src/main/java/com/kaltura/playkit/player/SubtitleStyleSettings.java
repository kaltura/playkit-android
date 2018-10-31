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

public class SubtitleStyleSettings {

    // Default values are in builder
    private int subtitleTextColor;
    private int subtitleBackgroundColor;
    private float subtitleTextSizeFraction;

    private SubtitleStyleSettings(SubtitleStyleBuilder subtitleStyleBuilder) {
        this.subtitleTextColor = subtitleStyleBuilder.subtitleTextColor;
        this.subtitleBackgroundColor = subtitleStyleBuilder.subtitleBackgroundColor;
        this.subtitleTextSizeFraction = subtitleStyleBuilder.subtitleTextSizeFraction;
    }

    public int getSubtitleTextColor() {
        return subtitleTextColor;
    }

    public int getSubtitleBackgroundColor() {
        return subtitleBackgroundColor;
    }

    public float getSubtitleTextSizeFraction() {
        return subtitleTextSizeFraction;
    }

    public static class SubtitleStyleBuilder {

        // Subtitle Text Color, Default is Black
        private int subtitleTextColor = Color.BLACK;
        // Subtitle Background Color, Default is Transparent
        private int subtitleBackgroundColor = Color.TRANSPARENT;

        // Recommended fraction values is  1f < subtitleTextSizeFraction < 2.5f with 0.25f Multiplier
        // Subtitle TextSize fraction, Default is 0.0533f ; {@link com.google.android.exoplayer2.ui.SubtitleView}
        private float subtitleTextSizeFraction = 0.0533f;

        public SubtitleStyleBuilder setSubtitleTextColor(int subtitleTextColor) {
            this.subtitleTextColor = subtitleTextColor;
            return this;
        }

        public SubtitleStyleBuilder setSubtitleBackgroundColor(int subtitleBackgroundColor) {
            this.subtitleBackgroundColor = subtitleBackgroundColor;
            return this;
        }

        public SubtitleStyleBuilder setSubtitleTextSizeFraction(float subtitleTextSizeFraction) {
            this.subtitleTextSizeFraction = subtitleTextSizeFraction;
            return this;
        }

        public SubtitleStyleSettings build() {
            return new SubtitleStyleSettings(this);
        }

    }

}
