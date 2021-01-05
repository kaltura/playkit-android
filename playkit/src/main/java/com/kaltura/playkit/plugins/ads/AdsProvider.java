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

package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.ads.PKAdPluginType;
import com.kaltura.playkit.ads.PKAdProviderListener;


public interface AdsProvider {
    void start();

    void destroyAdsManager();

    void resume();

    void pause();

    default void setVolume(float volume) {}

    void contentCompleted();

    PKAdInfo getAdInfo();

    AdCuePoints getCuePoints();

    boolean isAdDisplayed();

    boolean isAdPaused();

    default boolean isForceSinglePlayerRequired() {return false;}

    boolean isAdRequested();

    boolean isAllAdsCompleted();

    boolean isAdError();

    long getDuration();

    long getCurrentPosition();

    Long getPlaybackStartPosition();

    boolean isAlwaysStartWithPreroll();
    
    default long getFakePlayerPosition(long realPlayerPosition) { return 0; }

    default long getFakePlayerDuration(long duration) { return 0; }

    void setAdProviderListener(PKAdProviderListener adProviderListener);

    void setAdRequested(boolean isAdRequested);

    void resetPluginFlags();

    void removeAdProviderListener();

    void skipAd();

    default void seekTo(long position) {}

    default PKAdPluginType getAdPluginType() { return PKAdPluginType.client; }

    boolean isContentPrepared();
}
