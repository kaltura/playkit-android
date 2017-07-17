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

package com.kaltura.playkit.plugins.youbora;

import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.ads.PKAdErrorType;
import com.kaltura.playkit.PKError;
import com.npaw.youbora.adnalyzers.AdnalyzerGeneric;
import com.npaw.youbora.plugins.PluginGeneric;
import com.npaw.youbora.youboralib.BuildConfig;
import com.npaw.youbora.youboralib.utils.YBLog;

import static com.kaltura.playkit.PlayerEvent.Type.STATE_CHANGED;

/**
 * @hide
 */

class YouboraAdManager extends AdnalyzerGeneric {
    private static final PKLog log = PKLog.get("YouboraAdManager");

    private boolean isFirstPlay = true;
    private boolean isBuffering = false;
    private MessageBus messageBus;
    private double adBitrate = -1;
    private AdInfo currentAdInfo;

    private String lastReportedAdResource;
    private String lastReportedAdTitle;
    private Double lastReportedAdPlayhead;
    private Double lastReportedAdDuration;

    YouboraAdManager(PluginGeneric plugin, MessageBus messageBus) {
        super(plugin);
        this.adnalyzerVersion = BuildConfig.VERSION_NAME + "-" + getAdPlayerVersion();
        this.messageBus = messageBus;

        this.messageBus.listen(mEventListener, STATE_CHANGED);
        this.messageBus.listen(mEventListener, (Enum[]) AdEvent.Type.values());
    }

    private void onEvent(PlayerEvent.StateChanged event) {
        log.d(event.newState.toString());
        switch (event.newState) {
            case READY:
                if (isBuffering) {
                    isBuffering = false;
                    bufferedAdHandler();
                }
                break;
            case BUFFERING:
                isBuffering = true;
                bufferingAdHandler();
                break;
            default:
                break;
        }

        messageBus.post(new YouboraEvent.YouboraReport(event.eventType().name()));
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {

            log.d("on event " + event.eventType());

            if (event instanceof AdEvent) {
                log.d("AdManager: " + ((AdEvent) event).type.toString());
                switch (((AdEvent) event).type) {
                    case AD_REQUESTED:
                        lastReportedAdResource = ((AdEvent.AdRequestedEvent) event).adTagUrl;
                        log.d("lastReportedAdResource: " + lastReportedAdResource);
                        break;
                    case LOADED:
                        if (isFirstPlay) {
                            isFirstPlay = false;
                            plugin.playHandler();
                        }
                        currentAdInfo = ((AdEvent.AdLoadedEvent) event).adInfo;
                        populateAdValues();

                        playAdHandler();
                        break;
                    case STARTED:
                        currentAdInfo = ((AdEvent.AdStartedEvent) event).adInfo;
                        lastReportedAdPlayhead = Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);

                        joinAdHandler();
                        break;
                    case PAUSED:
                        currentAdInfo = ((AdEvent.AdPausedEvent) event).adInfo;
                        lastReportedAdPlayhead = Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);

                        pauseAdHandler();
                        break;
                    case RESUMED:
                        currentAdInfo = ((AdEvent.AdResumedEvent) event).adInfo;
                        if (isFirstPlay) {
                            isFirstPlay = false;
                            playAdHandler();
                            joinAdHandler();
                            populateAdValues();
                        }

                        lastReportedAdPlayhead = Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);

                        resumeAdHandler();
                        break;
                    case COMPLETED:
                        lastReportedAdPlayhead = lastReportedAdDuration;
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
                        endedAdHandler();
                        break;
                    case AD_BREAK_IGNORED:
                        endedAdHandler();
                        break;
                    case CONTENT_RESUME_REQUESTED:
                        endedAdHandler();
                        break;
                    case SKIPPED:
                        currentAdInfo = ((AdEvent.AdSkippedEvent) event).adInfo;
                        lastReportedAdPlayhead = Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
                        skipAdHandler();
                        break;
                    case ERROR:
                        AdEvent.Error errorEvent = (AdEvent.Error) event;
                        handleAdError(errorEvent.error);
                        break;
                    case CLICKED:
                        log.d("learn more clicked");
                        //We are not sending this event to youbora,
                        //so prevent it from dispatching through YouboraEvent.YouboraReport.
                        return;
                    case PLAY_HEAD_CHANGED:
                        lastReportedAdPlayhead = Long.valueOf(((AdEvent.AdPlayHeadEvent) event).adPlayHead).doubleValue();
                        //We are not sending this event to youbora,
                        //so prevent it from dispatching through YouboraEvent.YouboraReport.
                        return;
                    case AD_PROGRESS:
                        //We are not sending this event to youbora,
                        //so prevent it from dispatching through YouboraEvent.YouboraReport.
                        return;
                    default:
                        break;
                }

