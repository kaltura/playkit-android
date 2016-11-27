package com.kaltura.playkit;

/**
 * Created by anton.afanasiev on 17/11/2016.
 */
public class VideoTrackInfo extends BaseTrackInfo{

    private int width;
    private int height;
    private long bitrate;


    public VideoTrackInfo(long bitrate, int width, int height, String uniqueId, int groupIndex, int trackIndex) {
        super(uniqueId, groupIndex, trackIndex);
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
    }

    public long getBitrate() {
        return bitrate;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
