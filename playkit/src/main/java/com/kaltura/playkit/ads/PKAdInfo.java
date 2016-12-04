package com.kaltura.playkit.ads;

public interface PKAdInfo {
    public String getDescription();
    String getAdId();
    String getAdSystem();
    boolean isLinear();
    boolean isSkippable();
    String getTitle();
    String getContentType();
    int getAdWidth();
    int getAdHeight();
    String getTraffickingParameters();
    long getDuration();
}
