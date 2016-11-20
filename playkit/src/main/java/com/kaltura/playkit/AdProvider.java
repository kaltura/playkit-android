package com.kaltura.playkit;

public interface AdProvider {
    PKAdInfo getAdInfo();
    void start();
    // TODO more as required by IMA
}
