package com.kaltura.playkit;

public interface PKEvent {
    Enum eventType();

    interface Listener {
        void onEvent(PKEvent event);
    }
}

