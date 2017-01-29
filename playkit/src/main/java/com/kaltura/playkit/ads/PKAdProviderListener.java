package com.kaltura.playkit.ads;

/**
 * Created by gilad.nadav on 25/01/2017.
 */
public interface PKAdProviderListener {
    void prepareOnAdStarted();
    void prepareOnNoPreroll();
    void prepareOnAdRequestFailed();
}
