package com.kaltura.playkit.player;

import android.os.SystemClock;

import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.android.exoplayer2.decoder.DecoderCounters;
import com.kaltura.android.exoplayer2.source.LoadEventInfo;
import com.kaltura.android.exoplayer2.source.MediaLoadData;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.player.metadata.URIConnectionAcquiredInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;

import static com.kaltura.playkit.utils.Consts.HTTP_METHOD_GET;

class ExoAnalyticsAggregator extends EventListener implements AnalyticsListener {

    private static final PKLog log = PKLog.get("ExoAnalyticsAggregator");
    private final Map<String, URIConnectionAcquiredInfo> urlCallTimeMap = new ConcurrentHashMap<>();
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
    public void onVideoDisabled(EventTime eventTime, DecoderCounters decoderCounters) {
        skippedOutputBufferCount  = decoderCounters.skippedOutputBufferCount;
        renderedOutputBufferCount = decoderCounters.renderedOutputBufferCount;
        if (listener != null) {
            listener.onDecoderDisabled(skippedOutputBufferCount, renderedOutputBufferCount);
        }
    }
    
    @Override
    public void onLoadCompleted(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
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
    public void onIsLoadingChanged(EventTime eventTime, boolean isLoading) {
        log.v("onIsLoadingChanged eventPlaybackPositionMs = " + eventTime.eventPlaybackPositionMs + " totalBufferedDurationMs = " + eventTime.totalBufferedDurationMs + " isLoading = " +  Boolean.toString(isLoading));
    }

    @Override
    public void onLoadCanceled(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        onLoadCompleted(eventTime, loadEventInfo, mediaLoadData);   // in case there are bytes loaded
    }

    @Override
    public void onLoadError(EventTime eventTime, LoadEventInfo loadEventInfo,MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        onLoadCompleted(eventTime, loadEventInfo, mediaLoadData);   // in case there are bytes loaded
        if (listener != null) {
            listener.onLoadError(error, wasCanceled);
        }
    }

    public void setListener(PlayerEngine.AnalyticsListener listener) {
        this.listener = listener;
    }

    @Override // OKHTTTP
    public void callStart(Call call) {
        String loadedURL = call.request().url().toString();
        log.v("callStart = " + loadedURL);
        if (HTTP_METHOD_GET.equals(call.request().method())) {
            URIConnectionAcquiredInfo connectionInfo = new URIConnectionAcquiredInfo();
            connectionInfo.connectDurationMs = SystemClock.elapsedRealtime();
            connectionInfo.url = loadedURL;
            urlCallTimeMap.put(loadedURL, connectionInfo);
            //log.v("callStart put = " + SystemClock.elapsedRealtime() + " url = " + loadedURL);
        }
    }

    @Override // OKHTTTP
    public void connectionAcquired(Call call, Connection connection) {
        String loadedURL = call.request().url().toString();
        log.v("connectionAcquired = " + loadedURL);
        if (urlCallTimeMap.containsKey(loadedURL) && urlCallTimeMap.get(loadedURL) != null) {
            urlCallTimeMap.get(loadedURL).connectDurationMs = (SystemClock.elapsedRealtime() - urlCallTimeMap.get(loadedURL).connectDurationMs);
        }
    }

    @Override // OKHTTTP
    public void connectionReleased(Call call, Connection connection) {
        String loadedURL = call.request().url().toString();
        log.v("connectionReleased = " + loadedURL);
        if (urlCallTimeMap.containsKey(loadedURL) && urlCallTimeMap.get(loadedURL) != null) {
            if (listener != null) {
                listener.onConnectionAcquired(urlCallTimeMap.get(loadedURL));
                log.v("connectionReleased SEND EVENT " + urlCallTimeMap.get(loadedURL).toString());
            }

            urlCallTimeMap.remove(loadedURL);
        }
    }

    @Override
    public void dnsStart(Call call, String domainName) {
        log.v("dnsStart");
        String loadedURL = call.request().url().toString();
        if (urlCallTimeMap.containsKey(loadedURL) && urlCallTimeMap.get(loadedURL) != null) {
            if (urlCallTimeMap.containsKey(loadedURL)) {
                urlCallTimeMap.get(loadedURL).dnsDurationMs = SystemClock.elapsedRealtime();
            }
        }
    }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        log.v("dnsEnd");
        String loadedURL = call.request().url().toString();
        if (urlCallTimeMap.containsKey(loadedURL) && urlCallTimeMap.get(loadedURL) != null) {
            urlCallTimeMap.get(loadedURL).dnsDurationMs = (SystemClock.elapsedRealtime() - urlCallTimeMap.get(loadedURL).dnsDurationMs);
        }
    }

    @Override
    public void secureConnectStart(Call call) {
        log.v("secureConnectStart");
        String loadedURL = call.request().url().toString();
        if (urlCallTimeMap.containsKey(loadedURL) && urlCallTimeMap.get(loadedURL) != null) {
            urlCallTimeMap.get(loadedURL).tlsDurationMs = SystemClock.elapsedRealtime();
        }
    }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) {
        log.v("secureConnectEnd");
        String loadedURL = call.request().url().toString();
        if (urlCallTimeMap.containsKey(loadedURL) && urlCallTimeMap.get(loadedURL) != null) {
            urlCallTimeMap.get(loadedURL).tlsDurationMs = (SystemClock.elapsedRealtime() - urlCallTimeMap.get(loadedURL).tlsDurationMs);
        }
    }
}

