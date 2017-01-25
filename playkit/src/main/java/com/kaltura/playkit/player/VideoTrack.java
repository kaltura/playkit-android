package com.kaltura.playkit.player;

import com.kaltura.playkit.PKPublicAPI;

/**
 * Video track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
@PKPublicAPI
public class VideoTrack extends BaseTrack {

    private int width;
    private int height;
    private long bitrate;


     VideoTrack(String uniqueId, long bitrate, int width, int height, int selectionFlag, boolean isAdaptive) {
        super(uniqueId, selectionFlag, isAdaptive);
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
    }

    /**
     * Getter for the track bitrate.
     * Can be -1 if unknown or not applicable.
     * @return - the bitrate of the track.
     */
    public long getBitrate() {
        return bitrate;
    }

    /**
     * Getter for the track width.
     * Can be -1 if unknown or not applicable.
     * @return - the width of the track.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Getter for the track height.
     * Can be -1 if unknown or not applicable.
     * @return - the height of the track.
     */
    public int getHeight() {
        return height;
    }
}
