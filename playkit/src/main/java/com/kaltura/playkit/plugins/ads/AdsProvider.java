package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.ads.AdEnabledPlayerController;
import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.plugins.ads.ima.IMAConfig;


public interface AdsProvider {
    IMAConfig getAdsConfig();

    void start();
    void destroy();
    void resume();
    void pause();
    void contentCompleted();
    PKAdInfo getAdInfo();
    boolean isAdDisplayed();
    boolean isAdPaused();
    boolean isAdRequested();
    long getDuration();
    long getCurrentPosition();
    void setAdProviderListener(AdEnabledPlayerController adEnabledPlayerController);
    void removeAdProviderListener();
    void skipAd();
}
