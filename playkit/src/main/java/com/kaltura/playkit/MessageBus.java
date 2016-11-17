package com.kaltura.playkit;

import android.content.Context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Noam Tamim @ Kaltura on 07/11/2016.
 */
public class MessageBus {
    private final Context context;
    private Map<PKEvent, Set<Listener>> listeners;

    public MessageBus(Context context) {
        this.context = context;
        listeners = new HashMap<>();
    }
    
    public void post(PKEvent event) {
        for (Listener listener : listeners.get(event)) {
            listener.onEvent(event);
        }
    }
    
    public void listen(Listener listener, PKEvent... events) {
        for (PKEvent event : events) {
            Set<Listener> listenerSet = listeners.get(event);
            if (listenerSet == null) {
                listenerSet = new HashSet<>();
                listenerSet.add(listener);
                listeners.put(event, listenerSet);
            } else {
                listenerSet.add(listener);
            }
        }
    }
    
    public interface Listener {
        void onEvent(PKEvent event);
    } 
}
