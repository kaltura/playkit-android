package com.kaltura.playkit.ads;

public interface PKAdInfo {
    public String getAdDescription();
    String getAdId();
    String getAdSystem();
    boolean isAdSkippable();
    String getAdTitle();
    String getAdContentType();
    int  getAdWidth();
    int  getAdHeight();
    int  getAdPodCount();
    int  getAdPodPosition();
    long getAdPodTimeOffset();
    long getAdDuration();
}
