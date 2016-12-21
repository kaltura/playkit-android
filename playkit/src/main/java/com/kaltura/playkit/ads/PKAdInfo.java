package com.kaltura.playkit.ads;

import java.util.List;

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
    String   getClickThroughUrl();
    List<Long> getAdCuePoints();
}
