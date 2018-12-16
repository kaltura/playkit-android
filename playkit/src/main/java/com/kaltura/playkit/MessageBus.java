/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("WeakerAccess")
public class MessageBus {
    private static final String TAG = "MessageBus";
    private Handler postHandler = new Handler(Looper.getMainLooper());
    private Map<Object, Set<PKEvent.Listener>> listeners;

    public MessageBus() {
        listeners = new ConcurrentHashMap<>();
    }

    public void post(final PKEvent event) {

        final Set<PKEvent.Listener> listenerSet = new HashSet<>();
        listenerSet.addAll(safeSet(this.listeners.get(event.eventType())));
        listenerSet.addAll(safeSet(this.listeners.get(event.getClass())));

        if (!listenerSet.isEmpty()) {
            postHandler.post(() -> {
                for (PKEvent.Listener listener : listenerSet) {
                    try {
                        // If the listener type does not match event type (programming error),
                        // there will be a ClassCastException. Log it but don't crash.
                        //noinspection unchecked
                        listener.onEvent(event);
                    } catch (ClassCastException e) {
                        Log.e(TAG, "Wrong type of listener " + listener.getClass() + " for event (" + event.eventType() + ")", e);
                    }
                }
            });
        }
    }

    private Set<PKEvent.Listener> safeSet(@Nullable Set<PKEvent.Listener> listeners) {
        return listeners != null ? listeners : Collections.emptySet();
    }

    public void remove(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            Set<PKEvent.Listener> listenerSet = listeners.get(eventType);
            if (listenerSet != null) {
                listenerSet.remove(listener);
            }
        }
    }

    public void removeListener(PKEvent.Listener listener) {
        for (Set<PKEvent.Listener> listenerSet : listeners.values()) {
            Iterator<PKEvent.Listener> iterator = listenerSet.iterator();
            while (iterator.hasNext()) {
                PKEvent.Listener element = iterator.next();
                if (element == listener) {
                    iterator.remove();
                }
            }
        }
    }

    public PKEvent.Listener listen(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            addListener(eventType, listener);
        }
        return listener;
    }

    public void addListener(Enum type, PKEvent.Listener listener) {
        addListener((Object)type, listener);
    }

    public <E extends PKEvent> void addListener(Class<E> type, PKEvent.Listener listener) {
        addListener((Object)type, listener);
    }

    private void addListener(Object type, PKEvent.Listener listener) {
        Set<PKEvent.Listener> listenerSet = listeners.get(type);
        if (listenerSet == null) {
            listenerSet = new HashSet<>();
            listenerSet.add(listener);
            listeners.put(type, listenerSet);
        } else {
            listenerSet.add(listener);
        }
    }
}
