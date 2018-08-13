package com.kaltura.playkit.player;

import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.kaltura.playkit.player.Profiler.Opt;

import java.io.IOException;

import static com.google.android.exoplayer2.C.DATA_TYPE_DRM;
import static com.google.android.exoplayer2.C.DATA_TYPE_MANIFEST;
import static com.google.android.exoplayer2.C.DATA_TYPE_MEDIA;
import static com.google.android.exoplayer2.C.SELECTION_REASON_ADAPTIVE;
import static com.google.android.exoplayer2.C.SELECTION_REASON_INITIAL;
import static com.google.android.exoplayer2.C.SELECTION_REASON_MANUAL;
import static com.google.android.exoplayer2.C.SELECTION_REASON_TRICK_PLAY;
import static com.google.android.exoplayer2.C.SELECTION_REASON_UNKNOWN;
import static com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO;
import static com.google.android.exoplayer2.C.TRACK_TYPE_DEFAULT;
import static com.google.android.exoplayer2.C.TRACK_TYPE_TEXT;
import static com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_AD_INSERTION;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_INTERNAL;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_PERIOD_TRANSITION;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT;

class ExoPlayerProfilingListener implements AnalyticsListener {

    private final Profiler profiler;
    private PlayerEngine playerEngine;

    ExoPlayerProfilingListener(Profiler profiler, PlayerEngine playerEngine) {
        this.profiler = profiler;
        this.playerEngine = playerEngine;
    }

    private Opt opt(String name, Object value) {
        if (value == null) {
            return new Opt(null);
        }

        return new Opt(name + "=" + value);
    }

    public void log(String event, Object... strings) {
        profiler.logWithPlaybackInfo(event, playerEngine, strings);
    }

    private String trackSelectionReasonString(int trackSelectionReason) {
        switch (trackSelectionReason) {
            case SELECTION_REASON_UNKNOWN: return "Unknown";
            case SELECTION_REASON_INITIAL: return "Initial";
            case SELECTION_REASON_MANUAL: return "Manual";
            case SELECTION_REASON_ADAPTIVE: return "Adaptive";
            case SELECTION_REASON_TRICK_PLAY: return "TrickPlay";
            default: return "Unknown:" + trackSelectionReason;
        }
    }

    private String trackFormatString(@Nullable Format trackFormat) {
        if (trackFormat == null) {
            return null;
        }
        return "id=" + trackFormat.id + Profiler.SEPARATOR + "bitrate=" + trackFormat.bitrate + Profiler.SEPARATOR +
                "codecs=" + trackFormat.codecs + Profiler.SEPARATOR + "language=" + trackFormat.language;
    }

    private String trackTypeString(int trackType) {
        switch (trackType) {
            case TRACK_TYPE_DEFAULT: return "Default";
            case TRACK_TYPE_AUDIO: return "Audio";
            case TRACK_TYPE_VIDEO: return "Video";
            case TRACK_TYPE_TEXT: return "Text";
            default: return null;
        }
    }

    private String dataTypeString(int dataType) {
        switch (dataType) {
            case DATA_TYPE_MEDIA: return "Media";
            case DATA_TYPE_MANIFEST: return "Manifest";
            case DATA_TYPE_DRM: return "DRM";
            default: return null;
        }
    }

    private void logLoadingEvent(String event, DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, Long loadDurationMs, Long bytesLoaded, IOException error, Boolean wasCanceled) {
        String dataTypeString = dataTypeString(dataType);
        String trackTypeString = trackTypeString(trackType);

        if (dataTypeString == null || trackTypeString == null) {
            return;
        }

        log(event, "time=" + (elapsedRealtimeMs - profiler.startTime), "uri=" + dataSpec.uri.getLastPathSegment(), "dataType=" + dataTypeString, "trackType=" + trackTypeString,
                trackFormatString(trackFormat), "reason=" + trackSelectionReasonString(trackSelectionReason),
                "rangeStart=" + mediaStartTimeMs, "rangeEnd=" + mediaEndTimeMs, //"bandwidth=" + profiler.lastBandwidthSample,
                opt("loadTime", loadDurationMs), opt("bytes", bytesLoaded), opt("error", error), opt("cancelled", wasCanceled));
    }

