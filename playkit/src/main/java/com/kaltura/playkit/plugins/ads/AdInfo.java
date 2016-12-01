package com.kaltura.playkit.plugins.ads;

import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.kaltura.playkit.PKAdInfo;

import java.util.List;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdInfo implements PKAdInfo{

    private String adDescriptionc;
    private double  adDuration;
    private String  adTitle;
    private boolean isAdSkippable;
    private String  contnentType;
    private String  adId;
    private String  adSystem;
    private int     adHeight;
    private int     adWidth;
    private String  traffickingParameters;
    private AdPodInfo adPodInfo;
    private List<Float> adCuePoints;

    public AdInfo(String adDescriptionc, double adDuration, String adTitle, boolean isAdSkippable, String contnentType,
                  String adId, String adSystem, int adHeight, int adWidth, String traffickingParameters, AdPodInfo adPodInfo,List<Float> adCuePoints) {
        this.adDescriptionc = adDescriptionc;
        this.adDuration = adDuration;
        this.adTitle = adTitle;
        this.isAdSkippable = isAdSkippable;
        this.contnentType = contnentType;
        this.adId = adId;
        this.adSystem = adSystem;
        this.adHeight = adHeight;
        this.adWidth = adWidth;
        this.traffickingParameters = traffickingParameters;
        this.adPodInfo = adPodInfo;
        this.adCuePoints = adCuePoints;
    }

    public String getAdDescriptionc() {
        return adDescriptionc;
    }

    public String getContnentType() {
        return contnentType;
    }

    public String getAdTitle() {
        return adTitle;
    }

    public void setAdTitle(String adTitle) {
        this.adTitle = adTitle;
    }

    public boolean isAdSkippable() {
        return isAdSkippable;
    }

    public String getContentType() {
        return contnentType;
    }

    @Override
    public int getAdWidth() {
        return adWidth;
    }

    @Override
    public int getAdHeight() {
        return adHeight;
    }

    @Override
    public String getTraffickingParameters() {
        return traffickingParameters;
    }

    @Override
    public double getDuration() {
        return adDuration;
    }

    @Override
    public String getDescription() {
        return null;
    }

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public String getAdSystem() {
        return adSystem;
    }

    @Override
    public boolean isLinear() {
        return false;
    }

    @Override
    public boolean isSkippable() {
        return false;
    }

    @Override
    public String getTitle() {
        return null;
    }

    public AdPodInfo getAdPodInfo() {
        return adPodInfo;
    }

    public List<Float> getAdCuePoints() {
        return adCuePoints;
    }
}
