/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import androidx.annotation.NonNull;

import com.kaltura.android.exoplayer2.source.dash.manifest.EventStream;
import com.kaltura.playkit.player.AudioTrack;
import com.kaltura.playkit.player.ImageTrack;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.player.TextTrack;
import com.kaltura.playkit.player.VideoTrack;
import com.kaltura.playkit.player.metadata.PKMetadata;
import com.kaltura.playkit.player.metadata.URIConnectionAcquiredInfo;

import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PlayerEvent implements PKEvent {

    public static final Class<Error> error = Error.class;
    public static final Class<StateChanged> stateChanged = StateChanged.class;
    public static final Class<DurationChanged> durationChanged = DurationChanged.class;
    public static final Class<TracksAvailable> tracksAvailable = TracksAvailable.class;
    public static final Class<VolumeChanged> volumeChanged = VolumeChanged.class;
    public static final Class<PlaybackInfoUpdated> playbackInfoUpdated = PlaybackInfoUpdated.class;
    public static final Class<MetadataAvailable> metadataAvailable = MetadataAvailable.class;
    public static final Class<SourceSelected> sourceSelected = SourceSelected.class;
    public static final Class<PlayheadUpdated> playheadUpdated = PlayheadUpdated.class;
    public static final Class<Seeking> seeking = Seeking.class;
    public static final Class<VideoTrackChanged> videoTrackChanged = VideoTrackChanged.class;
    public static final Class<AudioTrackChanged> audioTrackChanged = AudioTrackChanged.class;
    public static final Class<TextTrackChanged> textTrackChanged = TextTrackChanged.class;
    public static final Class<EventStreamChanged> eventStreamAvailable = EventStreamChanged.class;
    public static final Class<ImageTrackChanged> imageTrackChanged = ImageTrackChanged.class;

    public static final Class<PlaybackRateChanged> playbackRateChanged = PlaybackRateChanged.class;
    public static final Class<SubtitlesStyleChanged> subtitlesStyleChanged = SubtitlesStyleChanged.class;
    public static final Class<VideoFramesDropped> videoFramesDropped = VideoFramesDropped.class;
    public static final Class<OutputBufferCountUpdate> outputBufferCountUpdate = OutputBufferCountUpdate.class;
    public static final Class<ConnectionAcquired> connectionAcquired = ConnectionAcquired.class;
    public static final Class<BytesLoaded> bytesLoaded = BytesLoaded.class;
    public static final Class<SurfaceAspectRationResizeModeChanged> surfaceAspectRationSizeModeChanged = SurfaceAspectRationResizeModeChanged.class;

    public static final PlayerEvent.Type canPlay = Type.CAN_PLAY;
    public static final PlayerEvent.Type ended = Type.ENDED;
    public static final PlayerEvent.Type loadedMetadata = Type.LOADED_METADATA;
    public static final PlayerEvent.Type pause = Type.PAUSE;
    public static final PlayerEvent.Type play = Type.PLAY;
    public static final PlayerEvent.Type playing = Type.PLAYING;
    public static final PlayerEvent.Type seeked = Type.SEEKED;
    public static final PlayerEvent.Type replay = Type.REPLAY;
    public static final PlayerEvent.Type stopped = Type.STOPPED;

    public final Type type;

    public PlayerEvent(Type type) {
        this.type = type;
    }

    public static class Generic extends PlayerEvent {
        public Generic(Type type) {
            super(type);
        }
    }

    public static class StateChanged extends PlayerEvent {
        public final PlayerState newState;
        public final PlayerState oldState;

        public StateChanged(PlayerState newState, PlayerState oldState) {
            super(Type.STATE_CHANGED);
            this.newState = newState;
            this.oldState = oldState;
        }
    }

    public static class DurationChanged extends PlayerEvent {

        public final long duration;

        public DurationChanged(long duration) {
            super(Type.DURATION_CHANGE);
            this.duration = duration;
        }
    }

    public static class TracksAvailable extends PlayerEvent {

        public final PKTracks tracksInfo;
        public final PKTracksAvailableStatus pkTracksAvailableStatus;

        public TracksAvailable(PKTracks tracksInfo, PKTracksAvailableStatus pkTracksAvailableStatus) {
            super(Type.TRACKS_AVAILABLE);
            this.tracksInfo = tracksInfo;
            this.pkTracksAvailableStatus = pkTracksAvailableStatus;
        }
    }

    public static class VolumeChanged extends PlayerEvent {

        public final float volume;

        public VolumeChanged(float volume) {
            super(Type.VOLUME_CHANGED);
            this.volume = volume;
        }
    }

    public static class PlaybackInfoUpdated extends PlayerEvent {

        public final PlaybackInfo playbackInfo;

        public PlaybackInfoUpdated(PlaybackInfo playbackInfo) {
            super(Type.PLAYBACK_INFO_UPDATED);
            this.playbackInfo = playbackInfo;
        }
    }

    public static class MetadataAvailable extends PlayerEvent {

        public final List<PKMetadata> metadataList;

        public MetadataAvailable(List<PKMetadata> metadataList) {
            super(Type.METADATA_AVAILABLE);
            this.metadataList = metadataList;
        }
    }

    public static class SourceSelected extends PlayerEvent {

        public final PKMediaSource source;

        public SourceSelected(PKMediaSource source) {
            super(Type.SOURCE_SELECTED);
            this.source = source;
        }
    }

    public static class PlayheadUpdated extends PlayerEvent {

        public final long position;
        public final long bufferPosition;
        public final long duration;

        public PlayheadUpdated(long position, long bufferPosition, long duration) {
            super(Type.PLAYHEAD_UPDATED);
            this.position = position;
            this.bufferPosition = bufferPosition;
            this.duration = duration;
        }
    }

    public static class Error extends PlayerEvent {

        public final PKError error;

        public Error(PKError error) {
            super(Type.ERROR);
            this.error = error;
        }
    }

    public static class Seeking extends PlayerEvent {

        public final long currentPosition;
        public final long targetPosition;

        public Seeking(long currentPosition, long targetPosition) {
            super(Type.SEEKING);
            this.currentPosition = currentPosition;
            this.targetPosition = targetPosition;
        }
    }

    public static class VideoTrackChanged extends PlayerEvent {

        public final VideoTrack newTrack;

        public VideoTrackChanged(VideoTrack newTrack) {
            super(Type.VIDEO_TRACK_CHANGED);
            this.newTrack = newTrack;
        }
    }

    public static class AudioTrackChanged extends PlayerEvent {

        public final AudioTrack newTrack;

        public AudioTrackChanged(AudioTrack newTrack) {
            super(Type.AUDIO_TRACK_CHANGED);
            this.newTrack = newTrack;
        }
    }

    public static class TextTrackChanged extends PlayerEvent {

        public final TextTrack newTrack;

        public TextTrackChanged(TextTrack newTrack) {
            super(Type.TEXT_TRACK_CHANGED);
            this.newTrack = newTrack;
        }
    }

    public static class EventStreamChanged extends PlayerEvent {
        public final List<EventStream> eventStreamsList;
        public EventStreamChanged(List<EventStream> eventStreams) {
            super(Type.EVENT_STREAMS_CHANGED);
            this.eventStreamsList = eventStreams;
        }
    }

    public static class ImageTrackChanged extends PlayerEvent {

        public final ImageTrack newTrack;

        public ImageTrackChanged(ImageTrack newTrack) {
            super(Type.IMAGE_TRACK_CHANGED);
            this.newTrack = newTrack;
        }
    }

    public static class PlaybackRateChanged extends PlayerEvent {

        public final float rate;

        public PlaybackRateChanged(float rate) {
            super(Type.PLAYBACK_RATE_CHANGED);
            this.rate = rate;
        }
    }

    public static class SubtitlesStyleChanged extends PlayerEvent {

        public final String styleName;

        public SubtitlesStyleChanged(String styleName) {
            super(Type.SUBTITLE_STYLE_CHANGED);
            this.styleName = styleName;
        }
    }

    public static class SurfaceAspectRationResizeModeChanged extends PlayerEvent {

        public final PKAspectRatioResizeMode resizeMode;

        public SurfaceAspectRationResizeModeChanged(PKAspectRatioResizeMode resizeMode) {
            super(Type.ASPECT_RATIO_RESIZE_MODE_CHANGED);
            this.resizeMode = resizeMode;
        }
    }

    public static class VideoFramesDropped extends PlayerEvent {
        public final long droppedVideoFrames;
        public final long droppedVideoFramesPeriod;
        public final long totalDroppedVideoFrames;

        public VideoFramesDropped(long droppedVideoFrames, long droppedVideoFramesPeriod, long totalDroppedVideoFrames) {
            super(Type.VIDEO_FRAMES_DROPPED);
            this.droppedVideoFrames = droppedVideoFrames;
            this.droppedVideoFramesPeriod = droppedVideoFramesPeriod;
            this.totalDroppedVideoFrames = totalDroppedVideoFrames;
        }

        @NonNull
        @Override
        public String toString() {
            return "VideoFramesDropped{" +
                    "droppedVideoFrames=" + droppedVideoFrames +
                    ", droppedVideoFramesPeriod=" + droppedVideoFramesPeriod +
                    ", totalDroppedVideoFrames=" + totalDroppedVideoFrames +
                    '}';
        }
    }

    public static class ConnectionAcquired extends PlayerEvent {

        public final URIConnectionAcquiredInfo uriConnectionAcquiredInfo;

        public ConnectionAcquired(URIConnectionAcquiredInfo uriConnectionAcquiredInfo) {
            super(Type.CONNECTION_ACQUIRED);
            this.uriConnectionAcquiredInfo = uriConnectionAcquiredInfo;
        }
    }

    public static class OutputBufferCountUpdate extends PlayerEvent {
        public final int skippedOutputBufferCount;
        public final int renderedOutputBufferCount;

        public OutputBufferCountUpdate(int skippedOutputBufferCount, int renderedOutputBufferCount) {
            super(Type.OUTPUT_BUFFER_COUNT_UPDATE);
            this.skippedOutputBufferCount = skippedOutputBufferCount;
            this.renderedOutputBufferCount = renderedOutputBufferCount;
        }
    }

    public static class BytesLoaded extends PlayerEvent {

        /*
        TRACK_TYPE_UNKNOWN = -1;
        TRACK_TYPE_DEFAULT = 0;
        TRACK_TYPE_AUDIO = 1;
        TRACK_TYPE_VIDEO = 2;
        TRACK_TYPE_TEXT = 3;
        TRACK_TYPE_METADATA = 4;
        TRACK_TYPE_CAMERA_MOTION = 5;
        TRACK_TYPE_NONE = 6;
*/
        public final int trackType;
/*
        DATA_TYPE_UNKNOWN = 0;
        DATA_TYPE_MEDIA = 1;
        DATA_TYPE_MEDIA_INITIALIZATION = 2;
        DATA_TYPE_DRM = 3;
        DATA_TYPE_MANIFEST = 4;
        DATA_TYPE_TIME_SYNCHRONIZATION = 5;
        DATA_TYPE_AD = 6;
        DATA_TYPE_MEDIA_PROGRESSIVE_LIVE = 7;

 */
        public final int dataType;

        public final long bytesLoaded;
        public final long loadDuration;
        public final long totalBytesLoaded;

        public BytesLoaded(int trackType, int dataType, long bytesLoaded, long loadDuration, long totalBytesLoaded) {
            super(Type.BYTES_LOADED);
            this.trackType = trackType;
            this.dataType = dataType;
            this.bytesLoaded = bytesLoaded;
            this.loadDuration = loadDuration;
            this.totalBytesLoaded = totalBytesLoaded;
        }

        @NonNull
        @Override
        public String toString() {
            return "BytesLoaded{" +
                    "bytesLoaded=" + bytesLoaded +
                    ", totalBytesLoaded=" + totalBytesLoaded +
                    '}';
        }
    }

    public enum Type {
        STATE_CHANGED,
        CAN_PLAY,   // Sent when enough data is available that the media can be played, at least for a couple of frames. This corresponds to the HAVE_ENOUGH_DATA readyState.
        DURATION_CHANGE,   //  The metadata has loaded or changed, indicating a change in duration of the media. This is sent, for example, when the media has loaded enough that the duration is known.
        ENDED,   //  Sent when playback completes.
        ERROR,   //  Sent when an error occurs. The element's error attribute contains more information. See Error handling for details.
        LOADED_METADATA,   //  The media's metadata has finished loading; all attributes now contain as much useful information as they're going to.
        PAUSE,   //  Sent when playback is paused.
        PLAY,    //  Sent when playback of the media starts after having been paused; that is, when playback is resumed after a prior pause event.
        RETRY,   //  Sent when retry api is called by app
        PLAYING,   //  Sent when the media begins to play (either for the first time, after having been paused, or after ending and then restarting).
        SEEKED,   //  Sent when a seek operation completes.
        SEEKING,   //  Sent when a seek operation begins.
        TRACKS_AVAILABLE, // Sent when track info is available.
        REPLAY, //Sent when replay happened.
        PLAYBACK_INFO_UPDATED, // Sent event that notify about changes in the playback parameters. When bitrate of the video or audio track changes or new media loaded. Holds the PlaybackInfo.java object with relevant data.
        VOLUME_CHANGED, // Sent when volume is changed.
        STOPPED, // sent when stop player api is called
        METADATA_AVAILABLE, // Sent when there is metadata available for this entry.
        SOURCE_SELECTED, // Sent when the source was selected.
        PLAYHEAD_UPDATED, //Send player position every 100 Milisec
        VIDEO_TRACK_CHANGED,
        AUDIO_TRACK_CHANGED,
        TEXT_TRACK_CHANGED,
        IMAGE_TRACK_CHANGED,
        PLAYBACK_RATE_CHANGED,
        CONNECTION_ACQUIRED,
        VIDEO_FRAMES_DROPPED,   // Video frames were dropped, see PlayerEvent.VideoFramesDropped
        OUTPUT_BUFFER_COUNT_UPDATE,
        BYTES_LOADED,           // Bytes were downloaded from the network
        SUBTITLE_STYLE_CHANGED,  // Subtitle style is changed.
        ASPECT_RATIO_RESIZE_MODE_CHANGED, //Send when updating the Surface Vide Aspect Ratio size mode.
        EVENT_STREAMS_CHANGED //Send event streams received from manifest
    }

    @Override
    public Enum eventType() {
        return this.type;
    }


}
