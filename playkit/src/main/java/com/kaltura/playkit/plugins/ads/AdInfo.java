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

    public void setAdDescriptionc(String adDescriptionc) {
        this.adDescriptionc = adDescriptionc;
    }

    public String getContnentType() {
        return contnentType;
    }

    public void setContnentType(String contnentType) {
        this.contnentType = contnentType;
    }



    public void setAdDuration(double adDuration) {
        this.adDuration = adDuration;
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

    public void setAdSkippable(boolean adSkippable) {
        isAdSkippable = adSkippable;
    }

    public String getContentType() {
        return contnentType;
    }

    public void setTraffickingParameters(String traffickingParameters) {
        this.traffickingParameters = traffickingParameters;
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

    public void setContentType(String contnentType) {
        this.contnentType = contnentType;
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

    public void setAdSystem(String adSystem) {
        this.adSystem = adSystem;
    }

    public void setAdHeight(int adHeight) {
        this.adHeight = adHeight;
    }

    public void setAdWidth(int adWidth) {
        this.adWidth = adWidth;
    }

    public AdPodInfo getAdPodInfo() {
        return adPodInfo;
    }

    public void setAdPodInfo(AdPodInfo adPodInfo) {
        this.adPodInfo = adPodInfo;
    }

    public List<Float> getAdCuePoints() {
        return adCuePoints;
    }

    public void setAdCuePoints(List<Float> adCuePoints) {
        this.adCuePoints = adCuePoints;
    }
}
