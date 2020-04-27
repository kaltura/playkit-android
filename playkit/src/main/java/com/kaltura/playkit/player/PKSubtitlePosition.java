package com.kaltura.playkit.player;

import android.text.Layout;

import com.kaltura.playkit.utils.Consts;

public class PKSubtitlePosition {

    // Allow the subtitle view to move left (ALIGN_NORMAL), middle (ALIGN_CENTER) and right (ALIGN_OPPOSITE)
    private Layout.Alignment subtitleHorizontalPosition = Layout.Alignment.ALIGN_CENTER;
    // Allow the subtitle view to move vertically
    private float verticalPositionPercentage = 1.0f;
    // Allow the subtitle view to move horizontally but with in the left-middle and middle-right viewport
    private float horizontalPositionPercentage = 1.0f;
    private float defaultPercentage = 1.0f;
    private float positionLowerLimit = 10.0f;

    public Layout.Alignment getSubtitleHorizontalPosition() {
        return subtitleHorizontalPosition;
    }

    public float getVerticalPositionPercentage() {
        return verticalPositionPercentage;
    }

    public float getHorizontalPositionPercentage() {
        return horizontalPositionPercentage;
    }

    public PKSubtitlePosition setSubtitleHorizontalPosition(Layout.Alignment subtitleHorizontalPosition) {
        this.subtitleHorizontalPosition = subtitleHorizontalPosition;
        return this;
    }

    public PKSubtitlePosition setVerticalPositionPercentage(float verticalPositionPercentage) {
        if (verticalPositionPercentage < positionLowerLimit || verticalPositionPercentage > Consts.PERCENT_FACTOR_FLOAT) {
            this.verticalPositionPercentage = defaultPercentage;
            return this;
        }

        this.verticalPositionPercentage = verticalPositionPercentage / Consts.PERCENT_FACTOR_FLOAT;
        return this;
    }

    public PKSubtitlePosition setHorizontalPositionPercentage(float horizontalPositionPercentage) {
        if (horizontalPositionPercentage < positionLowerLimit || horizontalPositionPercentage > Consts.PERCENT_FACTOR_FLOAT) {
            this.horizontalPositionPercentage = defaultPercentage;
            return this;
        }

        this.horizontalPositionPercentage = horizontalPositionPercentage / Consts.PERCENT_FACTOR_FLOAT;
        return this;
    }

    public PKSubtitlePosition resetSubtitleViewPosition() {
        this.subtitleHorizontalPosition = Layout.Alignment.ALIGN_CENTER;
        this.verticalPositionPercentage = defaultPercentage;
        this.horizontalPositionPercentage = defaultPercentage;

        return this;
    }
}
