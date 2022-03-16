/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.ads.PKAdInfo;

import java.util.Collections;
import java.util.List;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdInfo implements PKAdInfo {

    private String adDescription;
    private long adDuration;
    private long adPlayHead;

    private String adTitle;
    private String streamId;
    private boolean isAdSkippable;
    private long skipTimeOffset;
    private String adContentType;
    private String adId;
    private String adSystem;
    private String creativeId;
    private String creativeAdId;
    private String advertiserName;
    private String dealId;
    private String surveyUrl;
    private String traffickingParams;
    private List<String> adWrapperCreativeIds;
    private List<String> adWrapperIds;
    private List<String> adWrapperSystems;
    private int adHeight;
    private int adWidth;
    private int mediaBitrate;
    private int totalAdsInPod;
    private int adIndexInPod;
    private int podIndex;
    private int podCount;
    private boolean isBumper;
    private long adPodTimeOffset;

    public AdInfo(String adDescription, long adDuration, long adPlayHead, String adTitle,
                  boolean isAdSkippable, long skipTimeOffset, String adContentType,
                  String adId, String adSystem, String creativeId, String creativeAdId,
                  String advertiserName, String dealId, String surveyUrl,
                  String traffickingParams, List<String> adWrapperCreativeIds,
                  List<String> adWrapperIds, List<String> adWrapperSystems,
                  int adHeight, int adWidth, int mediaBitrate,
                  int totalAdsInPod, int adIndexInPod, int currentPodIndex, int podCount,
                  boolean isBumper, long adPodTimeOffset) {

        this.adDescription = adDescription;
        this.adDuration = adDuration;
        this.adPlayHead = adPlayHead;
        this.adTitle = adTitle;
        this.isAdSkippable = isAdSkippable;
        this.skipTimeOffset = skipTimeOffset;
        this.adContentType = adContentType;
        this.adId = adId;
        this.adSystem = adSystem;
        this.creativeId = creativeId;
        this.creativeAdId = creativeAdId;
        this.advertiserName = advertiserName;
        this.dealId = dealId;
        this.surveyUrl = surveyUrl;
        this.traffickingParams = traffickingParams;
        this.adWrapperCreativeIds = adWrapperCreativeIds;
        this.adWrapperIds = adWrapperIds;
        this.adWrapperSystems = adWrapperSystems;
        this.adHeight = adHeight;
        this.adWidth = adWidth;
        this.mediaBitrate = mediaBitrate;
        this.totalAdsInPod = totalAdsInPod;
        this.adIndexInPod = adIndexInPod;
        this.podIndex = currentPodIndex;
        this.podCount = podCount;
        this.isBumper = isBumper;
        this.adPodTimeOffset = adPodTimeOffset;
    }

    @Override
    public String getAdContentType() {
        return adContentType;
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
    public int getTotalAdsInPod() {
        return totalAdsInPod;
    }

    @Override
    public int getAdIndexInPod() {
        return adIndexInPod;
    }

    @Override
    public int getPodIndex() {
        return podIndex;
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

        if (adPodTimeOffset > 0) {
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
    public int getPodCount() {
        return podCount;
    }

    @Override
    public String getAdSystem() {
        return adSystem;
    }

    public String getCreativeId() {
        return creativeId;
    }

    public String getCreativeAdId() {
        return creativeAdId;
    }

    public String getAdvertiserName() {
        return advertiserName;
    }

    public String getDealId() {
        return dealId;
    }

    public String getSurveyUrl() {
        return surveyUrl;
    }

    public String getTraffickingParams() {
        return traffickingParams;
    }

    public List<String> getAdWrapperCreativeIds() {
        return adWrapperCreativeIds != null ? adWrapperCreativeIds : Collections.emptyList();
    }

    public List<String> getAdWrapperIds() {
        return adWrapperIds != null ? adWrapperIds : Collections.emptyList();
    }

    public List<String> getAdWrapperSystems() {
        return adWrapperSystems != null ? adWrapperSystems : Collections.emptyList();
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
    public boolean isBumper() {
        return isBumper;
    }

    @Override
    public long getAdPodTimeOffset() {
        return adPodTimeOffset;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setAdPlayHead(long adPlayHead) {
        this.adPlayHead = adPlayHead;
    }

    public void setMediaBitrate(int mediaBitrate) {
        this.mediaBitrate = mediaBitrate;
    }

    public void setAdWidth(int adWidth) {
        this.adWidth = adWidth;
    }

    public void setAdHeight(int adHeight) {
        this.adHeight = adHeight;
    }

    public void setAdSkipOffset(long skipTimeOffset) {
        this.skipTimeOffset = skipTimeOffset;
        if (this.skipTimeOffset > 0) {
            isAdSkippable = true;
        }
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public long getSkipTimeOffset() {
        return skipTimeOffset;
    }

    public int getMediaBitrate() {
        return mediaBitrate;
    }

    @Override
    public String toString() {
        String adType;
        if (adPodTimeOffset > 0) {
            adType = "Mid-Roll";
        } else if (adPodTimeOffset < 0) {
            adType = "Post-Roll";
        } else {
            adType = "Pre-Roll";
        }
        return "AdType=" + adType + " adTimeOffset=" + adPodTimeOffset + " adTitle=" + adTitle + " adDuration=" + adDuration + " isBumper=" + isBumper + " contentType= " + adContentType + " adBitrate=" + mediaBitrate +
               " adWidth=" + adWidth + " adHeight=" + adHeight + " adCount=" + adIndexInPod + "/" + totalAdsInPod + " podCount=" + podIndex + "/" + podCount;
    }
}
