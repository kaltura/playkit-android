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
import android.support.annotation.NonNull;
import android.util.Log;

import com.kaltura.playkit.plugins.ads.AdEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Noam Tamim @ Kaltura on 07/11/2016.
 */
@SuppressWarnings("WeakerAccess")
public class MessageBus implements Post.Target {
    private static final String TAG = "MessageBus";

    private Handler postHandler = new Handler(Looper.getMainLooper());
    private Map<Object, Set<Object>> listeners;
    private final LegacyEventAdapter legacyEventAdapter;

    enum ListenerType {
        player, ads
    }

    public MessageBus() {
        listeners = new ConcurrentHashMap<>();

        // Pre-allocate the sets for player and ads listeners.
        listeners.put(ListenerType.player, new HashSet<>(10));
        listeners.put(ListenerType.ads, new HashSet<>(10));


        legacyEventAdapter = new LegacyEventAdapter(this);

        // Forward new-style player events to legacy listeners
        getListeners(ListenerType.player).add(legacyEventAdapter.newToLegacyPlayerEvents);

        // Forward new-style ads events to legacy listeners
        getListeners(ListenerType.ads).add(legacyEventAdapter.newToLegacyAdsEvents);

        // Forward legacy ads events to new listeners. Player events are only sent in new format.
        listen(legacyEventAdapter.legacyToNewAdsEvents, AdEvent.Type.values());
    }

    public void postPlayerEvent(Post<PlayerListener> post) {
        final Set<Object> listeners = getListeners(ListenerType.player);
        if (listeners == null) {
            return;
        }
        post(() -> {
            for (Object listener : listeners) {
                if (listener instanceof PlayerListener) {
                    post.run(((PlayerListener) listener));
                }
            }
        });
    }

    public void postAdsEvent(Post<AdsListener> post) {
        final Set<Object> listeners = this.getListeners(ListenerType.ads);
        if (listeners == null) {
            return;
        }
        post(() -> {
            for (Object listener : listeners) {
                if (listener instanceof AdsListener) {
                    post.run(((AdsListener) listener));
                }
            }
        });
    }

    void post(Runnable runnable) {
        postHandler.post(runnable);
    }

    public void post(final PKEvent event) {

        if (event instanceof PlayerEvent || event instanceof AdEvent) {
            Log.d(TAG, "LEGACY POSTING EVENT " + event.eventType());
        }

        postInternal(event, false);
    }

    void postFromAdapter(PKEvent event) {

        Log.d(TAG, "ADAPTER POSTING EVENT " + event.eventType());

        postInternal(event, true);
    }

    private void postInternal(PKEvent event, boolean fromProxy) {

        final Set<Object> listeners = this.listeners.get(event.eventType());

        if (listeners == null) {
            return;
        }
        postHandler.post(() -> {
            for (Object listener : new HashSet<>(listeners)) {
                if (listener instanceof PKEvent.Listener) {
                    if (fromProxy && listener == legacyEventAdapter.legacyToNewAdsEvents) {
                        continue;
                    }
                    ((PKEvent.Listener) listener).onEvent(event);
                }
            }
        });
    }

    public void remove(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            Set<Object> listenerSet = listeners.get(eventType);
            if (listenerSet != null) {
                listenerSet.remove(listener);
            }
        }
    }

    public void removeListener(PKEvent.Listener listener) {
        for (Set<Object> listenerSet : listeners.values()) {
            Iterator<Object> iterator = listenerSet.iterator();
            while (iterator.hasNext()) {
                Object element = iterator.next();
                if (element == listener) {
                    iterator.remove();
                }
            }
        }
    }

    public PKEvent.Listener listen(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            Set<Object> listenerSet = listeners.get(eventType);
            if (listenerSet == null) {
                listenerSet = new HashSet<>();
                listenerSet.add(listener);
                listeners.put(eventType, listenerSet);
            } else {
                listenerSet.add(listener);
            }
        }
        return listener;
    }

    public void listen(PlayerListener listener) {
        getListeners(ListenerType.player).add(listener);
    }

    private Set<Object> getListeners(ListenerType type) {
        return listeners.get(type);
    }

    public void listen(AdsListener listener) {
        getListeners(ListenerType.ads).add(listener);
    }
}
