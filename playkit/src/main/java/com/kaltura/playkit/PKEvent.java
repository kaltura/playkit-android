package com.kaltura.playkit;

@PKPublicAPI
public interface PKEvent {
    Enum eventType();

    interface Listener {
        void onEvent(PKEvent event);
    }
}

