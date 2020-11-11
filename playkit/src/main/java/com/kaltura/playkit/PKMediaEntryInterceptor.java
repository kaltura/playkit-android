package com.kaltura.playkit;

/**
 * Created by alex_lytvynenko on 09.11.2020.
 */
public interface PKMediaEntryInterceptor {
    void apply(PKMediaEntry mediaEntry, OnMediaInterceptorListener listener);

    interface OnMediaInterceptorListener {
        void onApplyMediaCompleted(PKError error);
    }
}
