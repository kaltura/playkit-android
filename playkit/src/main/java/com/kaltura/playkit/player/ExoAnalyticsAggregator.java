package com.kaltura.playkit.player;

import android.os.SystemClock;

import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.android.exoplayer2.decoder.DecoderCounters;
import com.kaltura.android.exoplayer2.source.MediaSourceEventListener;
import com.kaltura.playkit.PKLog;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;

import static com.kaltura.playkit.utils.Consts.HTTP_METHOD_GET;

class ExoAnalyticsAggregator extends EventListener implements AnalyticsListener {

    private static final PKLog log = PKLog.get("ExoAnalyticsAggregator");
    private final Map<String, Long>  domainCallStartRelTimeMap = new ConcurrentHashMap<>();
    private long totalDroppedFrames;
    private long totalBytesLoaded;
    private int renderedOutputBufferCount;
    private int skippedOutputBufferCount;

    private PlayerEngine.AnalyticsListener listener;

    void reset() {
        totalDroppedFrames = 0;
        totalBytesLoaded = 0;
        renderedOutputBufferCount = 0;
        skippedOutputBufferCount = 0;
    }

    @Override
    public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
        totalDroppedFrames += droppedFrames;
        if (listener != null) {
            listener.onDroppedFrames(droppedFrames, elapsedMs, totalDroppedFrames);
        }
    }

    @Override
    public void onDecoderDisabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {
        if (trackType == C.TRACK_TYPE_VIDEO || trackType == C.TRACK_TYPE_DEFAULT ) {
            skippedOutputBufferCount  = decoderCounters.skippedOutputBufferCount;
            renderedOutputBufferCount = decoderCounters.renderedOutputBufferCount;
            listener.onDecoderDisabled(skippedOutputBufferCount, renderedOutputBufferCount);
        }
    }

    @Override
    public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (loadEventInfo.bytesLoaded > 0) {
            if (mediaLoadData.trackType == C.TRACK_TYPE_VIDEO  || mediaLoadData.trackType == C.TRACK_TYPE_AUDIO || mediaLoadData.trackType == C.TRACK_TYPE_DEFAULT) { // in HLS track type 0 is sent in dash type 1 is sent
                totalBytesLoaded += loadEventInfo.bytesLoaded;
            }
            log.v("onLoadCompleted trackType = " + mediaLoadData.trackType + ", mediaLoadData.dataType " + mediaLoadData.dataType + ", " + loadEventInfo.loadDurationMs + " " + loadEventInfo.uri.toString());
            if (listener != null) {
                listener.onBytesLoaded(mediaLoadData.trackType, mediaLoadData.dataType, loadEventInfo.bytesLoaded, loadEventInfo.loadDurationMs, totalBytesLoaded);
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
    
    @Override // EXO
    public void onLoadStarted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        String loadedURL = loadEventInfo.uri.toString();
        log.v("onLoadStarted = " + eventTime.realtimeMs + " url = " + loadedURL);
        if (domainCallStartRelTimeMap != null && mediaLoadData.trackType != C.TRACK_TYPE_VIDEO && domainCallStartRelTimeMap != null && mediaLoadData.trackType != C.TRACK_TYPE_AUDIO && mediaLoadData.trackType != C.TRACK_TYPE_DEFAULT) {
            if (domainCallStartRelTimeMap.containsKey(loadedURL)) {
                domainCallStartRelTimeMap.remove(loadedURL);
                //log.v("onLoadStarted ignore url = " + loadedURL);
            }
        }
    }

    @Override // OKHTTTP
    public void callStart(Call call) {
        String loadedURL = call.request().url().toString();
        log.v("callStart = " + loadedURL);
        if (HTTP_METHOD_GET.equals(call.request().method())) {
            domainCallStartRelTimeMap.put(loadedURL, SystemClock.elapsedRealtime());
            //log.v("callStart put = " + SystemClock.elapsedRealtime() + " url = " + loadedURL);
        }
    }

    @Override // OKHTTTP
    public void connectionAcquired(Call call, Connection connection) {
        String loadedURL = call.request().url().toString();
        log.v("connectionAcquired = " + loadedURL);
        if (domainCallStartRelTimeMap.containsKey(loadedURL)) {
            Long callStartTime = domainCallStartRelTimeMap.get(loadedURL);
            if (callStartTime != null) {
                long acquireTime = SystemClock.elapsedRealtime();
                //log.v("connectionAcquired update " + " url = " + loadedURL);
                domainCallStartRelTimeMap.put(loadedURL, (acquireTime - callStartTime));
            }
        }
    }

    @Override // OKHTTTP
    public void connectionReleased(Call call, Connection connection) {
        String loadedURL = call.request().url().toString();
        log.v("connectionReleased = " + loadedURL);
        if (domainCallStartRelTimeMap.containsKey(loadedURL)) {
            Long callDiffTime = domainCallStartRelTimeMap.get(loadedURL);
            if (callDiffTime != null) {
                    listener.onConnectionAcquired(callDiffTime);
                    //log.v("connectionReleased SEND EVENT = " + callDiffTime + " url = " + loadedURL);
            }
            domainCallStartRelTimeMap.remove(loadedURL);
        }
    }
}

