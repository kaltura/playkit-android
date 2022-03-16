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
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@SuppressWarnings("WeakerAccess")
public class MessageBus {

    private static final PKLog log = PKLog.get("MessageBus");

    private Handler postHandler = new Handler(Looper.getMainLooper());
    private Map<Object, Set<PKEvent.Listener>> listeners;   // Key is the event type, value is the listeners to call.
    private Map<Object, Set<PKEvent.Listener>> listenerGroups;  // Key is group id, value is the listeners to remove.

    public MessageBus() {
        listeners = Collections.synchronizedMap(new HashMap<>());
        listenerGroups = Collections.synchronizedMap(new WeakHashMap<>());
    }

    public void post(final PKEvent event) {

        // Listeners that are listening for this event
        final Set<PKEvent.Listener> postListeners = new HashSet<>();
        try {
            // By event type (PlayerEvent.DURATION_CHANGED etc)
            postListeners.addAll(safeSet(this.listeners.get(event.eventType())));
        } catch (ConcurrentModificationException ex) {
            postListeners.addAll(safeSet(this.listeners.get(event.eventType())));
        }
        try {
            // By event class (PlayerEvent.DurationChanged.class etc)
            postListeners.addAll(safeSet(this.listeners.get(event.getClass())));
        } catch (ConcurrentModificationException ex) {
            postListeners.addAll(safeSet(this.listeners.get(event.getClass())));
        }

        if (!postListeners.isEmpty()) {
            postHandler.post(() -> {
                for (PKEvent.Listener listener : postListeners) {
                    // If the listener type does not match event type (programming error),
                    // there will be a ClassCastException. Log it but don't crash.
                    try {
                        //noinspection unchecked
                        listener.onEvent(event);
                    } catch (ClassCastException e) {
                        log.e("Wrong type of listener " + listener.getClass() + " for event (" + event.eventType() + ")", e);
                    }
                }
            });
        }
    }

    public void post(final Runnable runnable) {
        if (postHandler != null) {
            postHandler.post(runnable);
        }
    }

    private static Set<PKEvent.Listener> safeSet(@Nullable Set<PKEvent.Listener> listeners) {
        return listeners != null ? listeners : Collections.emptySet();
    }


    /**
     * Remove the listener regardless of event type.
     * @param listener Listener to remove.
     */
    public void removeListener(PKEvent.Listener listener) {
        // Remove the listener from the type:listeners map
        for (Set<PKEvent.Listener> listenerSet : listeners.values()) {
            listenerSet.remove(listener);
        }

        // Remove the listener from the groupId:listeners map
        for (Set<PKEvent.Listener> listenerSet : listenerGroups.values()) {
            listenerSet.remove(listener);
        }

        // Remove empty groups
        final Iterator<Map.Entry<Object, Set<PKEvent.Listener>>> iterator = listenerGroups.entrySet().iterator();
        while (iterator.hasNext()) {
            final Set<PKEvent.Listener> value = iterator.next().getValue();
            if (value == null || value.isEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * Remove all listeners in the group regardless of event type.
     * @param groupId Group id of listeners to remove.
     */
    public void removeListeners(Object groupId) {
        if (groupId == null) {
            return;
        }

        final Set<PKEvent.Listener> listeners = listenerGroups.get(groupId);

        if (listeners != null) {
            // Remove the listener from the type:listeners map
            for (PKEvent.Listener listener : listeners) {
                for (Set<PKEvent.Listener> listenerSet : this.listeners.values()) {
                    listenerSet.remove(listener);
                }
            }

            listenerGroups.remove(groupId);
        }
    }

    /**
     * Add listener with groupId.
     * @param groupId   Group to which the listener belongs, for {@link #removeListeners(Object)}.
     * @param type      Type of event to listen to.
     * @param listener  Listener to call when the event occurs.
     */
    public void addListener(Object groupId, Enum type, PKEvent.Listener listener) {
        addListener(groupId, (Object)type, listener);
    }

    /**
     * Add listener with groupId.
     * @param groupId   Group to which the listener belongs, for {@link #removeListeners(Object)}.
     * @param type      Type of event to listen to.
     * @param listener  Listener to call when the event occurs.
     */
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

    // Deprecated
    /**
     *
     * @param listener      Listener to call when the events in the list occur.
     * @param eventTypes    List of event types.
     * @deprecated Please use {@link #addListener(Object, Class, PKEvent.Listener)} or {@link #addListener(Object, Enum, PKEvent.Listener)}.
     */
    @Deprecated
    public PKEvent.Listener listen(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            addListener(null, eventType, listener);
        }
        return listener;
    }

    /**
     * Remove listener for specific event types.
     * @param listener      Listener to call when the events in the list occur.
     * @param eventTypes    List of event types.
     * @deprecated Please use {@link #removeListeners(Object)} with {@link #addListener(Object, Class, PKEvent.Listener)} or {@link #addListener(Object, Enum, PKEvent.Listener)}.
     */
    @Deprecated
    public void remove(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            Set<PKEvent.Listener> listenerSet = listeners.get(eventType);
            if (listenerSet != null) {
                listenerSet.remove(listener);
            }
        }
    }
}
