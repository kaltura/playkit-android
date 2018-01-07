/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.ads;

public interface AdsProvider {
    Object getAdsConfig();
    void start();
    void destroyAdsManager();
    void resume();
    void pause();
    void contentCompleted();
    PKAdInfo getAdInfo();
    AdEvent.Type getAdPluginState();
    boolean isAdDisplayed();
    boolean isAdPaused();
    boolean isAdRequested();
    boolean isAllAdsCompleted();
    boolean isAdError();
    long getDuration();
    long getCurrentPosition();
    void setAdProviderListener(AdEnabledPlayerController adEnabledPlayerController);
    void removeAdProviderListener();
    void skipAd();
    void openLearnMore();
    void openComapanionAdLearnMore();
    void screenOrientationChanged(boolean isFullScreen);
    void volumeKeySilent(boolean isMute);


}
