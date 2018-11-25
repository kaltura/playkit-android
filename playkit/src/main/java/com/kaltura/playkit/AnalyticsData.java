package com.kaltura.playkit;

public class AnalyticsData {
    public long droppedVideoFrames;
    public long totalDroppedVideoFrames;

    public long bytesTransferred;
    public long totalBytesTransferred;

    public long bitrateEstimate;

    @Override
    public String toString() {
        return "AnalyticsData{" +
                "droppedVideoFrames=" + droppedVideoFrames +
                ", totalDroppedVideoFrames=" + totalDroppedVideoFrames +
                ", bytesTransferred=" + bytesTransferred +
                ", totalBytesTransferred=" + totalBytesTransferred +
                ", bitrateEstimate=" + bitrateEstimate +
                '}';
    }
}
