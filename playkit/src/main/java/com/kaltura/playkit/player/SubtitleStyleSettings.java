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
import android.graphics.Typeface;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.kaltura.android.exoplayer2.ui.CaptionStyleCompat;

public class SubtitleStyleSettings {

    public enum SubtitleStyleEdgeType {
        EDGE_TYPE_NONE, EDGE_TYPE_OUTLINE, EDGE_TYPE_DROP_SHADOW, EDGE_TYPE_RAISED, EDGE_TYPE_DEPRESSED
    }

    public enum SubtitleTextSizeFraction {
        SUBTITLE_FRACTION_50, SUBTITLE_FRACTION_75, SUBTITLE_FRACTION_100, SUBTITLE_FRACTION_125, SUBTITLE_FRACTION_150, SUBTITLE_FRACTION_200
    }

    public enum SubtitleStyleTypeface {
        DEFAULT, DEFAULT_BOLD, MONOSPACE, SERIF, SANS_SERIF
    }

    public enum SubtitleTypefaceStyle {
         NORMAL, BOLD, ITALIC, BOLD_ITALIC
    }

    private static final float fraction50 = 0.50f;
    private static final float fraction75 = 0.75f;
    private static final float fraction100 = 1.0f;
    private static final float fraction125 = 1.25f;
    private static final float fraction150 = 1.50f;
    private static final float fraction200 = 2.0f;

    private int subtitleTextColor = Color.WHITE;
    private int subtitleBackgroundColor = Color.BLACK;
    // Recommended fraction values is  1f < subtitleTextSizeFraction < 2.5f with 0.25f Multiplier
    // Subtitle TextSize fraction, Default is 1.0f ; {@link com.kaltura.android.exoplayer2.ui.SubtitleView}
    private float subtitleTextSizeFraction = fraction100;
    private int subtitleWindowColor = Color.TRANSPARENT;
    private int subtitleEdgeType = CaptionStyleCompat.EDGE_TYPE_NONE;
    private int subtitleEdgeColor = Color.WHITE;
    private Typeface subtitleTypeface = Typeface.DEFAULT;
    private String subtitleStyleName;
    private PKSubtitlePosition subtitlePosition;

    public SubtitleStyleSettings(String subtitleStyleName) {
        if (!TextUtils.isEmpty(subtitleStyleName)) {
            this.subtitleStyleName = subtitleStyleName;
        } else {
            this.subtitleStyleName = "Unknown"; // Application does not provide any subtitle name or it is set to null
        }
    }

    public int getTextColor() {
        return subtitleTextColor;
    }

    public int getBackgroundColor() {
        return subtitleBackgroundColor;
    }

    public float getTextSizeFraction() {
        return subtitleTextSizeFraction;
    }

    public int getWindowColor() {
        return subtitleWindowColor;
    }

    public int getEdgeType() {
        return subtitleEdgeType;
    }

    public int getEdgeColor() {
        return subtitleEdgeColor;
    }

    public Typeface getTypeface() {
        return subtitleTypeface;
    }

    public String getStyleName() {
        return subtitleStyleName;
    }

    public PKSubtitlePosition getSubtitlePosition() {
        return subtitlePosition;
    }

    public SubtitleStyleSettings setTextColor(int subtitleTextColor) {
        this.subtitleTextColor = subtitleTextColor;
        return this;
    }

    public SubtitleStyleSettings setBackgroundColor(int subtitleBackgroundColor) {
        this.subtitleBackgroundColor = subtitleBackgroundColor;
        return this;
    }

    public SubtitleStyleSettings setEdgeColor(int subtitleEdgeColor) {
        this.subtitleEdgeColor = subtitleEdgeColor;
        return this;
    }

    public SubtitleStyleSettings setWindowColor(int subtitleWindowColor) {
        this.subtitleWindowColor = subtitleWindowColor;
        return this;
    }

    public SubtitleStyleSettings setEdgeType(@NonNull SubtitleStyleEdgeType subtitleEdgeType) {
        switch (subtitleEdgeType) {
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

    public SubtitleStyleSettings setTextSizeFraction(@NonNull SubtitleTextSizeFraction subtitleTextSizeFraction) {
        switch (subtitleTextSizeFraction) {
            case SUBTITLE_FRACTION_50:
                this.subtitleTextSizeFraction = fraction50;
                break;
            case SUBTITLE_FRACTION_75:
                this.subtitleTextSizeFraction = fraction75;
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

    public SubtitleStyleSettings setTypeface(@NonNull SubtitleStyleTypeface subtitleStyleTypeface) {
        switch (subtitleStyleTypeface) {
            case DEFAULT_BOLD:
                subtitleTypeface = Typeface.DEFAULT_BOLD;
                break;
            case MONOSPACE:
                subtitleTypeface = Typeface.MONOSPACE;
                break;
            case SERIF:
                subtitleTypeface = Typeface.SERIF;
                break;
            case SANS_SERIF:
                subtitleTypeface = Typeface.SANS_SERIF;
                break;
            case DEFAULT:
            default:
                subtitleTypeface = Typeface.DEFAULT;
                break;
        }
        return this;
    }

    public SubtitleStyleSettings setSystemTypeface(String fontFamilyName, SubtitleTypefaceStyle style) {
        if (fontFamilyName == null || style == null) {
            subtitleTypeface = Typeface.DEFAULT;
        } else {
            subtitleTypeface = Typeface.create(fontFamilyName, style.ordinal());
        }
        return this;
    }

    public SubtitleStyleSettings setAssetTypeface(Typeface asstTypeface) {
        if (asstTypeface == null) {
            subtitleTypeface = Typeface.DEFAULT;
        } else {
            subtitleTypeface = asstTypeface;
        }
        return this;
    }

    public SubtitleStyleSettings setSubtitlePosition(PKSubtitlePosition subtitlePosition) {
        this.subtitlePosition = subtitlePosition;
        return this;
    }

    public CaptionStyleCompat toCaptionStyle() {
        return new CaptionStyleCompat(getTextColor(),
                getBackgroundColor(),
                getWindowColor(),
                getEdgeType(),
                getEdgeColor(),
                getTypeface());
    }
}
