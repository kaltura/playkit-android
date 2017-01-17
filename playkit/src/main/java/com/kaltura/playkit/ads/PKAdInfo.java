package com.kaltura.playkit.ads;

public interface PKAdInfo {

    public static int PRE_ROLL  = 0;
    public static int MID_ROLL  = 1;
    public static int POST_ROLL = 2;

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
    int      getAdType();
}
