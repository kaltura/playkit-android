package com.kaltura.playkit.player;

import android.text.Layout;

public class PKSubtitlePosition {

    private Layout.Alignment subtitleHorizontalPosition = Layout.Alignment.ALIGN_CENTER;
    private float verticalPositionPercentage = 1.0f;
    private float horizontalPositionPercentage = 1.0f;
    private boolean resetSubtitlePosition = false;

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
        if (verticalPositionPercentage < 10 || verticalPositionPercentage > 100) {
            this.verticalPositionPercentage = 1.0f;
            return this;
        }

        this.verticalPositionPercentage = verticalPositionPercentage / 100f;
        return this;
    }

    public PKSubtitlePosition setHorizontalPositionPercentage(float horizontalPositionPercentage) {
        if (horizontalPositionPercentage < 10 || horizontalPositionPercentage > 100) {
            this.horizontalPositionPercentage = 1.0f;
            return this;
        }

        this.horizontalPositionPercentage = horizontalPositionPercentage / 100f;
        return this;
    }
}
