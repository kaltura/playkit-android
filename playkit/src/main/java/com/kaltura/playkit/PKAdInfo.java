package com.kaltura.playkit;

import com.google.ads.interactivemedia.v3.api.AdPodInfo;

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
    double getDuration();
    AdPodInfo getAdPodInfo();

    //String[] getAdWrapperIds();
    //String[] getAdWrapperSystems();
    //Set<UiElement> getUiElements();
}
