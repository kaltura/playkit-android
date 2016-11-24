package com.kaltura.playkit;

/**
 * Created by anton.afanasiev on 17/11/2016.
 */
public class VideoTrackData {

    private long bitrate;
    private int width;
    private int height;
    private String id;


    public VideoTrackData(long bitrate, int width, int height, String id) {
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
        this.id = id;
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

    public String getId() {
        return id;
    }

}
