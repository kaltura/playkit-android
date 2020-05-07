package com.kaltura.playkit.player;

import android.text.Layout;

import com.kaltura.android.exoplayer2.text.Cue;
import com.kaltura.playkit.utils.Consts;

public class PKSubtitlePosition {

    // If `overrideInlineCueConfig` is set to true, player will ignore the existing values coming in Cue Settings
    // and will always use the given Cue Settings
    public PKSubtitlePosition(boolean overrideInlineCueConfig) {
        this.overrideInlineCueConfig = overrideInlineCueConfig;
    }

    // For Left to Right Language texts(LTR), Allow the subtitle view to move left (ALIGN_NORMAL), middle (ALIGN_CENTER) and right (ALIGN_OPPOSITE)
    // For Right to Left Language texts(RTL), Allow the subtitle view to move Right (ALIGN_NORMAL), middle (ALIGN_CENTER) and left (ALIGN_OPPOSITE)
    private Layout.Alignment subtitleHorizontalPosition = Layout.Alignment.ALIGN_CENTER;

    // Allow the subtitle view to move vertically (100% = 100 OR 10% = 10)
    // For vertical positions, percentage starts from Top(100) to Bottom(10)
    private float verticalPositionPercentage = Cue.DIMEN_UNSET;

    // Allow the subtitle view to move horizontally but with in the left-middle and middle-right viewport
    // For horizontal(Left viewport) positions, percentage starts from center(10 or 10%) to Left(100 or 100%)
    // For horizontal(Right viewport) positions, percentage starts from center(10 or 10%) to Right(100 or 100%)
    private float horizontalPositionPercentage = Cue.DIMEN_UNSET;

    // The type of line, Fraction, Number, Unset. We are accepting values for Line in fraction so
    // keeping it LINE_TYPE_FRACTION. It is not exposed.
    private int lineType = Cue.LINE_TYPE_FRACTION;

    // Lower limit either for vertical (top to bottom) for horizontal (center to left/right)
    private int positionLowerLimit = 10;

    // Override the current subtitle Positioning with the In-stream subtitle text track configuration
    private boolean overrideInlineCueConfig;

    public Layout.Alignment getSubtitleHorizontalPosition() {
        return subtitleHorizontalPosition;
    }

    public float getVerticalPositionPercentage() {
        return verticalPositionPercentage;
    }

    public float getHorizontalPositionPercentage() {
        return horizontalPositionPercentage;
    }

    public boolean isOverrideInlineCueConfig() {
        return overrideInlineCueConfig;
    }

    public int getLineType() {
        return lineType;
    }

    /**
     * Allow the subtitle view to move left (ALIGN_NORMAL), middle (ALIGN_CENTER) and right (ALIGN_OPPOSITE)
     * For RTL texts, Allow the subtitle view to move Right (ALIGN_NORMAL), middle (ALIGN_CENTER) and left (ALIGN_OPPOSITE)
     *
     * Set the horizontal(Left/Right viewport) positions, percentage starts from
     * center(10 is for 10%) to Left/Right(100 is for 100%)
     *
     * @param horizontalPosition subtitle view positioning
     * @param horizontalPositionPercentage percentage to left/right viewport from center
     * @return PKSubtitlePosition
     */
    private PKSubtitlePosition setHorizontalPositionLevel(Layout.Alignment horizontalPosition, float horizontalPositionPercentage) {
        this.subtitleHorizontalPosition = horizontalPosition;
        this.horizontalPositionPercentage = horizontalPositionPercentage;
        return this;
    }

    /**
     * Set the vertical(Top to Bottom) positions, percentage starts from
     * Top(10 is for 10%) to Bottom(100 is for 100%)
     *
     * @param verticalPositionPercentage percentage to vertical viewport from top to bottom
     * @return PKSubtitlePosition
     */
    private PKSubtitlePosition setVerticalPositionLevel(float verticalPositionPercentage) {
        this.verticalPositionPercentage = verticalPositionPercentage;
        return this;
    }

