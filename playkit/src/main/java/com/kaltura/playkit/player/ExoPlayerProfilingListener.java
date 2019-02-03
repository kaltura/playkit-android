package com.kaltura.playkit.player;

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
import java.util.LinkedHashSet;

import static com.google.android.exoplayer2.C.DATA_TYPE_AD;
import static com.google.android.exoplayer2.C.DATA_TYPE_DRM;
import static com.google.android.exoplayer2.C.DATA_TYPE_MANIFEST;
import static com.google.android.exoplayer2.C.DATA_TYPE_MEDIA;
import static com.google.android.exoplayer2.C.DATA_TYPE_MEDIA_INITIALIZATION;
import static com.google.android.exoplayer2.C.DATA_TYPE_TIME_SYNCHRONIZATION;
import static com.google.android.exoplayer2.C.DATA_TYPE_UNKNOWN;
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
import static com.kaltura.playkit.player.DefaultProfiler.field;
import static com.kaltura.playkit.player.Profiler.pkLog;
import static com.kaltura.playkit.player.DefaultProfiler.timeField;

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

    private String trackFormatString(@Nullable Format trackFormat) {
        if (trackFormat == null) {
            return null;
        }
        return DefaultProfiler.nullable("id", trackFormat.id) + DefaultProfiler.SEPARATOR + "bitrate=" + trackFormat.bitrate + DefaultProfiler.SEPARATOR +
                DefaultProfiler.nullable("codecs", trackFormat.codecs) + DefaultProfiler.SEPARATOR + DefaultProfiler.nullable("language", trackFormat.language);
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
            case DATA_TYPE_UNKNOWN: return "Unknown";
            case DATA_TYPE_MEDIA: return "Media";
            case DATA_TYPE_MEDIA_INITIALIZATION: return "MediaInit";
            case DATA_TYPE_MANIFEST: return "Manifest";
            case DATA_TYPE_DRM: return "DRM";
            case DATA_TYPE_TIME_SYNCHRONIZATION: return "TimeSync";
            case DATA_TYPE_AD: return "Ad";
            default: return null;
        }
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

        log("PlayerStateChanged", field("state", state), field("shouldPlay", playWhenReady));
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

        log("PositionDiscontinuity", field("reason", reasonString));
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
        log("PlaybackParametersChanged", field("speed", playbackParameters.speed), field("pitch", playbackParameters.pitch));
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
        log("RepeatModeChanged", field("repeatMode", strMode));
    }

    @Override
    public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {
        log("ShuffleModeChanged", field("shuffleModeEnabled", shuffleModeEnabled));
    }

    @Override
    public void onLoadingChanged(EventTime eventTime, boolean isLoading) {
        log("LoadingChanged", field("isLoading", isLoading));
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

        log("PlayerError", field("type", type), "cause={" + error.getCause() + "}");
    }

    @Override
    public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        LinkedHashSet<TrackGroup> trackGroupSet = new LinkedHashSet<>(trackGroups.length);
        for (int i = 0; i < trackSelections.length; i++) {
            final TrackSelection trackSelection = trackSelections.get(i);
            if (trackSelection != null) {
                trackGroupSet.add(trackSelection.getTrackGroup());
            }
        }

        // Add the rest
        for (int i = 0; i < trackGroups.length; i++) {
            final TrackGroup trackGroup = trackGroups.get(i);
            trackGroupSet.add(trackGroup);
        }

        JsonArray jTrackGroups = new JsonArray(trackGroups.length);
        for (TrackGroup trackGroup : trackGroupSet) {
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
                field("available", jTrackGroups.toString()),
                field("selected", jTrackSelections.toString()));
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
        jsonObject.addProperty("mimeType", format.sampleMimeType);

        return jsonObject;
    }

    private void logLoadingEvent(String event, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error, Boolean wasCanceled) {
        String dataTypeString = dataTypeString(mediaLoadData.dataType);
        String trackTypeString = trackTypeString(mediaLoadData.trackType);

        if (dataTypeString == null) {
            return;
        }

        if (loadEventInfo.dataSpec.uri.getPath().contains("widevine")) {
            pkLog.d(loadEventInfo.dataSpec.uri.toString());
        }

        log(event,
                timeField("time", loadEventInfo.elapsedRealtimeMs - profiler.startTime), field("uri", loadEventInfo.dataSpec.uri.toString()),
                field("dataType", dataTypeString), field("trackType", trackTypeString),
                trackFormatString(mediaLoadData.trackFormat), field("reason", trackSelectionReasonString(mediaLoadData.trackSelectionReason)),
                timeField("rangeStart", mediaLoadData.mediaStartTimeMs), timeField("rangeEnd", mediaLoadData.mediaEndTimeMs),
                timeField("loadTime", loadEventInfo.loadDurationMs), field("bytes", loadEventInfo.bytesLoaded),
                field("error", error != null ? "{" + error.getMessage() + "}" : null), wasCanceled == null ? null : field("canceled", wasCanceled));
    }

    @Override
    public void onLoadStarted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadStarted", loadEventInfo, mediaLoadData, null, null);
        profiler.maybeLogServerInfo(loadEventInfo.uri);
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

        log("DownstreamFormatChanged", field("trackType", trackTypeString), trackFormatString(mediaLoadData.trackFormat), field("reason", trackSelectionReasonString(mediaLoadData.trackSelectionReason)));
    }

    @Override
    public void onUpstreamDiscarded(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        String trackTypeString = trackTypeString(mediaLoadData.trackType);
        if (trackTypeString == null) {
            return;
        }
        log("UpstreamDiscarded", field("trackType", trackTypeString), field("start", mediaLoadData.mediaStartTimeMs / 1000f), field("end", mediaLoadData.mediaEndTimeMs / 1000f));
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
        log("BandwidthSample",
                field("bandwidth", bitrateEstimate),
                timeField("totalLoadTime", totalLoadTimeMs),
                field("totalBytesLoaded", totalBytesLoaded)
        );
    }

    @Override
    public void onMetadata(EventTime eventTime, Metadata metadata) {

    }

    @Override
    public void onDecoderEnabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {

    }

    @Override
    public void onDecoderInitialized(EventTime eventTime, int trackType, String decoderName, long initializationDurationMs) {
        log("DecoderInitialized", field("name", decoderName), field("duration", initializationDurationMs / 1000f));
    }

    @Override
    public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {
        log("DecoderInputFormatChanged", field("id", format.id), field("codecs", format.codecs), field("bitrate", format.bitrate));
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
        log("DroppedFrames", field("count", droppedFrames), field("time", elapsedMs / 1000f));
    }

    @Override
    public void onVideoSizeChanged(EventTime eventTime, int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        log("VideoSizeChanged", field("width", width), field("height", height));
    }

    @Override
    public void onRenderedFirstFrame(EventTime eventTime, Surface surface) {
        log("RenderedFirstFrame");
    }

    @Override
    public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {
        log("ViewportSizeChange", field("width", width), field("height", height));
    }

    @Override
    public void onVolumeChanged(EventTime eventTime, float volume) {
        log("VolumeChanged", field("volume", volume));
    }

    @Override
    public void onDrmSessionAcquired(EventTime eventTime) {
        log("DrmSessionAcquired");
    }

    @Override
    public void onDrmSessionReleased(EventTime eventTime) {
        log("DrmSessionReleased");
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
