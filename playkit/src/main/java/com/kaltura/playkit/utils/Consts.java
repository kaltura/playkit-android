package com.kaltura.playkit.utils;

/**
 * Created by anton.afanasiev on 04/12/2016.
 */

public class Consts {

    /**
     * Special constant representing an unset or unknown time or duration. Suitable for use in any
     * time base.
     */
    public static final long TIME_UNSET = Long.MIN_VALUE + 1;

    /**
     * Represents an unset or unknown position.
     */
    public static final int POSITION_UNSET = -1;

    /**
     * Represents an unset or unknown volume.
     */
    public static final float VOLUME_UNKNOWN = -1;

    /**
     * Identifier of the Video track type.
     */
    public static final int TRACK_TYPE_VIDEO = 0;
    /**
     * Identifier of the Audio track type.
     */
    public static final int TRACK_TYPE_AUDIO = 1;
    /**
     * Identifier for the Text track type.
     */
    public static final int TRACK_TYPE_TEXT = 2;
    /**
     * Identifier for the unknown track type.
     */
    public static final int TRACK_TYPE_UNKNOWN = -1;
    /**
     * Identifier for the no value.
     */
    public static final long NO_VALUE = -1;
}
