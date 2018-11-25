package com.kaltura.playkit.player;

import android.support.annotation.NonNull;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.kaltura.playkit.AnalyticsData;

import java.io.IOException;

class ExoAnalyticsAggregator implements AnalyticsListener {

    private long totalDroppedFrames;
    private long totalBytesTransferred;
    private long lastBitrateEstimate;

    private PlayerEngine.AnalyticsListener listener;

    void reset() {
        totalDroppedFrames = 0;
        totalBytesTransferred = 0;
        lastBitrateEstimate = 0;
    }

    @Override
    public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
        lastBitrateEstimate = bitrateEstimate;
        if (listener != null) {
            final AnalyticsData stats = createPlaybackStats();
            listener.onUpdate(stats);
        }
    }

    @Override
    public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
        totalDroppedFrames += droppedFrames;
        if (listener != null) {
            AnalyticsData stats = createPlaybackStats();
            stats.droppedVideoFrames = droppedFrames;
            listener.onUpdate(stats);
        }
    }

    @Override
    public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (loadEventInfo.bytesLoaded > 0) {
            totalBytesTransferred += loadEventInfo.bytesLoaded;

            if (listener != null) {
                AnalyticsData stats = createPlaybackStats();
                stats.bytesTransferred = loadEventInfo.bytesLoaded;
                listener.onUpdate(stats);
            }
        }
    }

    @Override
    public void onLoadCanceled(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        onLoadCompleted(eventTime, loadEventInfo, mediaLoadData);   // in case there are bytes loaded
    }

    @Override
    public void onLoadError(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        onLoadCompleted(eventTime, loadEventInfo, mediaLoadData);   // in case there are bytes loaded
    }

    @NonNull
    private AnalyticsData createPlaybackStats() {
        AnalyticsData stats = new AnalyticsData();
        stats.bitrateEstimate = lastBitrateEstimate;
        stats.totalBytesTransferred = totalBytesTransferred;
        stats.totalDroppedVideoFrames = totalDroppedFrames;
        return stats;
    }

    public void setListener(PlayerEngine.AnalyticsListener listener) {
        this.listener = listener;
    }
}

