package com.kaltura.playkit.player.thumbnail;

public class ImageRangeInfo {
    private final long startPosition;
    private final long endPosition;

    public ImageRangeInfo(long startPosition, long endPosition) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public long getEndPosition() {
        return endPosition;
    }
}

