package com.kaltura.playkit;

import android.content.Context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Noam Tamim @ Kaltura on 07/11/2016.
 */
@SuppressWarnings("WeakerAccess")
public class MessageBus {
    private final Context context;
    private Map<Object, Set<PKEvent.Listener>> listeners;

    public MessageBus(Context context) {
        this.context = context;
        listeners = new HashMap<>();
    }
    
    public void post(PKEvent event) {

        Set<PKEvent.Listener> listeners = this.listeners.get(event.eventType());
        if (listeners != null) {
            for (PKEvent.Listener listener : listeners) {
                listener.onEvent(event);
            }
        }
    }
    
    public void listen(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            Set<PKEvent.Listener> listenerSet = listeners.get(eventType);
            if (listenerSet == null) {
                listenerSet = new HashSet<>();
                listenerSet.add(listener);
                listeners.put(eventType, listenerSet);
            } else {
                listenerSet.add(listener);
            }
        }
    }
}
