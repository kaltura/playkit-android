package com.kaltura.playkit;

/**
 * Video track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
public class VideoTrack extends BaseTrack {

    private int width;
    private int height;
    private long bitrate;


    public VideoTrack(String uniqueId, long bitrate, int width, int height, boolean isAdaptive) {
        super(uniqueId, isAdaptive);
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
