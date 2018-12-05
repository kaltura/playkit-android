package com.kaltura.playkit;

public interface MessageBus {
    PKEvent.Listener listen(PKEvent.Listener listener, Enum... eventTypes);

    void remove(PKEvent.Listener listener, Enum... eventTypes);

    void removeListener(PKEvent.Listener listener);

    void post(PKEvent event);

    void addListener(PlayerListener listener);

    void addListener(AdsListener listener);

    void postPlayerEvent(Post<PlayerListener> post);

    void postAdsEvent(Post<AdsListener> post);

    interface Post<L> {
        void run(L L);
    }
}
