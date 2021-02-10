package com.kaltura.playkit.profiler;


import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.decoder.DecoderReuseEvaluation;
import com.kaltura.android.exoplayer2.source.LoadEventInfo;
import com.kaltura.android.exoplayer2.source.MediaLoadData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.android.exoplayer2.ExoPlaybackException;
import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.Player;
import com.kaltura.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.android.exoplayer2.decoder.DecoderCounters;
import com.kaltura.android.exoplayer2.metadata.Metadata;
import com.kaltura.android.exoplayer2.source.TrackGroup;
import com.kaltura.android.exoplayer2.source.TrackGroupArray;
import com.kaltura.android.exoplayer2.trackselection.ExoTrackSelection;
import com.kaltura.android.exoplayer2.trackselection.TrackSelection;
import com.kaltura.android.exoplayer2.trackselection.TrackSelectionArray;

import java.io.IOException;
import java.util.LinkedHashSet;

import static com.kaltura.android.exoplayer2.C.DATA_TYPE_AD;
import static com.kaltura.android.exoplayer2.C.DATA_TYPE_DRM;
import static com.kaltura.android.exoplayer2.C.DATA_TYPE_MANIFEST;
import static com.kaltura.android.exoplayer2.C.DATA_TYPE_MEDIA;
import static com.kaltura.android.exoplayer2.C.DATA_TYPE_MEDIA_INITIALIZATION;
import static com.kaltura.android.exoplayer2.C.DATA_TYPE_TIME_SYNCHRONIZATION;
import static com.kaltura.android.exoplayer2.C.DATA_TYPE_UNKNOWN;
import static com.kaltura.android.exoplayer2.C.SELECTION_REASON_ADAPTIVE;
import static com.kaltura.android.exoplayer2.C.SELECTION_REASON_INITIAL;
import static com.kaltura.android.exoplayer2.C.SELECTION_REASON_MANUAL;
import static com.kaltura.android.exoplayer2.C.SELECTION_REASON_TRICK_PLAY;
import static com.kaltura.android.exoplayer2.C.SELECTION_REASON_UNKNOWN;
import static com.kaltura.android.exoplayer2.C.TRACK_TYPE_AUDIO;
import static com.kaltura.android.exoplayer2.C.TRACK_TYPE_DEFAULT;
import static com.kaltura.android.exoplayer2.C.TRACK_TYPE_TEXT;
import static com.kaltura.android.exoplayer2.C.TRACK_TYPE_VIDEO;
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_AD_INSERTION;
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_INTERNAL;
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_PERIOD_TRANSITION;
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK;
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT;

import static com.kaltura.playkit.profiler.PlayKitProfiler.MSEC_MULTIPLIER_FLOAT;
import static com.kaltura.playkit.profiler.PlayKitProfiler.field;
import static com.kaltura.playkit.profiler.PlayKitProfiler.joinFields;
import static com.kaltura.playkit.profiler.PlayKitProfiler.nullable;
import static com.kaltura.playkit.profiler.PlayKitProfiler.timeField;

class ExoPlayerProfilingListener implements AnalyticsListener {

    private boolean shouldPlay;

    @NonNull
    private final PlayKitProfiler profiler;

    ExoPlayerProfilingListener(@NonNull PlayKitProfiler profiler) {
        this.profiler = profiler;
    }

