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

import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.player.metadata.PKMetadata;

import java.util.List;

/**
 * Created by Noam Tamim @ Kaltura on 24/10/2016.
 */


public class PlayerEvent implements PKEvent {

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

        public TracksAvailable(PKTracks tracksInfo) {
            super(Type.TRACKS_AVAILABLE);
            this.tracksInfo = tracksInfo;
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

    public static class Error extends PlayerEvent {

        public final PKError error;

        public Error(PKError error) {
            super(Type.ERROR);
            this.error = error;
        }
    }

    public final Type type;

    public PlayerEvent(Type type) {
        this.type = type;
    }

    public enum Type {
        STATE_CHANGED,
        CAN_PLAY,   // Sent when enough data is available that the media can be played, at least for a couple of frames. This corresponds to the HAVE_ENOUGH_DATA readyState.
        DURATION_CHANGE,   //  The metadata has loaded or changed, indicating a change in duration of the media. This is sent, for example, when the media has loaded enough that the duration is known.
        ENDED,   //  Sent when playback completes.
        ERROR,   //  Sent when an error occurs. The element's error attribute contains more information. See Error handling for details.
        LOADED_METADATA,   //  The media's metadata has finished loading; all attributes now contain as much useful information as they're going to.
        PAUSE,   //  Sent when playback is paused.
        PLAY,   //  Sent when playback of the media starts after having been paused; that is, when playback is resumed after a prior pause event.
        PLAYING,   //  Sent when the media begins to play (either for the first time, after having been paused, or after ending and then restarting).
        SEEKED,   //  Sent when a seek operation completes.
        SEEKING,   //  Sent when a seek operation begins.
        TRACKS_AVAILABLE, // Sent when track info is available.
        REPLAY, //Sent when replay happened.
        PLAYBACK_INFO_UPDATED, // Sent event that notify about changes in the playback parameters. When bitrate of the video or audio track changes or new media loaded. Holds the PlaybackInfo.java object with relevant data.
        VOLUME_CHANGED, // Sent when volume is changed.
        STOPPED, // sent when stop player api is called
        METADATA_AVAILABLE, // Sent when there is metadata available for this entry.
        SOURCE_SELECTED // Sent when the source was selected.
    }

    @Override
    public Enum eventType() {
        return this.type;
    }

    public interface Listener {
        void onPlayerEvent(Player player, Type event);
    }
}