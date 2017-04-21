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
    int      getAdPodCount();
    int      getAdPodPosition();
    long     getAdPodTimeOffset();
    long     getAdDuration();
    long     getAdPlayHead();
    AdPositionType getAdPositionType();
}
