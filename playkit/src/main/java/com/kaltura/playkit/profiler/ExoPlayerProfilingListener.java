package com.kaltura.playkit.profiler;


import static com.kaltura.androidx.media3.common.C.DATA_TYPE_AD;
import static com.kaltura.androidx.media3.common.C.DATA_TYPE_DRM;
import static com.kaltura.androidx.media3.common.C.DATA_TYPE_MANIFEST;
import static com.kaltura.androidx.media3.common.C.DATA_TYPE_MEDIA;
import static com.kaltura.androidx.media3.common.C.DATA_TYPE_MEDIA_INITIALIZATION;
import static com.kaltura.androidx.media3.common.C.DATA_TYPE_TIME_SYNCHRONIZATION;
import static com.kaltura.androidx.media3.common.C.DATA_TYPE_UNKNOWN;
import static com.kaltura.androidx.media3.common.C.SELECTION_REASON_ADAPTIVE;
import static com.kaltura.androidx.media3.common.C.SELECTION_REASON_INITIAL;
import static com.kaltura.androidx.media3.common.C.SELECTION_REASON_MANUAL;
import static com.kaltura.androidx.media3.common.C.SELECTION_REASON_TRICK_PLAY;
import static com.kaltura.androidx.media3.common.C.SELECTION_REASON_UNKNOWN;
import static com.kaltura.androidx.media3.common.C.TRACK_TYPE_AUDIO;
import static com.kaltura.androidx.media3.common.C.TRACK_TYPE_DEFAULT;
import static com.kaltura.androidx.media3.common.C.TRACK_TYPE_TEXT;
import static com.kaltura.androidx.media3.common.C.TRACK_TYPE_VIDEO;
import static com.kaltura.androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION;
import static com.kaltura.androidx.media3.common.Player.DISCONTINUITY_REASON_INTERNAL;
import static com.kaltura.androidx.media3.common.Player.DISCONTINUITY_REASON_REMOVE;
import static com.kaltura.androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK;
import static com.kaltura.androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT;
import static com.kaltura.androidx.media3.common.Player.DISCONTINUITY_REASON_SKIP;
import static com.kaltura.playkit.profiler.PlayKitProfiler.MSEC_MULTIPLIER_FLOAT;
import static com.kaltura.playkit.profiler.PlayKitProfiler.field;
import static com.kaltura.playkit.profiler.PlayKitProfiler.joinFields;
import static com.kaltura.playkit.profiler.PlayKitProfiler.nullable;
import static com.kaltura.playkit.profiler.PlayKitProfiler.timeField;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.androidx.media3.common.Format;
import com.kaltura.androidx.media3.common.PlaybackException;
import com.kaltura.androidx.media3.common.PlaybackParameters;
import com.kaltura.androidx.media3.common.Player;
import com.kaltura.androidx.media3.common.Tracks;
import com.kaltura.androidx.media3.exoplayer.analytics.AnalyticsListener;
import com.kaltura.androidx.media3.exoplayer.DecoderCounters;
import com.kaltura.androidx.media3.exoplayer.DecoderReuseEvaluation;
import com.kaltura.androidx.media3.exoplayer.drm.DrmSession;
import com.kaltura.androidx.media3.common.Metadata;
import com.kaltura.androidx.media3.exoplayer.source.LoadEventInfo;
import com.kaltura.androidx.media3.exoplayer.source.MediaLoadData;
import com.kaltura.androidx.media3.common.TrackGroup;
import com.kaltura.androidx.media3.common.VideoSize;
import com.kaltura.playkit.PKPlaybackException;
import com.kaltura.playkit.player.PKPlayerErrorType;

