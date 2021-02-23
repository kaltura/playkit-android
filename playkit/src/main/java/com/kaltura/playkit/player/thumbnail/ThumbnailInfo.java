package com.kaltura.playkit.player.thumbnail;

public class ThumbnailInfo {
    private String url;  // url of the image that contains the thumbnail slice
    private float x;       // x position of the thumbnail
    private float y;       // y position of the thumbnail
    private float width;   // width of the thumbnail
    private float height;  // height of the thumbnail

    public ThumbnailInfo(String url, float x, float y, float width, float height) {
        this.url = url;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getUrl() {
        return url;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}