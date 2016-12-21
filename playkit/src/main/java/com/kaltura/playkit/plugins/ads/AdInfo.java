package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.ads.PKAdInfo;

import java.util.List;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdInfo implements PKAdInfo {

    private String  adDescription;
    private long    adDuration;

    private String  adTitle;
    private boolean isAdSkippable;
    private String  adContnentType;
    private String  adId;
    private String  adSystem;
    private int     adHeight;
    private int     adWidth;
    private int     adPodCount;
    private int     adPodPosition;
    private long    adPodTimeOffset;
    private String  clickThroughUrl;
    private List<Long> adCuePoints;

    public AdInfo(String adDescription, long adDuration, String adTitle, boolean isAdSkippable, String adContnentType,
                  String adId, String adSystem, int adHeight, int adWidth,
                  int adPodCount, int adPodPosition, long adPodTimeOffset, String clickThroughUrl, List<Long> adCuePoints) {

        this.adDescription = adDescription;
        this.adDuration    = adDuration;
        this.adTitle       = adTitle;
        this.isAdSkippable = isAdSkippable;
        this.adContnentType  = adContnentType;
        this.adId            = adId;
        this.adSystem        = adSystem;
        this.adHeight        = adHeight;
        this.adWidth         = adWidth;
        this.adPodCount      = adPodCount;
        this.adPodPosition   = adPodPosition;
        this.adPodTimeOffset = adPodTimeOffset;
        this.clickThroughUrl = clickThroughUrl;
        this.adCuePoints = adCuePoints;
    }


    @Override
    public String getAdContentType() {
        return adContnentType;
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
    public long getAdDuration() {
        return adDuration;
    }

    @Override
    public String getClickThroughUrl() {
        return clickThroughUrl;
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
    public int getAdPodPosition() {
        return adPodPosition;
    }

    @Override
    public long getAdPodTimeOffset() {
        return adPodTimeOffset;
    }

    @Override
    public List<Long> getAdCuePoints() {
        return adCuePoints;
    }

    @Override
    public String toString() {
        String adType = "";
        if (adPodTimeOffset > 0 ) {
            adType = "Mid-Roll";
        } else if (adPodTimeOffset < 0) {
            adType = "Post-Roll";
        } else {
            adType = "Pre-Roll";
        }
        return "AdType=" + adType + " adTimeOffset=" + adPodTimeOffset + " adTitle=" + adTitle + " adDuration=" + adDuration + " contentType=" + adContnentType + " podCount = " + adPodPosition + "/" + adPodCount;
    }
}