    @Override
    public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {
        String state;
        switch (playbackState) {
            case Player.STATE_IDLE:
                state = "Idle";
                break;
            case Player.STATE_BUFFERING:
                state = "Buffering";
                break;
            case Player.STATE_READY:
                state = "Ready";
                break;
            case Player.STATE_ENDED:
                state = "Ended";
                break;
            default: return;
        }

        log("PlayerStateChanged", "state=" + state, "play=" + playWhenReady);
    }

    @Override
    public void onTimelineChanged(EventTime eventTime, int reason) {

    }

    @Override
    public void onPositionDiscontinuity(EventTime eventTime, int reason) {
        String reasonString;
        switch (reason) {
            case DISCONTINUITY_REASON_PERIOD_TRANSITION:
                reasonString = "PeriodTransition";
                break;
            case DISCONTINUITY_REASON_SEEK:
                reasonString = "PeriodTransition";
                break;
            case DISCONTINUITY_REASON_SEEK_ADJUSTMENT:
                reasonString = "PeriodTransition";
                break;
            case DISCONTINUITY_REASON_AD_INSERTION:
                reasonString = "PeriodTransition";
                break;
            case DISCONTINUITY_REASON_INTERNAL:
                reasonString = "PeriodTransition";
                break;
            default:
                reasonString = "Unknown:" + reason;
        }

        log("PositionDiscontinuity", "reason=" + reasonString);
    }

    @Override
    public void onSeekStarted(EventTime eventTime) {
        log("SeekStarted");
    }

    @Override
    public void onSeekProcessed(EventTime eventTime) {
        log("SeekProcessed");
    }

    @Override
    public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {
        log("PlaybackParametersChanged", "speed=" + playbackParameters.speed, "pitch=" + playbackParameters.pitch);
    }

