package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.Player;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdEvent implements PKEvent {

    public static class Generic extends AdEvent {
        public Generic(Type type) {
            super(type);
        }
    }

    public final Type type;

    private AdEvent(Type type) {
        this.type = type;
    }

    public enum Type {
        AD_STARTED,
        AD_PAUSED,
        AD_RESUMED,
        AD_COMPLETED,
        AD_FIRST_QUARTILE,
        AD_MIDPOINT,
        AD_THIRD_QUARTILE,
        AD_SKIPPED(),
        AD_CLICKED,
        AD_TAPPED,
        AD_ICON_TAPPED,
        AD_AD_BREAK_READY,
        AD_AD_PROGRESS,
        AD_AD_BREAK_STARTED,
        AD_AD_BREAK_ENDED,
        AD_CUEPOINTS_CHANGED,
        AD_LOADED,
        AD_CONTENT_PAUSE_REQUESTED,
        AD_CONTENT_RESUME_REQUESTED,
        AD_ALL_ADS_COMPLETED,

        AD_INTERNAL_ERROR,
        AD_VAST_MALFORMED_RESPONSE,
        AD_UNKNOWN_AD_RESPONSE,
        AD_VAST_LOAD_TIMEOUT,
        AD_VAST_TOO_MANY_REDIRECTS,
        AD_VIDEO_PLAY_ERROR,
        AD_VAST_MEDIA_LOAD_TIMEOUT,
        AD_VAST_LINEAR_ASSET_MISMATCH,
        AD_OVERLAY_AD_PLAYING_FAILED,
        AD_OVERLAY_AD_LOADING_FAILED,
        AD_VAST_NONLINEAR_ASSET_MISMATCH,
        AD_COMPANION_AD_LOADING_FAILED,
        AD_UNKNOWN_ERROR,
        AD_VAST_EMPTY_RESPONSE,
        AD_FAILED_TO_REQUEST_ADS,
        AD_VAST_ASSET_NOT_FOUND,
        AD_ADS_REQUEST_NETWORK_ERROR,
        AD_INVALID_ARGUMENTS,
        AD_PLAYLIST_NO_CONTENT_TRACKING;
    }

    @Override
    public Enum eventType() {
        return this.type;
    }

    public interface Listener {
        void onPlayerEvent(Player player, AdEvent.Type event);
    }
}
