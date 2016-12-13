package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.ads.PKAdInfo;

import java.util.List;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdInfo implements PKAdInfo{

    private String  adDescription;
    private double  adDuration;
    private String  adTitle;
    private boolean isAdSkippable;
    private String  contnentType;
    private String  adId;
    private String  adSystem;
    private int     adHeight;
    private int     adWidth;
    int             adPodCount;
    private List<Float> adCuePoints;

    public AdInfo(String adDescription, double adDuration, String adTitle, boolean isAdSkippable, String contnentType,
                  String adId, String adSystem, int adHeight, int adWidth, int adPodCount, List<Float> adCuePoints) {
        this.adDescription = adDescription;
        this.adDuration    = adDuration;
        this.adTitle       = adTitle;
        this.isAdSkippable = isAdSkippable;
        this.contnentType  = contnentType;
        this.adId        = adId;
        this.adSystem    = adSystem;
        this.adHeight    = adHeight;
        this.adWidth     = adWidth;
        this.adPodCount  = adPodCount;
        this.adCuePoints = adCuePoints;
    }


    @Override
    public String getAdContentType() {
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
    public double getAdDuration() {
        return adDuration;
    }

    @Override
    public String getAdDescription() {
        return adDescription;
    }

    @Override
    public String getAdId() {
        return adId;
    }

    @Override
    public int getAdPodCount() {
        return adPodCount;
    }

    @Override
    public String getAdSystem() {
        return adSystem;
    }

    @Override
    public boolean isAdSkippable() {
        return isAdSkippable;
    }

    @Override
    public String getAdTitle() {
        return adTitle;
    }

    @Override
    public List<Float> getAdCuePoints() {
        return adCuePoints;
    }

    @Override
    public String toString() {
        return "adTitle=" + adTitle + " adDuration=" + adDuration + " contentType=" + contnentType + " podCount = " + adPodCount;
    }
}