    @Override
    public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {
        String strMode;
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                strMode = "Off";
                break;
            case Player.REPEAT_MODE_ONE:
                strMode = "One";
                break;
            case Player.REPEAT_MODE_ALL:
                strMode = "All";
                break;
            default:
                strMode = "Unknown(" + repeatMode + ")";
                break;
        }
        log("RepeatModeChanged", "repeatMode=" + strMode);
    }

    @Override
    public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {
        log("ShuffleModeChanged", "shuffleModeEnabled=" + shuffleModeEnabled);
    }

    @Override
    public void onLoadingChanged(EventTime eventTime, boolean isLoading) {
        log("LoadingChanged", "isLoading=" + isLoading);
    }

    @Override
    public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {
        String type = null;
        switch (error.type) {
            case ExoPlaybackException.TYPE_SOURCE:
                type = "SourceError";
                break;
            case ExoPlaybackException.TYPE_RENDERER:
                type = "RendererError";
                break;
            case ExoPlaybackException.TYPE_UNEXPECTED:
                type = "UnexpectedError";
                break;
        }

        log("PlayerError", "type=" + type, "cause=" + error.getCause());
    }

    @Override
    public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadStarted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadStarted", loadEventInfo.dataSpec, mediaLoadData.dataType, mediaLoadData.trackType, mediaLoadData.trackFormat, mediaLoadData.trackSelectionReason, mediaLoadData.mediaStartTimeMs, mediaLoadData.mediaEndTimeMs, loadEventInfo.elapsedRealtimeMs, null, null, null, null);
    }

    @Override
    public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadCompleted", loadEventInfo.dataSpec, mediaLoadData.dataType, mediaLoadData.trackType, mediaLoadData.trackFormat, mediaLoadData.trackSelectionReason, mediaLoadData.mediaStartTimeMs, mediaLoadData.mediaEndTimeMs, loadEventInfo.elapsedRealtimeMs, loadEventInfo.loadDurationMs, loadEventInfo.bytesLoaded, null, null);
    }

    @Override
    public void onLoadCanceled(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadCanceled", loadEventInfo.dataSpec, mediaLoadData.dataType, mediaLoadData.trackType, mediaLoadData.trackFormat, mediaLoadData.trackSelectionReason, mediaLoadData.mediaStartTimeMs, mediaLoadData.mediaEndTimeMs, loadEventInfo.elapsedRealtimeMs, loadEventInfo.loadDurationMs, loadEventInfo.bytesLoaded, null, null);
    }

    @Override
    public void onLoadError(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        logLoadingEvent("LoadError", loadEventInfo.dataSpec, mediaLoadData.dataType, mediaLoadData.trackType, mediaLoadData.trackFormat, mediaLoadData.trackSelectionReason, mediaLoadData.mediaStartTimeMs, mediaLoadData.mediaEndTimeMs, loadEventInfo.elapsedRealtimeMs, loadEventInfo.loadDurationMs, loadEventInfo.bytesLoaded, error, wasCanceled);
    }

    @Override
    public void onDownstreamFormatChanged(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        String trackTypeString = trackTypeString(mediaLoadData.trackType);
        if (trackTypeString == null) {
            return;
        }

        log("DownstreamFormatChanged", "trackType=" + trackTypeString, trackFormatString(mediaLoadData.trackFormat), "reason=" + trackSelectionReasonString(mediaLoadData.trackSelectionReason));
    }

    @Override
    public void onUpstreamDiscarded(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        String trackTypeString = trackTypeString(mediaLoadData.trackType);
        if (trackTypeString == null) {
            return;
        }
        log("UpstreamDiscarded", "trackType=" + trackTypeString, "range=" + mediaLoadData.mediaStartTimeMs + ".." + mediaLoadData.mediaEndTimeMs);
    }

    @Override
    public void onMediaPeriodCreated(EventTime eventTime) {

    }

    @Override
    public void onMediaPeriodReleased(EventTime eventTime) {

    }

    @Override
    public void onReadingStarted(EventTime eventTime) {

    }

    @Override
    public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
        log("BandwidthEstimate", "bitrateEstimate=" + bitrateEstimate);
    }

    @Override
    public void onViewportSizeChange(EventTime eventTime, int width, int height) {
        log("ViewportSizeChange", "width=" + width, "height=" + height);
    }

    @Override
    public void onNetworkTypeChanged(EventTime eventTime, @Nullable NetworkInfo networkInfo) {

    }

    @Override
    public void onMetadata(EventTime eventTime, Metadata metadata) {

    }

    @Override
    public void onDecoderEnabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {

    }

    @Override
    public void onDecoderInitialized(EventTime eventTime, int trackType, String decoderName, long initializationDurationMs) {
        log("DecoderInitialized", "name=" + decoderName, "duration=" + initializationDurationMs);
    }

    @Override
    public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {
        log("DecoderInputFormatChanged", "id=" + format.id, "codecs=" + format.codecs, "bitrate=" + format.bitrate);
    }

    @Override
    public void onDecoderDisabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {

    }

    @Override
    public void onAudioSessionId(EventTime eventTime, int audioSessionId) {

    }

    @Override
    public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

    }

    @Override
    public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
        log("DroppedFrames", "count=" + droppedFrames, "time=" + elapsedMs);
    }

    @Override
    public void onVideoSizeChanged(EventTime eventTime, int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        log("VideoSizeChanged", "size=" + width + "x" + height);
    }

    @Override
    public void onRenderedFirstFrame(EventTime eventTime, Surface surface) {
        log("RenderedFirstFrame");
    }

    @Override
    public void onDrmKeysLoaded(EventTime eventTime) {

    }

    @Override
    public void onDrmSessionManagerError(EventTime eventTime, Exception error) {

    }

    @Override
    public void onDrmKeysRestored(EventTime eventTime) {

    }

    @Override
    public void onDrmKeysRemoved(EventTime eventTime) {

    }
}
