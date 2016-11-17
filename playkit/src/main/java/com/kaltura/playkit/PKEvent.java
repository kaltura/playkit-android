package com.kaltura.playkit;

public interface PKEvent {
    String name();

    interface Listener<EventType extends PKEvent> {
        void onEvent(EventType event);
    }
}

