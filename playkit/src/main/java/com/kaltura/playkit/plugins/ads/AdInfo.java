package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.ads.PKAdInfo;

import java.util.List;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdInfo implements PKAdInfo{

    private String  adDescription;
    private long    adDuration;
    private String  adTitle;
    private boolean isAdSkippable;
    private String  contnentType;
    private String  adId;
    private String  adSystem;
    private int     adHeight;
    private int     adWidth;
    int             podCount;
    private List<Float> adCuePoints;

    public AdInfo(String adDescription, long adDuration, String adTitle, boolean isAdSkippable, String contnentType,
                  String adId, String adSystem, int adHeight, int adWidth, int podCount, List<Float> adCuePoints) {
        this.adDescription = adDescription;
        this.adDuration = adDuration;
        this.adTitle = adTitle;
        this.isAdSkippable = isAdSkippable;
        this.contnentType = contnentType;
        this.adId = adId;
        this.adSystem = adSystem;
        this.adHeight = adHeight;
        this.adWidth = adWidth;
        this.podCount = podCount;
        this.adCuePoints = adCuePoints;
    }

    public String getAdDescription() {
        return adDescription;
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
    public long getDuration() {
        return adDuration;
    }

    @Override
    public String getDescription() {
        return null;
    }

    public String getAdId() {
        return adId;
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

    public List<Float> getAdCuePoints() {
        return adCuePoints;
    }

    @Override
    public int getPodCount() {
        return podCount;
    }

    @Override
    public String toString() {
        return "adTitle=" + adTitle + " adDuration=" + adDuration + " contentType=" + contnentType + " podCount = " + podCount;
    }
}