    public void log(String event, String... strings) {
        profiler.logWithPlaybackInfo(event, strings);
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

        return joinFields(nullable("id", trackFormat.id), "bitrate=" + trackFormat.bitrate,
                nullable("codecs", trackFormat.codecs), nullable("language", trackFormat.language));
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
    public void onPlaybackStateChanged(EventTime eventTime,  int playbackState) {
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

        log("PlayerStateChanged", field("state", state), field("shouldPlay", shouldPlay));
    }

    @Override
    public void onPlayWhenReadyChanged(EventTime eventTime, boolean playWhenReady, int reason) {
        shouldPlay = playWhenReady;
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
                reasonString = "Seek";
                break;
            case DISCONTINUITY_REASON_SEEK_ADJUSTMENT:
                reasonString = "SeekAdjustment";
                break;
            case DISCONTINUITY_REASON_AD_INSERTION:
                reasonString = "AdInsertion";
                break;
            case DISCONTINUITY_REASON_INTERNAL:
                reasonString = "Internal";
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
            case ExoPlaybackException.TYPE_REMOTE:
                type = "remoteComponentError";
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
            ExoTrackSelection trackSelection = null;
            if (trackSelections.get(i) instanceof ExoTrackSelection) {
                trackSelection = (ExoTrackSelection) trackSelections.get(i);
            }
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

    private void logLoadingEvent(String event, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, Boolean wasCanceled) {
        String dataTypeString = dataTypeString(mediaLoadData.dataType);
        String trackTypeString = trackTypeString(mediaLoadData.trackType);

        if (dataTypeString == null) {
            return;
        }

        log(event,
                timeField("time", loadEventInfo.elapsedRealtimeMs - profiler.sessionStartTime), field("uri", loadEventInfo.dataSpec.uri.toString()),
                field("dataType", dataTypeString), field("trackType", trackTypeString),
                trackFormatString(mediaLoadData.trackFormat), field("reason", trackSelectionReasonString(mediaLoadData.trackSelectionReason)),
                timeField("rangeStart", mediaLoadData.mediaStartTimeMs), timeField("rangeEnd", mediaLoadData.mediaEndTimeMs),
                timeField("loadTime", loadEventInfo.loadDurationMs), field("bytes", loadEventInfo.bytesLoaded),
                field("error", error != null ? "{" + error.getMessage() + "}" : null), wasCanceled == null ? null : field("canceled", wasCanceled));
    }

    @Override
    public void onLoadStarted(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadStarted", loadEventInfo, mediaLoadData, null, null);
        profiler.maybeLogServerInfo(loadEventInfo.uri);
    }

    @Override
    public void onLoadCompleted(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadCompleted", loadEventInfo, mediaLoadData, null, null);
    }

    @Override
    public void onLoadCanceled(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadCanceled", loadEventInfo, mediaLoadData, null, null);
    }

    @Override
    public void onLoadError(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        logLoadingEvent("LoadError", loadEventInfo, mediaLoadData, error, wasCanceled);
    }

    @Override
    public void onDownstreamFormatChanged(EventTime eventTime, MediaLoadData mediaLoadData) {
        String trackTypeString = trackTypeString(mediaLoadData.trackType);
        if (trackTypeString == null) {
            return;
        }

        log("DownstreamFormatChanged", field("trackType", trackTypeString), trackFormatString(mediaLoadData.trackFormat), field("reason", trackSelectionReasonString(mediaLoadData.trackSelectionReason)));
    }

    @Override
    public void onUpstreamDiscarded(EventTime eventTime, MediaLoadData mediaLoadData) {
        String trackTypeString = trackTypeString(mediaLoadData.trackType);
        if (trackTypeString == null) {
            return;
        }
        log("UpstreamDiscarded", field("trackType", trackTypeString), field("start", mediaLoadData.mediaStartTimeMs / MSEC_MULTIPLIER_FLOAT), field("end", mediaLoadData.mediaEndTimeMs / MSEC_MULTIPLIER_FLOAT));
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
    public void onAudioEnabled(EventTime eventTime, DecoderCounters counters) {

    }

    @Override
    public void onVideoEnabled(EventTime eventTime, DecoderCounters counters) {

    }

    @Override
    public void onVideoDecoderInitialized(EventTime eventTime, String decoderName, long initializationDurationMs) {
        log("DecoderInitialized", field("name", decoderName), field("duration", initializationDurationMs / MSEC_MULTIPLIER_FLOAT));
    }

    @Override
    public void onVideoInputFormatChanged(EventTime eventTime, Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
        log("DecoderInputFormatChanged", field("id", format.id), field("codecs", format.codecs), field("bitrate", format.bitrate));
    }

    @Override
    public void onVideoDisabled(EventTime eventTime, DecoderCounters decoderCounters) {

    }

    @Override
    public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

    }

    @Override
    public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
        log("DroppedFrames", field("count", droppedFrames), field("time", elapsedMs / MSEC_MULTIPLIER_FLOAT));
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
