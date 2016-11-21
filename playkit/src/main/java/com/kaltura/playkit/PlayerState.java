package com.kaltura.playkit;

/**
 * Created by Noam Tamim @ Kaltura on 24/10/2016.
 */
public enum PlayerState {
    IDLE, LOADING, READY, BUFFERING;

    public interface Listener {
        void onPlayerStateChanged(Player player, PlayerState newState);
    }
    
    public static final PKEvent EVENT_TYPE = new PKEvent() {
        @Override
        public Object eventId() {
            return this;
        }
    };
    
    public static class Event implements PKEvent {
        
        public final PlayerState oldState, newState;

        public Event(PlayerState oldState, PlayerState newState) {
            this.oldState = oldState;
            this.newState = newState;
        }


        @Override
        public Object eventId() {
            return EVENT_TYPE;
        }
    }
}
