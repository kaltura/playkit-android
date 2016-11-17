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

        Set<PKEvent.Listener> listeners = this.listeners.get(event.eventId());
        if (listeners != null) {
            for (PKEvent.Listener listener : listeners) {
                listener.onEvent(event);
            }
        }
    }
    
    public void listen(PKEvent.Listener listener, PKEvent... events) {
        for (PKEvent event : events) {
            Set<PKEvent.Listener> listenerSet = listeners.get(event.eventId());
            if (listenerSet == null) {
                listenerSet = new HashSet<>();
                listenerSet.add(listener);
                listeners.put(event, listenerSet);
            } else {
                listenerSet.add(listener);
            }
        }
        
    }
}
