package com.kaltura.playkit;

public interface PKEvent {
    String name();

    interface Listener {
        void onEvent(PKEvent event);
    }
}

