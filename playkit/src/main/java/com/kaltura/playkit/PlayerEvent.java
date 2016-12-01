package com.kaltura.playkit;

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

        private final TracksInfo tracksInfo;

        public TracksAvailable(TracksInfo tracksInfo){
            super(Type.TRACKS_AVAILABLE);
            this.tracksInfo = tracksInfo;
        }

        public TracksInfo getTracksInfo() {
            return tracksInfo;
        }
    }

    public final Type type;

    private PlayerEvent(Type type) {
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
        FIRST_PLAY, // Sent when the playback of the media started to play for the first time.
        TRACKS_AVAILABLE; // Sent when track info is available.
    }

    @Override
    public Enum eventType() {
        return this.type;
    }

    public interface Listener {
        void onPlayerEvent(Player player, Type event);
    }
}
