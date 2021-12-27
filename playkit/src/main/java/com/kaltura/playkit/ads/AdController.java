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

import androidx.annotation.NonNull;

import com.kaltura.playkit.PKController;
import com.kaltura.playkit.plugins.ads.AdCuePoints;

import java.util.List;

/**
 * Created by Noam Tamim @ Kaltura on 14/12/2016.
 */
public interface AdController extends PKController {
    void skip();

    void play();

    void pause();

    void seekTo(long position);

    default void setVolume(float volume) {}

    long getAdCurrentPosition();

    long getAdDuration();

    boolean isAdDisplayed();

    boolean isAdPlaying();

    boolean isAdError();

    boolean isAllAdsCompleted();

    PKAdInfo getAdInfo();

    AdCuePoints getCuePoints();

    PKAdPluginType getAdPluginType();

    // APIs for internal use (AdLayout)
    void setAdvertisingConfig(boolean isConfigured, @NonNull AdType adType, IMAEventsListener imaEventsListener);

    void advertisingPlayAdNow(String adTag);

    void advertisingSetCuePoints(List<Long> cuePoints);

    void advertisingSetAdInfo(PKAdvertisingAdInfo pkAdvertisingAdInfo);

    void advertisingPreparePlayer();
}
