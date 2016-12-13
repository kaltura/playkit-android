package com.kaltura.playkit.ads;

import java.util.List;

public interface PKAdInfo {
    public String getAdDescription();
    String getAdId();
    String getAdSystem();
    boolean isAdSkippable();
    String getAdTitle();
    String getAdContentType();
    int getAdWidth();
    int getAdHeight();
    int getAdPodCount();
    int getAdPosition();
    double getAdPodTimeOffset();
    double getAdDuration();
    List<Float> getAdCuePoints();
}
