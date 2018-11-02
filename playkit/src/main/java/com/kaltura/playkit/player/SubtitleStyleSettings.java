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

import com.google.android.exoplayer2.text.CaptionStyleCompat;

public class SubtitleStyleSettings {

    public enum SubtitleStyleEdgeType {
        EDGE_TYPE_NONE, EDGE_TYPE_OUTLINE, EDGE_TYPE_DROP_SHADOW, EDGE_TYPE_RAISED, EDGE_TYPE_DEPRESSED;
    }

    public enum SubtitleTextSizeFraction {
        SUBTITLE_FRACTION_50, SUBTITLE_FRACTION_75, SUBTITLE_FRACTION_100, SUBTITLE_FRACTION_125, SUBTITLE_FRACTION_150, SUBTITLE_FRACTION_200
    }

    private static final float fraction50  = 0.50f;
    private static final float fraction75  = 0.75f;
    private static final float fraction100 = 1.0f;
    private static final float fraction125 = 1.25f;
    private static final float fraction150 = 1.50f;
    private static final float fraction200 = 2.0f;

    // Default values are in builder
    private int subtitleTextColor;
    private int subtitleBackgroundColor;
    private float subtitleTextSizeFraction;
    private int subtitleWindowColor;
    private int subtitleEdgeType;
    private int subtitleEdgeColor;

    private SubtitleStyleSettings(SubtitleStyleBuilder subtitleStyleBuilder) {
        this.subtitleTextColor = subtitleStyleBuilder.subtitleTextColor;
        this.subtitleBackgroundColor = subtitleStyleBuilder.subtitleBackgroundColor;
        this.subtitleTextSizeFraction = subtitleStyleBuilder.subtitleTextSizeFraction;
        this.subtitleWindowColor = subtitleStyleBuilder.subtitleWindowColor;
        this.subtitleEdgeType = subtitleStyleBuilder.subtitleEdgeType;
        this.subtitleEdgeColor = subtitleStyleBuilder.subtitleEdgeColor;
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

    public int getSubtitleWindowColor() {
        return subtitleWindowColor;
    }

    public int getSubtitleEdgeType() {
        return subtitleEdgeType;
    }

    public int getSubtitleEdgeColor() {
        return subtitleEdgeColor;
    }

    public static class SubtitleStyleBuilder {

        private int subtitleTextColor = Color.WHITE;
        private int subtitleBackgroundColor = Color.BLACK;
        // Recommended fraction values is  1f < subtitleTextSizeFraction < 2.5f with 0.25f Multiplier
        // Subtitle TextSize fraction, Default is 1.0f ; {@link com.google.android.exoplayer2.ui.SubtitleView}
        private float subtitleTextSizeFraction = fraction100;
        private int subtitleWindowColor = Color.TRANSPARENT;
        private int subtitleEdgeType = CaptionStyleCompat.EDGE_TYPE_NONE;
        private int subtitleEdgeColor = Color.WHITE;

        public SubtitleStyleBuilder setSubtitleTextColor(int subtitleTextColor) {
            this.subtitleTextColor = subtitleTextColor;
            return this;
        }

        public SubtitleStyleBuilder setSubtitleBackgroundColor(int subtitleBackgroundColor) {
            this.subtitleBackgroundColor = subtitleBackgroundColor;
            return this;
        }

        public SubtitleStyleBuilder setSubtitleEdgeColor(int subtitleEdgeColor) {
            this.subtitleEdgeColor = subtitleEdgeColor;
            return this;
        }

        public SubtitleStyleBuilder setSubtitleWindowColor(int subtitleWindowColor) {
            this.subtitleWindowColor = subtitleWindowColor;
            return this;
        }

        public SubtitleStyleBuilder setSubtitleEdgeType(SubtitleStyleEdgeType subtitleEdgeType) {
            switch (subtitleEdgeType) {
                case EDGE_TYPE_NONE:
                    this.subtitleEdgeType = CaptionStyleCompat.EDGE_TYPE_NONE;
                    break;
                case EDGE_TYPE_OUTLINE:
                    this.subtitleEdgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE;
                    break;
                case EDGE_TYPE_DROP_SHADOW:
                    this.subtitleEdgeType = CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW;
                    break;
                case EDGE_TYPE_RAISED:
                    this.subtitleEdgeType = CaptionStyleCompat.EDGE_TYPE_RAISED;
                    break;
                case EDGE_TYPE_DEPRESSED:
                    this.subtitleEdgeType = CaptionStyleCompat.EDGE_TYPE_DEPRESSED;
                    break;
                default:
                    this.subtitleEdgeType = CaptionStyleCompat.EDGE_TYPE_NONE;
                    break;
            }
            return this;
        }

        public SubtitleStyleBuilder setSubtitleTextSizeFraction(SubtitleTextSizeFraction subtitleTextSizeFraction) {
            switch (subtitleTextSizeFraction) {
                case SUBTITLE_FRACTION_50:
                    this.subtitleTextSizeFraction = fraction50;
                    break;
                case SUBTITLE_FRACTION_75:
                    this.subtitleTextSizeFraction = fraction75;
                    break;
                case SUBTITLE_FRACTION_100:
                    this.subtitleTextSizeFraction = fraction100;
                    break;
                case SUBTITLE_FRACTION_125:
                    this.subtitleTextSizeFraction = fraction125;
                    break;
                case SUBTITLE_FRACTION_150:
                    this.subtitleTextSizeFraction = fraction150;
                    break;
                case SUBTITLE_FRACTION_200:
                    this.subtitleTextSizeFraction = fraction200;
                    break;
                default:
                    this.subtitleTextSizeFraction = fraction100;
                    break;
            }
            return this;
        }

        public SubtitleStyleSettings build() {
            return new SubtitleStyleSettings(this);
        }

    }

}
