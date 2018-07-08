package com.kaltura.playkit.player;

import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
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

class ExoPlayerProfilingListener implements MediaSourceEventListener, Player.EventListener, VideoRendererEventListener {

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

    private String trackFormatString(Format trackFormat) {
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

    @Override
    public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                              int trackSelectionReason, Object trackSelectionData,
                              long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {

        logLoadingEvent("LoadStarted", dataSpec, dataType, trackType, trackFormat, trackSelectionReason, mediaStartTimeMs, mediaEndTimeMs, elapsedRealtimeMs, null, null, null, null);
    }

    @Override
    public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                                int trackSelectionReason, Object trackSelectionData,
                                long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
                                long loadDurationMs, long bytesLoaded) {

        logLoadingEvent("LoadCompleted", dataSpec, dataType, trackType, trackFormat, trackSelectionReason, mediaStartTimeMs, mediaEndTimeMs, elapsedRealtimeMs, loadDurationMs, bytesLoaded, null, null);
    }

    @Override
    public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                               int trackSelectionReason, Object trackSelectionData,
                               long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
                               long loadDurationMs, long bytesLoaded) {

        logLoadingEvent("LoadCanceled", dataSpec, dataType, trackType, trackFormat, trackSelectionReason, mediaStartTimeMs, mediaEndTimeMs, elapsedRealtimeMs, loadDurationMs, bytesLoaded, null, null);
    }

    @Override
    public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                            int trackSelectionReason, Object trackSelectionData,
                            long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
                            long loadDurationMs, long bytesLoaded,
                            IOException error, boolean wasCanceled) {

        logLoadingEvent("LoadError", dataSpec, dataType, trackType, trackFormat, trackSelectionReason, mediaStartTimeMs, mediaEndTimeMs, elapsedRealtimeMs, loadDurationMs, bytesLoaded, error, wasCanceled);
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
    public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {
        String trackTypeString = trackTypeString(trackType);
        if (trackTypeString == null) {
            return;
        }
        log("UpstreamDiscarded", "trackType=" + trackTypeString, "range=" + mediaStartTimeMs + ".." + mediaEndTimeMs);
    }

    @Override
    public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {
        String trackTypeString = trackTypeString(trackType);
        if (trackTypeString == null) {
            return;
        }

        log("DownstreamFormatChanged", "trackType=" + trackTypeString, trackFormatString(trackFormat), "reason=" + trackSelectionReasonString(trackSelectionReason), "mediaTime=" + mediaTimeMs);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
        // ???
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
//        for (int i = 0; i < trackSelections.length; i++) {
//            trackSelections.get(i).getSelectedFormat()
//
//        }
//        if (trackSelections.length >= 2) {  // video and audio
//            Format selectedVideoFormat = trackSelections.get(C.TRACK_TYPE_VIDEO).getSelectedFormat();
//            Format selectedAudioFormat = trackSelections.get(C.TRACK_TYPE_AUDIO).getSelectedFormat();
//            log("TracksChanged", trackFormatString(selectedVideoFormat), trackFormatString(selectedAudioFormat));
//        } else if (trackSelections.length == 1) {   // just video
//            Format selectedVideoFormat = trackSelections.get(C.TRACK_TYPE_VIDEO).getSelectedFormat();
//            log("TracksChanged", trackFormatString(selectedVideoFormat));
//        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // ???
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
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
    public void onRepeatModeChanged(int repeatMode) {}

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}

    @Override
    public void onPlayerError(ExoPlaybackException error) {
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
    public void onPositionDiscontinuity(int reason) {
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
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

    @Override
    public void onSeekProcessed() {
        log("SeekProcessed");
    }

    @Override
    public void onVideoEnabled(DecoderCounters counters) {}

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        log("VideoDecoderInitialized", "name=" + decoderName, "duration=" + initializationDurationMs);
    }

    @Override
    public void onVideoInputFormatChanged(Format format) {
        log("VideoInputFormatChanged", "id=" + format.id, "codecs=" + format.codecs, "bitrate=" + format.bitrate);
    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {
        log("DroppedFrames", "count=" + count, "time=" + elapsedMs);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        log("VideoSizeChanged", "size=" + width + "x" + height);
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
        log("RenderedFirstFrame");
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {}
}
