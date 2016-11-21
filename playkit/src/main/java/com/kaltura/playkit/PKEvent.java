package com.kaltura.playkit;

public interface PKEvent {
    Object eventId();

    interface Listener {
        void onEvent(PKEvent event);
    }
}

