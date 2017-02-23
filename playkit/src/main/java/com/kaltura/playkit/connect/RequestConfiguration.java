package com.kaltura.playkit.connect;

/**
 */

public interface RequestConfiguration {

    long getReadTimeout();
    long getWriteTimeout();
    long getConnectTimeout();
    int getRetry();
}
