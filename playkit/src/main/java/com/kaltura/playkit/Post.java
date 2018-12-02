package com.kaltura.playkit;

public interface Post<L> {
    void run(L L);

    interface Target {
        void postPlayerEvent(Post<PlayerListener> post);

        void postAdsEvent(Post<AdsListener> post);

        void post(final PKEvent event);
    }
}
