package com.kaltura.playkit;

import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdInfo;

@SuppressWarnings("unused")
public interface AdsListener extends PKListener {

    default void onAdFirstPlay() {}

    default void onAdDisplayedAfterContentPause() {}

    default void onCompleted() {}

    default void onFirstQuartile() {}

    default void onMidpoint() {}

    default void onThirdQuartile() {}

    default void onClicked() {}

    default void onTapped() {}

    default void onIconTapped() {}

    default void onAdBreakReady() {}

    default void onAdProgress() {}

    default void onAdBreakStarted() {}

    default void onAdBreakEnded() {}

    default void onAdBreakIgnored() {}

    default void onContentPauseRequested() {}

    default void onContentResumeRequested() {}

    default void onAllAdsCompleted() {}

    default void onAdLoadTimeoutTimerStarted() {}

    default void onAdLoaded(AdInfo adInfo) {}

    default void onAdStarted(AdInfo adInfo) {}

    default void onAdPaused(AdInfo adInfo) {}

    default void onAdResumed(AdInfo adInfo) {}

    default void onAdSkipped(AdInfo adInfo) {}

    default void onAdCuePointsUpdate(AdCuePoints cuePoints) {}

    default void onAdPlayHead(long adPlayHead) {}

    default void onAdRequested(String adTagUrl) {}

    default void onAdBufferStart(long adPosition) {}

    default void onAdBufferEnd(long adPosition) {}

    default void onError(PKError error) {}
}