    /**
     * Set the subtitle position any where on the video frame. This method allows to move in X-Y coordinates
     * To set the subtitle only in vertical direction (Y - coordinate) use {@link PKSubtitlePosition#setVerticalPosition(int)}
     *
     * @param horizontalPositionPercentage Set the horizontal(Left/Right viewport) positions, percentage starts from
     *                                     center(10 is for 10%) to Left/Right(100 is for 100%)
     *
     * @param verticalPositionPercentage Set the vertical(Top to Bottom) positions, percentage starts from
     *                                   Top(10 is for 10%) to Bottom(100 is for 100%)
     *
     * @param horizontalAlignment Allow the subtitle view to move left (ALIGN_NORMAL), middle (ALIGN_CENTER) and right (ALIGN_OPPOSITE)
     *                           For RTL texts, Allow the subtitle view to move Right (ALIGN_NORMAL), middle (ALIGN_CENTER) and left (ALIGN_OPPOSITE)
     *
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setPosition(int horizontalPositionPercentage, int verticalPositionPercentage, Layout.Alignment horizontalAlignment) {
        float verticalPosition = checkPositionPercentageLimit(verticalPositionPercentage);
        float horizontalPosition = checkPositionPercentageLimit(horizontalPositionPercentage);

        setHorizontalPositionLevel(horizontalAlignment, horizontalPosition);
        setVerticalPositionLevel(verticalPosition);
        lineType = Cue.LINE_TYPE_FRACTION;
        return this;
    }

    /**
     * Set the subtitle position only in Vertical direction (Up or Down) on the video frame. This method only allows to move in Y - coordinate
     * To set the subtitle any where on the video frame use {@link PKSubtitlePosition#setPosition(int, int, Layout.Alignment)}
     *
     * @param verticalPositionPercentage Top(10 is for 10%) to Bottom(100 is for 100%)
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setVerticalPosition(int verticalPositionPercentage) {
        float verticalPosition = checkPositionPercentageLimit(verticalPositionPercentage);

        setVerticalPositionLevel(verticalPosition);
        subtitleHorizontalPosition = Layout.Alignment.ALIGN_CENTER;
        horizontalPositionPercentage = Cue.DIMEN_UNSET;
        lineType = Cue.LINE_TYPE_FRACTION;
        return this;
    }

    /**
     * If `overrideInlineCueConfig` is false that mean; app does not want to override the inline Cue configuration.
     * App wants to go with Cue configuration.
     * BUT Beware that it will call {@link PKSubtitlePosition#setOverrideInlineCueConfig(boolean)} with false value
     * means after that in next call, app needs to {@link PKSubtitlePosition#setOverrideInlineCueConfig(boolean)}
     * with another value.
     *
     * OTHERWISE
     *
     * If `overrideInlineCueConfig` is true then it will move subtitle to Bottom-Center which is a standard position for it
     *
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setToDefaultPosition(boolean overrideInlineCueConfig) {

        if (!overrideInlineCueConfig) {
            setOverrideInlineCueConfig(false);
            return this;
        }

        subtitleHorizontalPosition = null;
        verticalPositionPercentage = Cue.DIMEN_UNSET;
        horizontalPositionPercentage = Cue.DIMEN_UNSET;
        lineType = Cue.TYPE_UNSET;
        return this;
    }

    /**
     * If `overrideInlineCueConfig` is set to true, player will ignore the existing values coming in Cue Settings
     * and will always use the given Cue Settings
     *
     * @param overrideInlineCueConfig true or false
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setOverrideInlineCueConfig(boolean overrideInlineCueConfig) {
        this.overrideInlineCueConfig = overrideInlineCueConfig;
        return this;
    }

    private float checkPositionPercentageLimit(int positionPercentage) {
        float position;
        if (positionPercentage < positionLowerLimit || positionPercentage > Consts.DEFAULT_MAX_SUBTITLE_POSITION) {
            position = Cue.DIMEN_UNSET;
        } else {
            position = (float) positionPercentage / Consts.PERCENT_FACTOR_FLOAT;
        }
        return position;
    }
}
