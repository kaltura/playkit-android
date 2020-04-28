package com.kaltura.playkit.player;

import android.text.Layout;

import com.kaltura.android.exoplayer2.text.Cue;
import com.kaltura.playkit.utils.Consts;

import static com.kaltura.android.exoplayer2.text.Cue.DIMEN_UNSET;

public class PKSubtitlePosition {

    // Allow the subtitle view to move left (ALIGN_NORMAL), middle (ALIGN_CENTER) and right (ALIGN_OPPOSITE)
    private Layout.Alignment subtitleHorizontalPosition = Layout.Alignment.ALIGN_CENTER;

    // Allow the subtitle view to move vertically (100f / 100f = 1.0f OR 10f / 100f = 0.1f)
    // For vertical positions, percentage starts from Top(10.0f or 10%) to Bottom(100.0f or 100%)
    private float verticalPositionPercentage = 1.0f;

    // Allow the subtitle view to move horizontally but with in the left-middle and middle-right viewport
    // For horizontal(Left viewport) positions, percentage starts from center(10.0f or 10%) to Left(100.0f or 100%)
    // For horizontal(Right viewport) positions, percentage starts from center(10.0f or 10%) to Right(100.0f or 100%)
    private float horizontalPositionPercentage = 1.0f;

    // Lower limit either for vertical (top to bottom) for horizontal (center to left/right)
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

    /**
     * Allow the subtitle view to move left (ALIGN_NORMAL), middle (ALIGN_CENTER) and right (ALIGN_OPPOSITE)
     *
     * @param subtitleHorizontalPosition subtitle view positionaing
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setSubtitleHorizontalPosition(Layout.Alignment subtitleHorizontalPosition) {
        this.subtitleHorizontalPosition = subtitleHorizontalPosition;
        return this;
    }

    /**
     * Set the vertical(Top to Bottom) positions, percentage starts from
     * Top(10.0f is for 10%) to Bottom(100.0f is for 100%)
     *
     * @param verticalPositionPercentage percentage to vertical viewport from top to bottom
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setVerticalPositionPercentage(float verticalPositionPercentage) {
        if (verticalPositionPercentage < positionLowerLimit || verticalPositionPercentage > Consts.PERCENT_FACTOR_FLOAT) {
            verticalPositionPercentage = Consts.PERCENT_FACTOR_FLOAT;
        }

        this.verticalPositionPercentage = verticalPositionPercentage / Consts.PERCENT_FACTOR_FLOAT;
        return this;
    }

    /**
     * Set the horizontal(Left/Right viewport) positions, percentage starts from
     * center(10.0f is for 10%) to Left/Right(100.0f is for 100%)
     *
     * @param horizontalPositionPercentage percentage to left/right viewport from center
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setHorizontalPositionPercentage(float horizontalPositionPercentage) {
        if (horizontalPositionPercentage < positionLowerLimit || horizontalPositionPercentage > Consts.PERCENT_FACTOR_FLOAT) {
            horizontalPositionPercentage = Consts.PERCENT_FACTOR_FLOAT;
        }

        this.horizontalPositionPercentage = horizontalPositionPercentage / Consts.PERCENT_FACTOR_FLOAT;
        return this;
    }

    /**
     * Reset all the subtitle view postioning to default
     * It will move subtitle to Center
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition resetSubtitleViewPosition() {
        this.subtitleHorizontalPosition = Layout.Alignment.ALIGN_CENTER;
        this.verticalPositionPercentage = Cue.DIMEN_UNSET;
        this.horizontalPositionPercentage = Cue.DIMEN_UNSET;

        return this;
    }
}