                messageBus.post(new YouboraEvent.YouboraReport(event.eventType().name()));
            }
        }
    };

    public void startMonitoring(Object player) {
        log.d("startMonitoring");
        super.startMonitoring(player);
        isFirstPlay = true;
    }

    public void stopMonitoring() {
        log.d("stopMonitoring");

        super.stopMonitoring();
        if (plugin != null) {
            plugin.endedHandler();
        }
        this.messageBus.remove(mEventListener, STATE_CHANGED);
        messageBus.remove(mEventListener, (Enum[]) AdEvent.Type.values());
        messageBus.remove(mEventListener, (PlayerEvent.Type.ERROR));

    }

    public void setAdBitrate(Double bitrate) {
        this.adBitrate = bitrate;
    }

    @Override
    public Double getAdBitrate() {
        return this.adBitrate;
    }

    @Override
    public String getAdTitle() {
        log.d("getAdTitle ");
        return lastReportedAdTitle != null ? lastReportedAdTitle : "No Info";
    }

    @Override
    public Double getAdDuration() {
        return currentAdInfo != null ? (Long.valueOf(currentAdInfo.getAdDuration() / Consts.MILLISECONDS_MULTIPLIER).doubleValue()) : 0.0D;
    }

    @Override
    public String getAdPlayerVersion() {
        log.d("getAdPlayerVersion " + PlayKitManager.CLIENT_TAG);

        return Consts.KALTURA + "-" + PlayKitManager.CLIENT_TAG;
    }

    @Override
    public Double getAdPlayhead() {
        log.d("getAdPlayhead = " + lastReportedAdPlayhead);
        return lastReportedAdPlayhead;
    }

    @Override
    public String getAdPosition() {
        String adPosition = "unknown";

        if (currentAdInfo == null) {
            return adPosition;
        }

        switch (currentAdInfo.getAdPositionType()) {
            case PRE_ROLL:
                adPosition = "pre";
                break;
            case MID_ROLL:
                adPosition = "mid";
                break;
            case POST_ROLL:
                adPosition = "post";
                break;
            default:
                adPosition = "unknown";
        }
        log.d("adPosition = " + adPosition);
        return adPosition;
    }

    @Override
    public String getAdResource() {
        log.d("getAdResource = " + lastReportedAdResource);
        return lastReportedAdResource;
    }

    private void populateAdValues() {
        lastReportedAdDuration = Long.valueOf(currentAdInfo.getAdDuration() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
        lastReportedAdTitle = currentAdInfo.getAdTitle();
        lastReportedAdPlayhead = Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
        log.d("lastReportedAdDuration: " + lastReportedAdDuration);
        log.d("lastReportedAdTitle: " + lastReportedAdTitle);
        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
    }

    void resetAdValues() {
        isFirstPlay = true;
        currentAdInfo = null;
        lastReportedAdDuration = super.getAdDuration();
        lastReportedAdTitle = super.getAdTitle();
        lastReportedAdPlayhead = super.getAdPlayhead();
    }

    public void onUpdateConfig() {
        resetAdValues();
        adBitrate = -1;
        lastReportedAdResource = super.getAdResource();
    }

    private void handleAdError(PKError error) {

        PKAdErrorType adErrorType = (PKAdErrorType) error.errorType;
        switch (adErrorType) {
            case QUIET_LOG_ERROR:
                log.d("QUIET_LOG_ERROR. Avoid sending to Youbora.");
                return;
            default:
                YBLog.debug("onAdError " + adErrorType.name());
                endedAdHandler();
        }

        messageBus.post(new YouboraEvent.YouboraReport(adErrorType.name()));
    }
}
