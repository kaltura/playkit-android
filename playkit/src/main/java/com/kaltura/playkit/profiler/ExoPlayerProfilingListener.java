package com.kaltura.playkit.profiler;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.PlaybackException;
import com.kaltura.android.exoplayer2.PlaybackParameters;
import com.kaltura.android.exoplayer2.Player;
import com.kaltura.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.android.exoplayer2.decoder.DecoderCounters;
import com.kaltura.android.exoplayer2.decoder.DecoderReuseEvaluation;
import com.kaltura.android.exoplayer2.drm.DrmSession;
import com.kaltura.android.exoplayer2.metadata.Metadata;
import com.kaltura.android.exoplayer2.source.LoadEventInfo;
import com.kaltura.android.exoplayer2.source.MediaLoadData;
import com.kaltura.android.exoplayer2.source.TrackGroup;
import com.kaltura.android.exoplayer2.source.TrackGroupArray;
import com.kaltura.android.exoplayer2.trackselection.ExoTrackSelection;
import com.kaltura.android.exoplayer2.trackselection.TrackSelection;
import com.kaltura.android.exoplayer2.trackselection.TrackSelectionArray;
import com.kaltura.android.exoplayer2.video.VideoSize;
import com.kaltura.playkit.PKPlaybackException;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.player.PKPlayerErrorType;
import com.kaltura.playkit.player.Profiler.Event;

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
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_AUTO_TRANSITION;
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_INTERNAL;
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_REMOVE;
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK;
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT;
import static com.kaltura.android.exoplayer2.Player.DISCONTINUITY_REASON_SKIP;
import static com.kaltura.playkit.profiler.PlayKitProfiler.MSEC_MULTIPLIER_FLOAT;

class ExoPlayerProfilingListener implements AnalyticsListener {

    private boolean shouldPlay;

    @NonNull
    private final PlayKitProfiler profiler;

    ExoPlayerProfilingListener(@NonNull PlayKitProfiler profiler) {
        this.profiler = profiler;
    }

