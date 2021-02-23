package com.kaltura.playkit.player.thumbnail;

public class ImageRangeInfo {
    String imagelUrl;
    long startPosition;
    long endPosition;

    public ImageRangeInfo(String imagelUrl, long startPosition, long endPosition) {
        this.imagelUrl = imagelUrl;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
}
