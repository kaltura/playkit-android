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

import com.kaltura.playkit.plugins.ads.AdPositionType;

public interface PKAdInfo {

    String   getAdDescription();
    String   getAdId();
    String   getAdSystem();
    boolean  isAdSkippable();
    String   getAdTitle();
    String   getAdContentType();
    int      getAdWidth();
    int      getAdHeight();
    int      getTotalAdsInPod();
    int      getAdIndexInPod();
    int      getPodCount();
    int      getPodIndex();
    boolean  isBumper();
    long     getAdPodTimeOffset();
    long     getAdDuration();
    long     getAdPlayHead();
    AdPositionType getAdPositionType();
}
