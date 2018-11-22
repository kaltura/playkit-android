package com.kaltura.playkit;

public class AnalyticsData {
    public long newDroppedFrames;
    public long newDroppedFramesTimeMs;
    public long totalDroppedFrames;

    public long newBytesTransferred;
    public long totalBytesTransferred;

    public long bitrateEstimate;

    @Override
    public String toString() {
        return "AnalyticsData{" +
                "newDroppedFrames=" + newDroppedFrames +
                ", newDroppedFramesTimeMs=" + newDroppedFramesTimeMs +
                ", totalDroppedFrames=" + totalDroppedFrames +
                ", newBytesTransferred=" + newBytesTransferred +
                ", totalBytesTransferred=" + totalBytesTransferred +
                ", bitrateEstimate=" + bitrateEstimate +
                '}';
    }
}
