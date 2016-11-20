package com.kaltura.playkit;

/**
 * Created by anton.afanasiev on 17/11/2016.
 */
public class VideoTrackData {

    private long bitrate;
    private String mimeType;
    private String codecs;
    private int width;
    private int height;
    private int id;
    private float pixelWidthHeightRation;


    public VideoTrackData(long bitrate, String mimeType, String codecs, int width, int height, int id, float pixelWidthHeightRation) {
        this.bitrate = bitrate;
        this.mimeType = mimeType;
        this.codecs = codecs;
        this.width = width;
        this.height = height;
        this.id = id;
        this.pixelWidthHeightRation = pixelWidthHeightRation;
    }

    public long getBitrate() {
        return bitrate;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getCodecs() {
        return codecs;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getId() {
        return id;
    }

    public float getPixelWidthHeightRation() {
        return pixelWidthHeightRation;
    }
}
