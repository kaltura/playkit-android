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
    private final Context mContext;
    private Map<PKEvent, Set<Listener>> mListeners;

    public MessageBus(Context context) {
        mContext = context;
        mListeners = new HashMap<>();
    }
    
    public void post(PKEvent event) {
        for (Listener listener : mListeners.get(event)) {
            listener.onEvent(event);
        }
    }
    
    public void listen(Listener listener, PKEvent... events) {
        for (PKEvent event : events) {
            Set<Listener> listenerSet = mListeners.get(event);
            if (listenerSet == null) {
                listenerSet = new HashSet<>();
                listenerSet.add(listener);
                mListeners.put(event, listenerSet);
            } else {
                listenerSet.add(listener);
            }
        }
    }
    
    public interface Listener {
        void onEvent(PKEvent event);
    } 
}
