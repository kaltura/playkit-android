package com.kaltura.playkit.player;

import com.kaltura.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.android.exoplayer2.source.MediaSourceEventListener;

import java.io.IOException;

class ExoAnalyticsAggregator implements AnalyticsListener {

    private long totalDroppedFrames;
    private long totalBytesLoaded;

    private PlayerEngine.AnalyticsListener listener;

    void reset() {
        totalDroppedFrames = 0;
        totalBytesLoaded = 0;
    }

    @Override
    public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
        totalDroppedFrames += droppedFrames;
        if (listener != null) {
            listener.onDroppedFrames(droppedFrames, elapsedMs, totalDroppedFrames);
        }
    }

    @Override
    public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (loadEventInfo.bytesLoaded > 0) {
            totalBytesLoaded += loadEventInfo.bytesLoaded;

            if (listener != null) {
                listener.onBytesLoaded(loadEventInfo.bytesLoaded, totalBytesLoaded);
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
        if (listener != null) {
            listener.onLoadError(error, wasCanceled);
        }
    }

    public void setListener(PlayerEngine.AnalyticsListener listener) {
        this.listener = listener;
    }
}

