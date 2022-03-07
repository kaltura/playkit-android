package com.kaltura.playkit.player;

import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.android.exoplayer2.decoder.DecoderCounters;
import com.kaltura.android.exoplayer2.decoder.DecoderReuseEvaluation;
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

    public interface InputFormatChangedListener {
        void onVideoInputFormatChanged(@NonNull Format format);
        void onAudioInputFormatChanged(@NonNull Format format);
    }

    private static final PKLog log = PKLog.get("ExoAnalyticsAggregator");
    private final Map<String, URIConnectionAcquiredInfo> urlCallTimeMap = new ConcurrentHashMap<>();
    private long totalDroppedFrames;
    private long totalBytesLoaded;
    private int renderedOutputBufferCount;
    private int skippedOutputBufferCount;

    private PlayerEngine.AnalyticsListener listener;
    private @Nullable InputFormatChangedListener inputFormatChangedListener;

    void reset() {
        totalDroppedFrames = 0;
        totalBytesLoaded = 0;
        renderedOutputBufferCount = 0;
        skippedOutputBufferCount = 0;
    }

    @Override
    public void onDroppedVideoFrames(@NonNull EventTime eventTime, int droppedFrames, long elapsedMs) {
        totalDroppedFrames += droppedFrames;
        if (listener != null) {
            listener.onDroppedFrames(droppedFrames, elapsedMs, totalDroppedFrames);
        }
    }

    @Override
    public void onVideoDisabled(@NonNull EventTime eventTime, DecoderCounters decoderCounters) {
        skippedOutputBufferCount  = decoderCounters.skippedOutputBufferCount;
        renderedOutputBufferCount = decoderCounters.renderedOutputBufferCount;
        if (listener != null) {
            listener.onDecoderDisabled(skippedOutputBufferCount, renderedOutputBufferCount);
        }
    }

    @Override
    public void onLoadCompleted(@NonNull EventTime eventTime, LoadEventInfo loadEventInfo, @NonNull MediaLoadData mediaLoadData) {
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
        log.v("onIsLoadingChanged eventPlaybackPositionMs = " + eventTime.eventPlaybackPositionMs + " totalBufferedDurationMs = " + eventTime.totalBufferedDurationMs + " isLoading = " + isLoading);
    }

    @Override
    public void onLoadCanceled(@NonNull EventTime eventTime, @NonNull LoadEventInfo loadEventInfo, @NonNull MediaLoadData mediaLoadData) {
        onLoadCompleted(eventTime, loadEventInfo, mediaLoadData);   // in case there are bytes loaded
    }

    @Override
    public void onLoadError(@NonNull EventTime eventTime, @NonNull LoadEventInfo loadEventInfo, @NonNull MediaLoadData mediaLoadData, @NonNull IOException error, boolean wasCanceled) {
        log.v("onLoadError Uri = " + loadEventInfo.uri.toString());
        onLoadCompleted(eventTime, loadEventInfo, mediaLoadData);   // in case there are bytes loaded
        if (listener != null) {
            listener.onLoadError(error, wasCanceled);
        }
    }

    public void setListener(PlayerEngine.AnalyticsListener listener) {
        this.listener = listener;
    }

    public void setInputFormatChangedListener(@Nullable InputFormatChangedListener inputFormatChangedListener) {
        this.inputFormatChangedListener = inputFormatChangedListener;
    }

    @Override // OKHTTTP
    public void callStart(Call call) {
        String loadedURL = call.request().url().toString();
        log.v("callStart = " + loadedURL);
        if (!TextUtils.isEmpty(loadedURL) && HTTP_METHOD_GET.equals(call.request().method())) {
            URIConnectionAcquiredInfo connectionInfo = new URIConnectionAcquiredInfo();
            connectionInfo.connectDurationMs = SystemClock.elapsedRealtime();
            connectionInfo.url = loadedURL;
            urlCallTimeMap.put(loadedURL, connectionInfo);
            //log.v("callStart put = " + SystemClock.elapsedRealtime() + " url = " + loadedURL);
        }
    }

    @Override // OKHTTTP
    public void connectionAcquired(Call call, @NonNull Connection connection) {
        String loadedURL = call.request().url().toString();
        log.v("connectionAcquired = " + loadedURL);
        if (isLoadedURLExists(loadedURL)) {
            URIConnectionAcquiredInfo uriConnectionAcquiredInfo = urlCallTimeMap.get(loadedURL);
            if (uriConnectionAcquiredInfo != null) {
                uriConnectionAcquiredInfo.connectDurationMs = (SystemClock.elapsedRealtime() - uriConnectionAcquiredInfo.connectDurationMs);
            }
        }
    }

    @Override // OKHTTTP
    public void connectionReleased(Call call, @NonNull Connection connection) {
        String loadedURL = call.request().url().toString();
        log.v("connectionReleased = " + loadedURL);
        if (isLoadedURLExists(loadedURL)) {
            if (listener != null) {
                listener.onConnectionAcquired(urlCallTimeMap.get(loadedURL));
                URIConnectionAcquiredInfo uriConnectionAcquiredInfo = urlCallTimeMap.get(loadedURL);
                if (uriConnectionAcquiredInfo != null) {
                    log.v("connectionReleased SEND EVENT");
                }
            }

            urlCallTimeMap.remove(loadedURL);
        }
    }

    @Override
    public void dnsStart(Call call, @NonNull String domainName) {
        log.v("dnsStart");
        String loadedURL = call.request().url().toString();
        if (isLoadedURLExists(loadedURL)) {
            URIConnectionAcquiredInfo uriConnectionAcquiredInfo = urlCallTimeMap.get(loadedURL);
            if (uriConnectionAcquiredInfo != null) {
                uriConnectionAcquiredInfo.dnsDurationMs = SystemClock.elapsedRealtime();
            }
        }
    }

    @Override
    public void dnsEnd(Call call, @NonNull String domainName, @NonNull List<InetAddress> inetAddressList) {
        log.v("dnsEnd");
        String loadedURL = call.request().url().toString();
        if (isLoadedURLExists(loadedURL)) {
            URIConnectionAcquiredInfo uriConnectionAcquiredInfo = urlCallTimeMap.get(loadedURL);
            if (uriConnectionAcquiredInfo != null) {
                uriConnectionAcquiredInfo.dnsDurationMs = (SystemClock.elapsedRealtime() - uriConnectionAcquiredInfo.dnsDurationMs);
            }
        }
    }

    @Override
    public void secureConnectStart(Call call) {
        log.v("secureConnectStart");
        String loadedURL = call.request().url().toString();
        if (isLoadedURLExists(loadedURL)) {
            URIConnectionAcquiredInfo uriConnectionAcquiredInfo = urlCallTimeMap.get(loadedURL);
            if (uriConnectionAcquiredInfo != null) {
                uriConnectionAcquiredInfo.tlsDurationMs = SystemClock.elapsedRealtime();
            }
        }
    }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) {
        log.v("secureConnectEnd");
        String loadedURL = call.request().url().toString();
        if (isLoadedURLExists(loadedURL)) {
            URIConnectionAcquiredInfo uriConnectionAcquiredInfo = urlCallTimeMap.get(loadedURL);
            if (uriConnectionAcquiredInfo != null) {
                uriConnectionAcquiredInfo.tlsDurationMs = (SystemClock.elapsedRealtime() - uriConnectionAcquiredInfo.tlsDurationMs);
            }
        }
    }

    private boolean isLoadedURLExists(String loadedURL) {
        return loadedURL != null && urlCallTimeMap.containsKey(loadedURL) && urlCallTimeMap.get(loadedURL) != null;
    }

    @Override
    public void onVideoInputFormatChanged(@NonNull EventTime eventTime, @NonNull Format format, @Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
        if (inputFormatChangedListener != null) {
            inputFormatChangedListener.onVideoInputFormatChanged(format);
        }
    }

    @Override
    public void onAudioInputFormatChanged(@NonNull EventTime eventTime, @NonNull Format format, @Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
        if (inputFormatChangedListener != null) {
            inputFormatChangedListener.onAudioInputFormatChanged(format);
        }
    }
}

