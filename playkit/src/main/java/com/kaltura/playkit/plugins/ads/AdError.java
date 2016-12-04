package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.Player;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdError implements PKEvent {

    public Type errorType;
    public String message;

    public AdError(Type type, String message) {
        this.errorType = type;
        this.message = message;
    }

    public enum Type {

        INTERNAL_ERROR,
        VAST_MALFORMED_RESPONSE,
        UNKNOWN_AD_RESPONSE,
        VAST_LOAD_TIMEOUT,
        VAST_TOO_MANY_REDIRECTS,
        VIDEO_PLAY_ERROR,
        VAST_MEDIA_LOAD_TIMEOUT,
        VAST_LINEAR_ASSET_MISMATCH,
        OVERLAY_AD_PLAYING_FAILED,
        OVERLAY_AD_LOADING_FAILED,
        VAST_NONLINEAR_ASSET_MISMATCH,
        COMPANION_AD_LOADING_FAILED,
        UNKNOWN_ERROR,
        VAST_EMPTY_RESPONSE,
        FAILED_TO_REQUEST_ADS,
        VAST_ASSET_NOT_FOUND,
        ADS_REQUEST_NETWORK_ERROR,
        INVALID_ARGUMENTS,
        PLAYLIST_NO_CONTENT_TRACKING;

    }

    @Override
    public Enum eventType() {
        return this.errorType;
    }
    public interface Listener {
        void onPlayerEvent(Player player, AdError.Type event, String message);
    }

}
