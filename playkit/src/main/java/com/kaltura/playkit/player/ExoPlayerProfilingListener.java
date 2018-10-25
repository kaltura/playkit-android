package com.kaltura.playkit.player;

import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.lang.ref.WeakReference;

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

    @NonNull private final DefaultProfiler profiler;
    @NonNull private final WeakReference<PlayerEngine> playerEngine;

    ExoPlayerProfilingListener(@NonNull DefaultProfiler profiler, PlayerEngine playerEngine) {
        this.profiler = profiler;
        this.playerEngine = new WeakReference<>(playerEngine);
    }

    public void log(String event, String... strings) {
        final PlayerEngine pe = this.playerEngine.get();
        if (pe != null) {
            profiler.logWithPlaybackInfo(event, pe, strings);
        }
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

    private static String nullable(String name, String value) {
        if (value == null) {
            return name + "=null";
        }

        return DefaultProfiler.field(name, value);
    }

    private String trackFormatString(@Nullable Format trackFormat) {
        if (trackFormat == null) {
            return null;
        }
        return nullable("id", trackFormat.id) + DefaultProfiler.SEPARATOR + "bitrate=" + trackFormat.bitrate + DefaultProfiler.SEPARATOR +
                nullable("codecs", trackFormat.codecs) + DefaultProfiler.SEPARATOR + nullable("language", trackFormat.language);
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

    private static Float msecToSec(Long ms) {
        return ms == null ? null : ms / 1000f;
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

        log("PlayerStateChanged", DefaultProfiler.field("state", state), DefaultProfiler.field("shouldPlay", playWhenReady));
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

        log("PositionDiscontinuity", DefaultProfiler.field("reason", reasonString));
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
        log("PlaybackParametersChanged", DefaultProfiler.field("speed", playbackParameters.speed), DefaultProfiler.field("pitch", playbackParameters.pitch));
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
        log("RepeatModeChanged", DefaultProfiler.field("repeatMode", strMode));
    }

    @Override
    public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {
        log("ShuffleModeChanged", DefaultProfiler.field("shuffleModeEnabled", shuffleModeEnabled));
    }

    @Override
    public void onLoadingChanged(EventTime eventTime, boolean isLoading) {
        log("LoadingChanged", DefaultProfiler.field("isLoading", isLoading));
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

        log("PlayerError", DefaultProfiler.field("type", type), "cause={" + error.getCause() + "}");
    }

    @Override
    public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        JsonArray jTrackGroups = new JsonArray(trackGroups.length);
        for (int i = 0; i < trackGroups.length; i++) {
            final TrackGroup trackGroup = trackGroups.get(i);
            JsonArray jTrackGroup = new JsonArray(trackGroup.length);
            for (int j = 0; j < trackGroup.length; j++) {
                final Format format = trackGroup.getFormat(j);
                jTrackGroup.add(toJSON(format));
            }
            jTrackGroups.add(jTrackGroup);
        }

        JsonArray jTrackSelections = new JsonArray(trackSelections.length);
        for (int i = 0; i < trackSelections.length; i++) {
            final TrackSelection trackSelection = trackSelections.get(i);
            final Format selectedFormat = trackSelection == null ? null : trackSelection.getSelectedFormat();
            jTrackSelections.add(toJSON(selectedFormat));
        }

        log("TracksChanged",
                DefaultProfiler.field("available", jTrackGroups.toString()),
                DefaultProfiler.field("selected", jTrackSelections.toString()));
    }

    private JsonObject toJSON(@Nullable Format format) {

        if (format == null) {
            return null;
        }

        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("id", format.id);
        jsonObject.addProperty("bitrate", format.bitrate);
        jsonObject.addProperty("codecs", format.codecs);
        jsonObject.addProperty("language", format.language);
        jsonObject.addProperty("height", format.height);
        jsonObject.addProperty("width", format.width);
        jsonObject.addProperty("containerMimeType", format.containerMimeType);
        jsonObject.addProperty("sampleMimeType", format.sampleMimeType);

        return jsonObject;
    }

    private void logLoadingEvent(String event, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error, Boolean wasCanceled) {
        String dataTypeString = dataTypeString(mediaLoadData.dataType);
        String trackTypeString = trackTypeString(mediaLoadData.trackType);

        if (dataTypeString == null || trackTypeString == null) {
            return;
        }

        log(event,
                DefaultProfiler.timeField("time", loadEventInfo.elapsedRealtimeMs - profiler.startTime), DefaultProfiler.field("uri", loadEventInfo.dataSpec.uri.getLastPathSegment()),
                DefaultProfiler.field("dataType", dataTypeString), DefaultProfiler.field("trackType", trackTypeString),
                trackFormatString(mediaLoadData.trackFormat), DefaultProfiler.field("reason", trackSelectionReasonString(mediaLoadData.trackSelectionReason)),
                DefaultProfiler.timeField("rangeStart", mediaLoadData.mediaStartTimeMs), DefaultProfiler.timeField("rangeEnd", mediaLoadData.mediaEndTimeMs),
                DefaultProfiler.timeField("loadTime", loadEventInfo.loadDurationMs), DefaultProfiler.field("bytes", loadEventInfo.bytesLoaded),
                DefaultProfiler.field("error", error != null ? "{" + error.getMessage() + "}" : null), wasCanceled == null ? null : DefaultProfiler.field("canceled", wasCanceled));
    }

    @Override
    public void onLoadStarted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadStarted", loadEventInfo, mediaLoadData, null, null);
    }

    @Override
    public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadCompleted", loadEventInfo, mediaLoadData, null, null);
    }

    @Override
    public void onLoadCanceled(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadCanceled", loadEventInfo, mediaLoadData, null, null);
    }

    @Override
    public void onLoadError(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        logLoadingEvent("LoadError", loadEventInfo, mediaLoadData, error, wasCanceled);
    }

    @Override
    public void onDownstreamFormatChanged(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        String trackTypeString = trackTypeString(mediaLoadData.trackType);
        if (trackTypeString == null) {
            return;
        }

        log("DownstreamFormatChanged", DefaultProfiler.field("trackType", trackTypeString), trackFormatString(mediaLoadData.trackFormat), DefaultProfiler.field("reason", trackSelectionReasonString(mediaLoadData.trackSelectionReason)));
    }

    @Override
    public void onUpstreamDiscarded(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        String trackTypeString = trackTypeString(mediaLoadData.trackType);
        if (trackTypeString == null) {
            return;
        }
        log("UpstreamDiscarded", DefaultProfiler.field("trackType", trackTypeString), DefaultProfiler.field("start", mediaLoadData.mediaStartTimeMs / 1000f), DefaultProfiler.field("end", mediaLoadData.mediaEndTimeMs / 1000f));
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
        log("BandwidthEstimate", DefaultProfiler.field("bitrateEstimate", bitrateEstimate));
    }

    @Override
    public void onViewportSizeChange(EventTime eventTime, int width, int height) {
        log("ViewportSizeChange", DefaultProfiler.field("width", width), DefaultProfiler.field("height", height));
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
        log("DecoderInitialized", DefaultProfiler.field("name", decoderName), DefaultProfiler.field("duration", initializationDurationMs / 1000f));
    }

    @Override
    public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {
        log("DecoderInputFormatChanged", DefaultProfiler.field("id", format.id), DefaultProfiler.field("codecs", format.codecs), DefaultProfiler.field("bitrate", format.bitrate));
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
        log("DroppedFrames", DefaultProfiler.field("count", droppedFrames), DefaultProfiler.field("time", elapsedMs / 1000f));
    }

    @Override
    public void onVideoSizeChanged(EventTime eventTime, int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        log("VideoSizeChanged", DefaultProfiler.field("width", width), DefaultProfiler.field("height", height));
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