import java.io.IOException;

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
    public void onIsPlayingChanged(@NonNull EventTime eventTime, boolean isPlaying) {
        log("IsPlayingChanged",
                field("isPlaying", isPlaying));
    }

    @Override
    public void onPlaybackStateChanged(@NonNull EventTime eventTime,  int playbackState) {
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
    public void onPlayWhenReadyChanged(@NonNull EventTime eventTime, boolean playWhenReady, int reason) {
        shouldPlay = playWhenReady;
    }

    @Override
    public void onTimelineChanged(@NonNull EventTime eventTime, int reason) {
        String timeLineChangeReason = "NONE";
        switch (reason) {
            case Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE:
                timeLineChangeReason = "SOURCE_UPDATE";
                break;
            case Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED:
                timeLineChangeReason = "PLAYLIST_CHANGED";
                break;
        }
        log("onTimelineChanged", field("reason", timeLineChangeReason));
    }

    @Override
    public void onPositionDiscontinuity(@NonNull EventTime eventTime, @NonNull Player.PositionInfo oldPosition,
                                        @NonNull Player.PositionInfo newPosition, int reason) {
        String reasonString;
        switch (reason) {
            case DISCONTINUITY_REASON_AUTO_TRANSITION:
                reasonString = "AutoTransition";
                break;
            case DISCONTINUITY_REASON_SEEK:
                log("SeekProcessed");
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

        log("PositionDiscontinuity", field("reason", reasonString));
    }

    @Override
    public void onPlaybackParametersChanged(@NonNull EventTime eventTime, PlaybackParameters playbackParameters) {
        log("PlaybackParametersChanged",
                field("speed", playbackParameters.speed),
                field("pitch", playbackParameters.pitch));
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
        log("RepeatModeChanged", field("repeatMode", strMode));
    }

    @Override
    public void onShuffleModeChanged(@NonNull EventTime eventTime, boolean shuffleModeEnabled) {
        log("ShuffleModeChanged", field("shuffleModeEnabled", shuffleModeEnabled));
    }

    @Override
    public void onAudioCodecError(@NonNull EventTime eventTime, Exception audioCodecError) {
        log("PlayerError", field("type", "audioCodecError"), "cause={" + audioCodecError.getCause() + "}");
    }

    @Override
    public void onAudioSinkError(@NonNull EventTime eventTime, Exception audioSinkError) {
        log("PlayerError", field("type", "audioSinkError"), "cause={" + audioSinkError.getCause() + "}");
    }

    @Override
    public void onVideoCodecError(@NonNull EventTime eventTime, Exception videoCodecError) {
        log("PlayerError", field("type", "videoCodecError"), "cause={" + videoCodecError.getCause() + "}");
    }

    @Override
    public void onPlayerError(@NonNull EventTime eventTime, @NonNull PlaybackException playbackException) {
        Pair<PKPlayerErrorType, String> exceptionPair = PKPlaybackException.getPlaybackExceptionType(playbackException);
        String type = exceptionPair.first.toString();
        log("PlayerError", field("type", type), "cause={" + playbackException.getCause() + "}");
    }

    @Override
    public void onTracksChanged(@NonNull EventTime eventTime, Tracks tracks) {
        // Tracks.Group contains video/audio/text track groups
        ImmutableList<Tracks.Group> trackGroups = tracks.getGroups();
        JsonArray jTrackGroups = new JsonArray(trackGroups.size());
        JsonArray jTrackSelections = new JsonArray();

        for (Tracks.Group groups : trackGroups) {
            JsonArray jTrackGroup = new JsonArray(groups.length);

            // Get track groups for each Video/Audio/Text
            final TrackGroup trackGroup = groups.getMediaTrackGroup();

            for (int j = 0; j < trackGroup.length; j++) {
                // Get format of each track group present individually in video/audio/text
                Format format = trackGroup.getFormat(j);
                jTrackGroup.add(toJSON(format));
                if (groups.isSelected() && groups.isTrackSelected(j)) {
                    jTrackSelections.add(toJSON(format));
                }
            }
            jTrackGroups.add(jTrackGroup);
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
    public void onLoadStarted(@NonNull EventTime eventTime, @NonNull LoadEventInfo loadEventInfo, @NonNull MediaLoadData mediaLoadData) {
        logLoadingEvent("LoadStarted", loadEventInfo, mediaLoadData, null, null);
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

        log("DownstreamFormatChanged", field("trackType", trackTypeString), trackFormatString(mediaLoadData.trackFormat), field("reason", trackSelectionReasonString(mediaLoadData.trackSelectionReason)));
    }

    @Override
    public void onUpstreamDiscarded(@NonNull EventTime eventTime, MediaLoadData mediaLoadData) {
        String trackTypeString = trackTypeString(mediaLoadData.trackType);
        if (trackTypeString == null) {
            return;
        }
        log("UpstreamDiscarded", field("trackType", trackTypeString), field("start", mediaLoadData.mediaStartTimeMs / MSEC_MULTIPLIER_FLOAT), field("end", mediaLoadData.mediaEndTimeMs / MSEC_MULTIPLIER_FLOAT));
    }

    @Override
    public void onBandwidthEstimate(@NonNull EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
        log("BandwidthSample",
                field("bandwidth", bitrateEstimate),
                timeField("totalLoadTime", totalLoadTimeMs),
                field("totalBytesLoaded", totalBytesLoaded)
        );
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
        log("DecoderInitialized", field("name", decoderName), field("duration", initializationDurationMs / MSEC_MULTIPLIER_FLOAT));
    }

    @Override
    public void onVideoInputFormatChanged(@NonNull EventTime eventTime, Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
        log("DecoderInputFormatChanged", field("id", format.id), field("codecs", format.codecs), field("bitrate", format.bitrate));
    }

    @Override
    public void onVideoDisabled(@NonNull EventTime eventTime, @NonNull DecoderCounters decoderCounters) {

    }

    @Override
    public void onAudioUnderrun(@NonNull EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

    }

    @Override
    public void onDroppedVideoFrames(@NonNull EventTime eventTime, int droppedFrames, long elapsedMs) {
        log("DroppedFrames", field("count", droppedFrames), field("time", elapsedMs / MSEC_MULTIPLIER_FLOAT));
    }

    @Override
    public void onVideoSizeChanged(@NonNull EventTime eventTime, @NonNull VideoSize videoSize) {
        log("VideoSizeChanged", field("width", videoSize.width), field("height", videoSize.height));
    }

    @Override
    public void onRenderedFirstFrame(@NonNull EventTime eventTime, @NonNull Object output, long renderTimeMs) {
        log("RenderedFirstFrame");
    }

    @Override
    public void onSurfaceSizeChanged(@NonNull EventTime eventTime, int width, int height) {
        log("ViewportSizeChange", field("width", width), field("height", height));
    }

    @Override
    public void onVolumeChanged(@NonNull EventTime eventTime, float volume) {
        log("VolumeChanged", field("volume", volume));
    }

    @Override
    public void onDrmSessionAcquired(@NonNull EventTime eventTime, @DrmSession.State int state) {
        log("DrmSessionAcquired");
    }

    @Override
    public void onDrmSessionReleased(@NonNull EventTime eventTime) {
        log("DrmSessionReleased");
    }

    @Override
    public void onDrmKeysLoaded(@NonNull EventTime eventTime) {
        log("onDrmKeysLoaded");
    }

    @Override
    public void onDrmSessionManagerError(@NonNull EventTime eventTime, @NonNull Exception error) {
        log("onDrmSessionManagerError " + field("error", error.getMessage()));
    }

    @Override
    public void onDrmKeysRestored(@NonNull EventTime eventTime) {
        log("onDrmKeysRestored");
    }

    @Override
    public void onDrmKeysRemoved(@NonNull EventTime eventTime) {
        log("onDrmKeysRemoved");
    }
}