    private Event log(String event) {
        return profiler.logWithPlaybackInfo(event);
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

    private JsonObject trackFormatMap(@Nullable Format trackFormat) {
        if (trackFormat == null) {
            return null;
        }

        return new Utils.GsonObject()
                .add("id", trackFormat.id)
                .add("bitrate", trackFormat.bitrate)
                .add("codecs", trackFormat.codecs)
                .add("language", trackFormat.language)
                .jsonObject();
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
    public void onIsPlayingChanged(@NonNull EventTime eventTime, boolean isPlaying) {
        profiler.logWithPlaybackInfo("IsPlayingChanged").add("isPlaying", isPlaying).end();
    }

    @Override
    public void onPlaybackStateChanged(@NonNull EventTime eventTime, int playbackState) {
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

        log("PlayerStateChanged").add("state", state).add("shouldPlay", shouldPlay).end();
    }

    @Override
    public void onPlayWhenReadyChanged(@NonNull EventTime eventTime, boolean playWhenReady, int reason) {
        shouldPlay = playWhenReady;
    }

    @Override
    public void onTimelineChanged(@NonNull EventTime eventTime, int reason) {

    }

    @Override
    public void onPositionDiscontinuity(@NonNull EventTime eventTime,
                                        @NonNull Player.PositionInfo oldPosition,
                                        @NonNull Player.PositionInfo newPosition,
                                        int reason) {
        String reasonString;
        switch (reason) {
            case DISCONTINUITY_REASON_AUTO_TRANSITION:
                reasonString = "AutoTransition";
                break;
            case DISCONTINUITY_REASON_SEEK:
                log("SeekProcessed").end();
                reasonString = "Seek";
                break;
            case DISCONTINUITY_REASON_SEEK_ADJUSTMENT:
                reasonString = "SeekAdjustment";
                break;
            case DISCONTINUITY_REASON_SKIP:
                reasonString = "Skip";
                break;
            case DISCONTINUITY_REASON_REMOVE:
                reasonString = "Remove";
                break;
            case DISCONTINUITY_REASON_INTERNAL:
                reasonString = "Internal";
                break;
            default:
                reasonString = "Unknown:" + reason;
        }

        log("PositionDiscontinuity")
                .add("reason", reasonString)
                .end();
    }

    @Override
    public void onPlaybackParametersChanged(@NonNull EventTime eventTime, PlaybackParameters playbackParameters) {
        log("PlaybackParametersChanged")
                .add("speed", playbackParameters.speed)
                .add("pitch", playbackParameters.pitch)
                .end();
    }

    @Override
    public void onRepeatModeChanged(@NonNull EventTime eventTime, int repeatMode) {
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
        log("RepeatModeChanged")
                .add("repeatMode", strMode)
                .end();
    }

    @Override
    public void onShuffleModeChanged(@NonNull EventTime eventTime, boolean shuffleModeEnabled) {
        log("ShuffleModeChanged")
                .add("shuffleModeEnabled", shuffleModeEnabled)
                .end();
    }

    @Override
    public void onIsLoadingChanged(@NonNull EventTime eventTime, boolean isLoading) {
        log("LoadingChanged")
                .add("isLoading", isLoading)
                .end();
    }

    @Override
    public void onAudioCodecError(@NonNull EventTime eventTime, Exception audioCodecError) {
        log("PlayerError")
                .add("type", "audioCodecError")
                .add("cause", audioCodecError.getCause())
                .end();
    }

    @Override
    public void onAudioSinkError(@NonNull EventTime eventTime, Exception audioSinkError) {
        log("PlayerError")
                .add("type", "audioSinkError")
                .add("cause", audioSinkError.getCause())
                .end();
    }

    @Override
    public void onVideoCodecError(@NonNull EventTime eventTime, Exception videoCodecError) {
        log("PlayerError")
                .add("type", "videoCodecError")
                .add("cause", videoCodecError.getCause())
                .end();
    }

    @Override
    public void onPlayerError(@NonNull EventTime eventTime, @NonNull PlaybackException playbackException) {
        Pair<PKPlayerErrorType, String> exceptionPair = PKPlaybackException.getPlaybackExceptionType(playbackException);
        String type = exceptionPair.first.toString();
        log("PlayerError")
                .add("type", type)
                .add("cause", playbackException.getCause())
                .end();
    }

    @Override
    public void onTracksChanged(@NonNull EventTime eventTime, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
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

        log("TracksChanged")
                .add("available", jTrackGroups)
                .add("selected", jTrackSelections)
                .end();
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

    private void logLoadingEvent(String event, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, @Nullable IOException error, @Nullable Boolean wasCanceled) {        String dataTypeString = dataTypeString(mediaLoadData.dataType);
        String trackTypeString = trackTypeString(mediaLoadData.trackType);

        if (dataTypeString == null) {
            return;
        }

        final Utils.GsonObject e = log(event)
                .addTime("time", loadEventInfo.elapsedRealtimeMs - profiler.sessionStartTime)
                .add("uri", loadEventInfo.dataSpec.uri.toString())
                .add("dataType", dataTypeString)
                .add("trackType", trackTypeString)
                .addAll(trackFormatMap(mediaLoadData.trackFormat))
                .add("reason", trackSelectionReasonString(mediaLoadData.trackSelectionReason))
                .addTime("rangeStart", mediaLoadData.mediaStartTimeMs)
                .addTime("rangeEnd", mediaLoadData.mediaEndTimeMs)
                .addTime("loadTime", loadEventInfo.loadDurationMs)
                .add("bytes", loadEventInfo.bytesLoaded)
                .add("error", error != null ? error.getMessage() : null);

        if (wasCanceled != null) {
            e.add("canceled", wasCanceled);
        }
        e.end();
    }

    @Override
    public void onLoadStarted(@NonNull EventTime eventTime, LoadEventInfo loadEventInfo, @NonNull MediaLoadData mediaLoadData) {
        profiler.maybeLogServerInfo(loadEventInfo.uri);
    }

    @Override
    public void onLoadCompleted(@NonNull EventTime eventTime, @NonNull LoadEventInfo loadEventInfo, @NonNull MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadCompleted", loadEventInfo, mediaLoadData, null, null);
    }

    @Override
    public void onLoadCanceled(@NonNull EventTime eventTime, @NonNull LoadEventInfo loadEventInfo, @NonNull MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadCanceled", loadEventInfo, mediaLoadData, null, null);
    }

    @Override
    public void onLoadError(@NonNull EventTime eventTime, @NonNull LoadEventInfo loadEventInfo, @NonNull MediaLoadData mediaLoadData, @NonNull IOException error, boolean wasCanceled) {
        logLoadingEvent("LoadError", loadEventInfo, mediaLoadData, error, wasCanceled);
    }

    @Override
    public void onDownstreamFormatChanged(@NonNull EventTime eventTime, MediaLoadData mediaLoadData) {
        String trackTypeString = trackTypeString(mediaLoadData.trackType);
        if (trackTypeString == null) {
            return;
        }

        log("DownstreamFormatChanged")
                .add("trackType", trackTypeString)
                .addAll(trackFormatMap(mediaLoadData.trackFormat))
                .add("reason", trackSelectionReasonString(mediaLoadData.trackSelectionReason))
                .end();
    }

    @Override
    public void onUpstreamDiscarded(@NonNull EventTime eventTime, MediaLoadData mediaLoadData) {
        String trackTypeString = trackTypeString(mediaLoadData.trackType);
        if (trackTypeString == null) {
            return;
        }
        log("UpstreamDiscarded")
                .add("trackType", trackTypeString)
                .add("start", mediaLoadData.mediaStartTimeMs / MSEC_MULTIPLIER_FLOAT)
                .add("end", mediaLoadData.mediaEndTimeMs / MSEC_MULTIPLIER_FLOAT)
                .end();
    }

    @Override
    public void onBandwidthEstimate(@NonNull EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
        log("BandwidthSample")
                .add("bandwidth", bitrateEstimate)
                .addTime("totalLoadTime", totalLoadTimeMs)
                .add("totalBytesLoaded", totalBytesLoaded)
                .end();
    }

    @Override
    public void onMetadata(@NonNull EventTime eventTime, @NonNull Metadata metadata) {

    }

    @Override
    public void onAudioEnabled(@NonNull EventTime eventTime, @NonNull DecoderCounters counters) {

    }

    @Override
    public void onVideoEnabled(@NonNull EventTime eventTime, @NonNull DecoderCounters counters) {

    }

    @Override
    public void onVideoDecoderInitialized(@NonNull EventTime eventTime, @NonNull String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        log("DecoderInitialized")
                .add("name", decoderName)
                .add("duration", initializationDurationMs / MSEC_MULTIPLIER_FLOAT)
                .end();
    }

    @Override
    public void onVideoInputFormatChanged(@NonNull EventTime eventTime, Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
        log("DecoderInputFormatChanged")
                .add("id", format.id)
                .add("codecs", format.codecs)
                .add("bitrate", format.bitrate)
                .end();
    }

    @Override
    public void onVideoDisabled(@NonNull EventTime eventTime, @NonNull DecoderCounters decoderCounters) {

    }

    @Override
    public void onAudioUnderrun(@NonNull EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

    }

    @Override
    public void onDroppedVideoFrames(@NonNull EventTime eventTime, int droppedFrames, long elapsedMs) {
        log("DroppedFrames")
                .add("count", droppedFrames)
                .add("time", elapsedMs / MSEC_MULTIPLIER_FLOAT)
                .end();
    }

    @Override
    public void onVideoSizeChanged(@NonNull EventTime eventTime, @NonNull VideoSize videoSize) {
        log("VideoSizeChanged")
                .add("width", videoSize.width)
                .add("height", videoSize.height)
                .end();
    }

    @Override
    public void onRenderedFirstFrame(@NonNull EventTime eventTime, @NonNull Object output, long renderTimeMs) {
        log("RenderedFirstFrame")
                .add("renderTimeMs", renderTimeMs)
                .end();
    }

    @Override
    public void onSurfaceSizeChanged(@NonNull EventTime eventTime, int width, int height) {
        log("ViewportSizeChange")
                .add("width", width)
                .add("height", height)
                .end();
    }

    @Override
    public void onVolumeChanged(@NonNull EventTime eventTime, float volume) {
        log("VolumeChanged")
                .add("volume", volume)
                .end();
    }

    @Override
    public void onDrmSessionAcquired(@NonNull EventTime eventTime, @DrmSession.State int state) {
        log("DrmSessionAcquired").end();
    }

    @Override
    public void onDrmSessionReleased(@NonNull EventTime eventTime) {
        log("DrmSessionReleased").end();
    }

    @Override
    public void onDrmKeysLoaded(@NonNull EventTime eventTime) {

    }

    @Override
    public void onDrmSessionManagerError(@NonNull EventTime eventTime, @NonNull Exception error) {

    }

    @Override
    public void onDrmKeysRestored(@NonNull EventTime eventTime) {

    }

    @Override
    public void onDrmKeysRemoved(@NonNull EventTime eventTime) {

    }
}
