package com.kaltura.playkit;

/**
 * Created by Noam Tamim @ Kaltura on 24/10/2016.
 */
public enum PlayerState {
    IDLE, LOADING, READY, BUFFERING;
    
    public static final PKEvent EVENT = new PKEvent() {
        @Override
        public String name() {
            return "StateChangedEvent";
        }
    };
    
    public PKEvent stateChangedEvent(PlayerState oldState) {
        return new StateChangedEvent(oldState, this);
    }
    
    public static class StateChangedEvent implements PKEvent {

        public final PlayerState newState;
        public final PlayerState oldState;

        public StateChangedEvent(PlayerState newState, PlayerState oldState) {
            this.newState = newState;
            this.oldState = oldState;
        }

        @Override
        public String name() {
            return "StateChangedEvent";
        }
    }
}
