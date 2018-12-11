package com.kaltura.playkit;

import android.support.annotation.NonNull;

public interface MessageBus {

    /**
     * Start listening to events of the given types.
     * @param listener an object that will receive the events.
     * @param eventTypes array of event types that will be sent to the listener.
     * @return listener
     */
    @NonNull
    PKEvent.Listener listen(@NonNull PKEvent.Listener listener, @NonNull Enum... eventTypes);

    void postRunnable(Runnable runnable);

    void remove(@NonNull PKEvent.Listener listener, @NonNull Enum... eventTypes);

    void addListener(@NonNull PlayerListener listener);

    void addListener(@NonNull AdsListener listener);

    void removeListener(@NonNull PKListener listener);

    void post(@NonNull PKEvent event);

    void postPlayerEvent(@NonNull Message<PlayerListener> message);

    void postAdsEvent(@NonNull Message<AdsListener> message);

    /**
     * A message that can be sent by running code. Should be implemented using a lambda.
     * Usage:
     * bus.postAdsEvent(L->L.onAdStarted(adInfo))
     * bus.postPlayerEvent(L->L.onPlaying())
     * @param <L> listener type
     */
    interface Message<L> {
        void run(@NonNull L L);
    }
}
