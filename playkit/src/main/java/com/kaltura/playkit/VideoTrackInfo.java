package com.kaltura.playkit;

/**
 * Video track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
public class VideoTrackInfo extends BaseTrackInfo{

    private int width;
    private int height;
    private long bitrate;


    public VideoTrackInfo(String uniqueId, long bitrate, int width, int height, boolean isAdaptive) {
        super(uniqueId, isAdaptive);
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
    }

    /**
     * Getter for the track bitrate.
     * @return - the bitrate of the track.
     */
    public long getBitrate() {
        return bitrate;
    }

    /**
     * Getter for the track width.
     * @return - the width of the track.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Getter for the track height.
     * @return - the height of the track.
     */
    public int getHeight() {
        return height;
    }
}
