package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.AdsListener;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKError;

public class AdsListenerAdapter implements AdsListener {
    private final MessageBus messageBus;

    public AdsListenerAdapter(MessageBus messageBus) {
        this.messageBus = messageBus;
    }

    @Override
    public void onAdFirstPlay() {
        messageBus.post(new AdEvent(AdEvent.Type.AD_FIRST_PLAY));
    }

    @Override
    public void onAdDisplayedAfterContentPause() {
        messageBus.post(new AdEvent(AdEvent.Type.AD_DISPLAYED_AFTER_CONTENT_PAUSE));
    }

    @Override
    public void onCompleted() {
        messageBus.post(new AdEvent(AdEvent.Type.COMPLETED));
    }

    @Override
    public void onFirstQuartile() {
        messageBus.post(new AdEvent(AdEvent.Type.FIRST_QUARTILE));
    }

    @Override
    public void onMidpoint() {
        messageBus.post(new AdEvent(AdEvent.Type.MIDPOINT));
    }

    @Override
    public void onThirdQuartile() {
        messageBus.post(new AdEvent(AdEvent.Type.THIRD_QUARTILE));
    }

    @Override
    public void onClicked() {
        messageBus.post(new AdEvent(AdEvent.Type.CLICKED));
    }

    @Override
    public void onTapped() {
        messageBus.post(new AdEvent(AdEvent.Type.TAPPED));
    }

    @Override
    public void onIconTapped() {
        messageBus.post(new AdEvent(AdEvent.Type.ICON_TAPPED));
    }

    @Override
    public void onAdBreakReady() {
        messageBus.post(new AdEvent(AdEvent.Type.AD_BREAK_READY));
    }

    @Override
    public void onAdProgress() {
        messageBus.post(new AdEvent(AdEvent.Type.AD_PROGRESS));
    }

    @Override
    public void onAdBreakStarted() {
        messageBus.post(new AdEvent(AdEvent.Type.AD_BREAK_STARTED));
    }

    @Override
    public void onAdBreakEnded() {
        messageBus.post(new AdEvent(AdEvent.Type.AD_BREAK_ENDED));
    }

    @Override
    public void onAdBreakIgnored() {
        messageBus.post(new AdEvent(AdEvent.Type.AD_BREAK_IGNORED));
    }

    @Override
    public void onContentPauseRequested() {
        messageBus.post(new AdEvent(AdEvent.Type.CONTENT_PAUSE_REQUESTED));
    }

    @Override
    public void onContentResumeRequested() {
        messageBus.post(new AdEvent(AdEvent.Type.CONTENT_RESUME_REQUESTED));
    }

    @Override
    public void onAllAdsCompleted() {
        messageBus.post(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
    }

    @Override
    public void onAdLoadTimeoutTimerStarted() {
        messageBus.post(new AdEvent(AdEvent.Type.AD_LOAD_TIMEOUT_TIMER_STARTED));
    }

    @Override
    public void onAdLoaded(AdInfo adInfo) {
        messageBus.post(new AdEvent.AdLoadedEvent(adInfo));
    }

    @Override
    public void onAdStarted(AdInfo adInfo) {
        messageBus.post(new AdEvent.AdStartedEvent(adInfo));
    }

    @Override
    public void onAdPaused(AdInfo adInfo) {
        messageBus.post(new AdEvent.AdPausedEvent(adInfo));
    }

    @Override
    public void onAdResumed(AdInfo adInfo) {
        messageBus.post(new AdEvent.AdResumedEvent(adInfo));
    }

    @Override
    public void onAdSkipped(AdInfo adInfo) {
        messageBus.post(new AdEvent.AdSkippedEvent(adInfo));
    }

    @Override
    public void onAdCuePointsUpdate(AdCuePoints cuePoints) {
        messageBus.post(new AdEvent.AdCuePointsUpdateEvent(cuePoints));
    }

    @Override
    public void onAdPlayHead(long adPlayHead) {
        messageBus.post(new AdEvent.AdPlayHeadEvent(adPlayHead));
    }

    @Override
    public void onAdRequested(String adTagUrl) {
        messageBus.post(new AdEvent.AdRequestedEvent(adTagUrl));
    }

    @Override
    public void onAdBufferStart(long adPosition) {
        messageBus.post(new AdEvent.AdBufferStart(adPosition));
    }

    @Override
    public void onAdBufferEnd(long adPosition) {
        messageBus.post(new AdEvent.AdBufferEnd(adPosition));
    }

    @Override
    public void onAdPlaybackInfoUpdated(int width, int height, int bitrate) {
        messageBus.post(new AdEvent.AdPlaybackInfoUpdated(width, height, bitrate));
    }

    @Override
    public void onSkippableStateChanged() {
        messageBus.post(new AdEvent(AdEvent.Type.SKIPPABLE_STATE_CHANGED));
    }

    @Override
    public void onError(PKError error) {
        messageBus.post(new AdEvent.Error(error));
    }
}
