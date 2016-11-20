package com.kaltura.playkit;

/**
 * Created by Noam Tamim @ Kaltura on 24/10/2016.
 */

public enum PlayerEvent implements PKEvent {
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
    FIRST_PLAY; // Sent when the playback of the media started to play for the first time.


    @Override
    public Object eventId() {
        return this;
    }

    public interface Listener {
        void onPlayerEvent(Player player, PlayerEvent event);
    }
}
