package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.PKAdInfo;

/**
 * Created by gilad.nadav on 17/11/2016.
 */

public interface AdsProvider {
    String getPluginName();
    AdsConfig getAdsConfig();
    void requestAd();
    boolean start(boolean showLoadingView);
    void resume();
    void pause();
    void contentCompleted();
    PKAdInfo getAdInfo();
    boolean isAdDisplayed();
    boolean isAdPaused();
    boolean isAdRequested();

}
