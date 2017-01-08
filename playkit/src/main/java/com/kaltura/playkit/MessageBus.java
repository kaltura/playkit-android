package com.kaltura.playkit;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Noam Tamim @ Kaltura on 07/11/2016.
 */
@SuppressWarnings("WeakerAccess")
public class MessageBus {
    private Handler postHandler = new Handler(Looper.getMainLooper());
    private Map<Object, Set<PKEvent.Listener>> listeners;
    
    public MessageBus() {
        listeners = new HashMap<>();
    }
    
    public void post(final PKEvent event) {

        final Set<PKEvent.Listener> listeners = this.listeners.get(event.eventType());
        
        if (listeners != null) {
            postHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (PKEvent.Listener listener : listeners) {
                        listener.onEvent(event);
                    }
                }
            });
        }
    }

    public void remove(PKEvent.Listener listener, Enum... eventTypes){
        for (Enum eventType : eventTypes) {
            Set<PKEvent.Listener> listenerSet = listeners.get(eventType);
            if (listenerSet != null) {
                listenerSet.remove(listener);
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
