package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.plugins.ads.ima.IMAConfig;

/**
 * Created by gilad.nadav on 17/11/2016.
 */

public interface AdsProvider {
    IMAConfig getAdsConfig();
    void requestAd();
    void init();
    boolean start();
    void resume();
    void pause();
    void contentCompleted();
    PKAdInfo getAdInfo();
    boolean isAdDisplayed();
    boolean isAdPaused();
    boolean isAdRequested();
    long getDuration();
    long getCurrentPosition();

    void skipAd();
}
