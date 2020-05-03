package com.kaltura.playkit.player;

import android.text.Layout;

import com.kaltura.android.exoplayer2.text.Cue;
import com.kaltura.playkit.utils.Consts;

public class PKSubtitlePosition {

    // For Left to Right Language texts(LTR), Allow the subtitle view to move left (ALIGN_NORMAL), middle (ALIGN_CENTER) and right (ALIGN_OPPOSITE)
    // For Right to Left Language texts(RTL), Allow the subtitle view to move Right (ALIGN_NORMAL), middle (ALIGN_CENTER) and left (ALIGN_OPPOSITE)
    private Layout.Alignment subtitleHorizontalPosition = Layout.Alignment.ALIGN_CENTER;

    // Allow the subtitle view to move vertically (100% = 1.0f OR 10% = 0.1f)
    // For vertical positions, percentage starts from Top(0.1f) to Bottom(1.0f)
    private float verticalPositionPercentage = Consts.DEFAULT_MAX_SUBTITLE_POSITION;

    // Allow the subtitle view to move horizontally but with in the left-middle and middle-right viewport
    // For horizontal(Left viewport) positions, percentage starts from center(0.1f or 10%) to Left(1.0f or 100%)
    // For horizontal(Right viewport) positions, percentage starts from center(0.1f or 10%) to Right(1.0f or 100%)
    private float horizontalPositionPercentage = Consts.DEFAULT_MAX_SUBTITLE_POSITION;

    // Lower limit either for vertical (top to bottom) for horizontal (center to left/right)
    private float positionLowerLimit = 0.10f;

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
     * @param subtitleHorizontalPosition subtitle view positioning
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setSubtitleHorizontalPosition(Layout.Alignment subtitleHorizontalPosition) {
        this.subtitleHorizontalPosition = subtitleHorizontalPosition;
        return this;
    }

    /**
     * Set the vertical(Top to Bottom) positions, percentage starts from
     * Top(0.1f is for 10%) to Bottom(1.0f is for 100%)
     *
     * @param verticalPositionPercentage percentage to vertical viewport from top to bottom
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setVerticalPositionPercentage(float verticalPositionPercentage) {
        if (verticalPositionPercentage < positionLowerLimit || verticalPositionPercentage > Consts.DEFAULT_MAX_SUBTITLE_POSITION) {
            verticalPositionPercentage = Cue.DIMEN_UNSET;
        }

        this.verticalPositionPercentage = verticalPositionPercentage;
        return this;
    }

    /**
     * Set the horizontal(Left/Right viewport) positions, percentage starts from
     * center(0.1f is for 10%) to Left/Right(1.0f is for 100%)
     *
     * @param horizontalPositionPercentage percentage to left/right viewport from center
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setHorizontalPositionPercentage(float horizontalPositionPercentage) {
        if (horizontalPositionPercentage < positionLowerLimit || horizontalPositionPercentage > Consts.DEFAULT_MAX_SUBTITLE_POSITION) {
            horizontalPositionPercentage = Cue.DIMEN_UNSET;
        }

        this.horizontalPositionPercentage = horizontalPositionPercentage;
        return this;
    }

    /**
     * Reset all the subtitle view positioning to default
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
