package com.kaltura.playkit.player;

import android.text.Layout;

import com.kaltura.android.exoplayer2.text.Cue;
import com.kaltura.playkit.utils.Consts;

public class PKSubtitlePosition {

    // If `ignoreCueSettings` is set to true, player will ignore the existing values coming in Cue Settings
    // and will always use the given Cue Settings
    public PKSubtitlePosition(boolean ignoreCueSettings) {
        this.ignoreCueSettings = ignoreCueSettings;
    }

    // For Left to Right Language texts(LTR), Allow the subtitle view to move left (ALIGN_NORMAL), middle (ALIGN_CENTER) and right (ALIGN_OPPOSITE)
    // For Right to Left Language texts(RTL), Allow the subtitle view to move Right (ALIGN_NORMAL), middle (ALIGN_CENTER) and left (ALIGN_OPPOSITE)
    private Layout.Alignment subtitleHorizontalPosition = Layout.Alignment.ALIGN_CENTER;

    // Allow the subtitle view to move vertically (100% = 1.0f OR 10% = 0.1f)
    // For vertical positions, percentage starts from Top(0.1f) to Bottom(1.0f)
    private float verticalPositionPercentage = Cue.DIMEN_UNSET;

    // Allow the subtitle view to move horizontally but with in the left-middle and middle-right viewport
    // For horizontal(Left viewport) positions, percentage starts from center(0.1f or 10%) to Left(1.0f or 100%)
    // For horizontal(Right viewport) positions, percentage starts from center(0.1f or 10%) to Right(1.0f or 100%)
    private float horizontalPositionPercentage = Cue.DIMEN_UNSET;

    // The type of line, Fraction, Number, Unset. We are accepting values for Line in fraction so
    // keeping it LINE_TYPE_FRACTION. It is not exposed.
    private int lineType = Cue.LINE_TYPE_FRACTION;

    // Lower limit either for vertical (top to bottom) for horizontal (center to left/right)
    private float positionLowerLimit = 0.10f;

    // Override the current subtitle Positioning with the In-stream subtitle text track configuration
    private boolean ignoreCueSettings;

    public Layout.Alignment getSubtitleHorizontalPosition() {
        return subtitleHorizontalPosition;
    }

    public float getVerticalPositionPercentage() {
        return verticalPositionPercentage;
    }

    public float getHorizontalPositionPercentage() {
        return horizontalPositionPercentage;
    }

    public boolean isIgnoreCueSettings() {
        return ignoreCueSettings;
    }

    public int getLineType() {
        return lineType;
    }

    /**
     * Allow the subtitle view to move left (ALIGN_NORMAL), middle (ALIGN_CENTER) and right (ALIGN_OPPOSITE)
     * For RTL texts, Allow the subtitle view to move Right (ALIGN_NORMAL), middle (ALIGN_CENTER) and left (ALIGN_OPPOSITE)
     *
     * Set the horizontal(Left/Right viewport) positions, percentage starts from
     * center(0.1f is for 10%) to Left/Right(1.0f is for 100%)
     *
     * @param horizontalPosition subtitle view positioning
     * @param horizontalPositionPercentage percentage to left/right viewport from center
     * @return PKSubtitlePosition
     */
    private PKSubtitlePosition setHorizontalPositionLevel(Layout.Alignment horizontalPosition, float horizontalPositionPercentage) {
        this.subtitleHorizontalPosition = horizontalPosition;

        if (horizontalPositionPercentage < positionLowerLimit || horizontalPositionPercentage > Consts.DEFAULT_MAX_SUBTITLE_POSITION) {
            horizontalPositionPercentage = Cue.DIMEN_UNSET;
        }

        this.horizontalPositionPercentage = horizontalPositionPercentage;

        return this;
    }

    /**
     * Set the vertical(Top to Bottom) positions, percentage starts from
     * Top(0.1f is for 10%) to Bottom(1.0f is for 100%)
     *
     * @param verticalPositionPercentage percentage to vertical viewport from top to bottom
     * @return PKSubtitlePosition
     */
    private PKSubtitlePosition setVerticalPositionLevel(float verticalPositionPercentage) {
        if (verticalPositionPercentage < positionLowerLimit || verticalPositionPercentage > Consts.DEFAULT_MAX_SUBTITLE_POSITION) {
            verticalPositionPercentage = Cue.DIMEN_UNSET;
        }

        this.verticalPositionPercentage = verticalPositionPercentage;
        return this;
    }

    public PKSubtitlePosition setPosition(Layout.Alignment horizontalPosition, float horizontalPositionPercentage, float verticalPositionPercentage) {
        setHorizontalPositionLevel(horizontalPosition, horizontalPositionPercentage);
        setVerticalPositionLevel(verticalPositionPercentage);
        lineType = Cue.LINE_TYPE_FRACTION;
        return this;
    }

    public PKSubtitlePosition setVerticalPosition(float verticalPositionPercentage) {
        setVerticalPositionLevel(verticalPositionPercentage);
        subtitleHorizontalPosition = Layout.Alignment.ALIGN_CENTER;
        horizontalPositionPercentage = Cue.DIMEN_UNSET;
        lineType = Cue.LINE_TYPE_FRACTION;
        return this;
    }

    /**
     * Reset all the subtitle view positioning to default
     * It will move subtitle to Center
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition resetPosition() {
        subtitleHorizontalPosition = null;
        verticalPositionPercentage = Cue.DIMEN_UNSET;
        horizontalPositionPercentage = Cue.DIMEN_UNSET;
        lineType = Cue.TYPE_UNSET;
        return this;
    }

    /**
     * Override the current subtitle Positioning with the In-stream subtitle text track configuration
     * This is different then resetting the subtitle position because in reset we set in variables to
     * Cue.DIMEN_UNSET which eventually keeps everything in center.
     * In this API, we are asking the Cue to pick whatever is coming in In-stream subtitle, does not
     * care if configuration is there or not.
     *
     * @param ignoreCueSettings true or false
     * @return PKSubtitlePosition
     */
    public PKSubtitlePosition setIgnoreCueSettings(boolean ignoreCueSettings) {
        this.ignoreCueSettings = ignoreCueSettings;
        return this;
    }
}
