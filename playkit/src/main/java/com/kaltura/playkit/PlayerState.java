package com.kaltura.playkit;

/**
 * Created by Noam Tamim @ Kaltura on 24/10/2016.
 */
public enum PlayerState {
    IDLE, LOADING, READY, BUFFERING;

    public interface Listener {
        void onPlayerStateChanged(Player player, PlayerState newState);
    }
}
