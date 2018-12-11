package com.kaltura.playkit;

import android.support.annotation.NonNull;

public interface MessageBus extends PKMessage.Poster {

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
}
