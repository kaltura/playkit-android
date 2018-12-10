package com.kaltura.playkit;

import android.support.annotation.NonNull;

/**
 * A message that can be sent by running code. Should be implemented using a lambda.
 * Usage:
 * poster.postAdsEvent(L->L.onAdStarted(adInfo))
 * poster.postPlayerEvent(L->L.onPlaying())
 * @param <L> listener type
 */
public interface PKMessage<L> {
    void run(@NonNull L L);

    interface Poster {
        void post(@NonNull PKEvent event);

        void postPlayerEvent(@NonNull PKMessage<PlayerListener> message);

        void postAdsEvent(@NonNull PKMessage<AdsListener> message);
    }
}
