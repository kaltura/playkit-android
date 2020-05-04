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

    // The type of line, Fraction, Number, Unset. We are accepting values for Line in fraction so
    // keeping it LINE_TYPE_FRACTION. It is not exposed.
    private int lineType = Cue.LINE_TYPE_FRACTION;

    // Lower limit either for vertical (top to bottom) for horizontal (center to left/right)
    private float positionLowerLimit = 0.10f;

    // Override the current subtitle Positioning with the In-stream subtitle text track configuration
    private boolean overrideSubtitlePositionConfig = false;

    public Layout.Alignment getSubtitleHorizontalPosition() {
        return subtitleHorizontalPosition;
    }

    public float getVerticalPositionPercentage() {
        return verticalPositionPercentage;
    }

    public float getHorizontalPositionPercentage() {
        return horizontalPositionPercentage;
    }

    public boolean isOverrideSubtitlePositionConfig() {
        return overrideSubtitlePositionConfig;
    }

    public int getLineType() {
        return lineType;
    }

    /**
     * Allow the subtitle view to move left (ALIGN_NORMAL), middle (ALIGN_CENTER) and right (ALIGN_OPPOSITE)
     * For RTL texts, Allow the subtitle view to move Right (ALIGN_NORMAL), middle (ALIGN_CENTER) and left (ALIGN_OPPOSITE)
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
        this.subtitleHorizontalPosition = null;
        this.verticalPositionPercentage = Cue.DIMEN_UNSET;
        this.horizontalPositionPercentage = Cue.DIMEN_UNSET;
        this.lineType = Cue.TYPE_UNSET;
        return this;
    }

    /**
     * Override the current subtitle Positioning with the In-stream subtitle text track configuration
     * This is different then resetting the subtitle position because in reset we set in variables to
     * Cue.DIMEN_UNSET which eventually keeps everything in center.
     * In this API, we are asking the Cue to pick whatever is coming in In-stream subtitle, does not
     * care if configuration is there or not.
     *
     * @param overrideSubtitlePositionConfig true or false
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setOverrideSubtitlePositionConfig(boolean overrideSubtitlePositionConfig) {
        this.overrideSubtitlePositionConfig = overrideSubtitlePositionConfig;
        return this;
    }
}
