package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.ads.PKAdInfo;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdInfo implements PKAdInfo {

    private String  adDescription;
    private long    adDuration;
    private long    adPlayHead;

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

    public AdInfo(String adDescription, long adDuration, long adPlayHead, String adTitle, boolean isAdSkippable, String adContnentType,
                  String adId, String adSystem, int adHeight, int adWidth,
                  int adPodCount, int adPodPosition, long adPodTimeOffset) {

        this.adDescription = adDescription;
        this.adDuration    = adDuration;
        this.adPlayHead    = adPlayHead;
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
    public long getAdPlayHead() {
        return adPlayHead;
    }

    @Override
    public AdPositionType getAdPositionType() {

        if (adPodTimeOffset > 0 ) {
            return AdPositionType.MID_ROLL;
        } else if (adPodTimeOffset < 0) {
            return AdPositionType.POST_ROLL;
        } else {
            return AdPositionType.PRE_ROLL;
        }
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

    public void setAdPlayHead(long adPlayHead) {
        this.adPlayHead = adPlayHead;
    }

    public String getAdContnentType() {
        return adContnentType;

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
