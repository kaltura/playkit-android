package com.kaltura.playkit.player;

public class ThumbnailInfo {
    private String url;  // url of the image that contains the thumbnail slice
    private int x;       // x position of the thumbnail
    private int y;       // y position of the thumbnail
    private int width;   // width of the thumbnail
    private int height;  // height of the thumbnail

    public ThumbnailInfo(String url, int x, int y, int width, int height) {
        this.url = url;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getUrl() {
        return url;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}