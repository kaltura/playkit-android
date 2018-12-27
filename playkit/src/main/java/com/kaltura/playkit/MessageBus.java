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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("WeakerAccess")
public class MessageBus {

    private static final PKLog log = PKLog.get("MessageBus");

    private Handler postHandler = new Handler(Looper.getMainLooper());
    private Map<Object, Set<PKEvent.Listener>> listeners;   // Key is the event type, value is the listeners to call.
    private Map<Object, Set<PKEvent.Listener>> listenerGroups;  // Key is group id, value is the listeners to remove.

    public MessageBus() {
        listeners = new ConcurrentHashMap<>();
        listenerGroups = Collections.synchronizedMap(new WeakHashMap<>());
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
                        log.e("Wrong type of listener " + listener.getClass() + " for event (" + event.eventType() + ")", e);
                    }
                }
            });
        }
    }

    private static Set<PKEvent.Listener> safeSet(@Nullable Set<PKEvent.Listener> listeners) {
        return listeners != null ? listeners : Collections.emptySet();
    }

    @Deprecated
    public void remove(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            Set<PKEvent.Listener> listenerSet = listeners.get(eventType);
            if (listenerSet != null) {
                listenerSet.remove(listener);
            }
        }
    }

    private static <T> Set<T> clone(Set<T> set) {
        return new HashSet<>(set);
    }

    /**
     * Remove the listener regardless of event type.
     * @param listener Listener to remove.
     */
    public void removeListener(PKEvent.Listener listener) {
        for (Set<PKEvent.Listener> listenerSet : listeners.values()) {
            removeListener(listenerSet, listener);
        }

        for (Set<PKEvent.Listener> listenerSet : listenerGroups.values()) {
            removeListener(listenerSet, listener);
        }
    }

    private static void removeListener(Set<PKEvent.Listener> listenerSet, PKEvent.Listener listener) {
        Iterator<PKEvent.Listener> iterator = listenerSet.iterator();
        while (iterator.hasNext()) {
            PKEvent.Listener element = iterator.next();
            if (element == listener) {
                iterator.remove();
            }
        }
    }

    /**
     * Remove all listeners in the collection regardless of event type.
     * @param groupId Group id of listeners to remove.
     */
    public void removeListeners(Object groupId) {
        final Set<PKEvent.Listener> listeners = listenerGroups.get(groupId);

        for (PKEvent.Listener listener : new HashSet<>(listeners)) {
            removeListener(listener);
        }
    }

    @Deprecated
    public PKEvent.Listener listen(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            addListener(null, eventType, listener);
        }
        return listener;
    }

    public void addListener(Object groupId, Enum type, PKEvent.Listener listener) {
        addListener(groupId, (Object)type, listener);
    }

    public <E extends PKEvent> void addListener(Object groupId, Class<E> type, PKEvent.Listener<E> listener) {
        addListener(groupId, (Object)type, listener);
    }

    private void addListener(Object groupId, Object type, PKEvent.Listener listener) {
        addToMap(type, listener, listeners);
        addToMap(groupId, listener, listenerGroups);
    }

    private void addToMap(Object key, PKEvent.Listener listener, Map<Object, Set<PKEvent.Listener>> map) {
        Set<PKEvent.Listener> listenerSet;
        listenerSet = map.get(key);
        if (listenerSet == null) {
            listenerSet = new HashSet<>();
            listenerSet.add(listener);
            map.put(key, listenerSet);
        } else {
            listenerSet.add(listener);
        }
    }
}
