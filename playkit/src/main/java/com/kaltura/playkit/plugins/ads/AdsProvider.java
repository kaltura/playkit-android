package com.kaltura.playkit.plugins.ads;

/**
 * Created by gilad.nadav on 17/11/2016.
 */

public interface AdsProvider {
    void requestAd();
    boolean start(boolean showLoadingView);
    void resume();
    void pause();
    void contentCompleted();
    boolean isAdDisplayed();
    boolean isAdPaused();

}
