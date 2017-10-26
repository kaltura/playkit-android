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
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.PKAdEndedReason;
import com.kaltura.playkit.ads.PKAdErrorType;
import com.kaltura.playkit.ads.AdEvent;
import com.kaltura.playkit.ads.AdPositionType;
import com.kaltura.playkit.utils.Consts;
import com.npaw.youbora.adnalyzers.AdnalyzerGeneric;
import com.npaw.youbora.plugins.PluginGeneric;
import com.npaw.youbora.youboralib.BuildConfig;
import com.npaw.youbora.youboralib.utils.YBLog;

import static com.kaltura.playkit.PlayerEvent.Type.STATE_CHANGED;
import static com.kaltura.playkit.ads.AdPositionType.UNKNOWN;

/**
 * @hide
 */

class YouboraAdManager extends AdnalyzerGeneric {
    private static final PKLog log = PKLog.get("YouboraAdManager");

    private boolean isFirstPlay = true;
    private boolean isBuffering = false;
    private MessageBus messageBus;
    private double adBitrate = -1;
    private AdPositionType adPositionType = AdPositionType.UNKNOWN;


    private String lastReportedAdResource;
    private String lastReportedAdTitle;
    private Double lastReportedAdPlayhead;
    private Double lastReportedAdDuration;

    private String currentReportedAdTitle;
    private Double currentReportedAdDuration;
    private Double currentReportedAdPlayhead;

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

            if (event instanceof AdEvent) {
                if (((AdEvent) event).type != AdEvent.Type.AD_POSITION_UPDATED) {
                    log.d("AdManager: " + ((AdEvent) event).type.toString());
                }
                switch (((AdEvent) event).type) {
                    case AD_REQUESTED:
                        lastReportedAdResource = ((AdEvent.AdRequestedEvent) event).adTagUrl;
                        log.d("lastReportedAdResource: " + lastReportedAdResource);
                        break;
                    case AD_LOADED:
                        log.d("Youbora AD_LOADED");
                        if (isFirstPlay) {
                            lastReportedAdPlayhead = 0.0;
                            currentReportedAdPlayhead = 0.0;
                            isFirstPlay = false;
                            plugin.playHandler();
                        }

                        AdEvent.AdLoadedEvent adLoadedEvent = (AdEvent.AdLoadedEvent) event;
                        adPositionType = adLoadedEvent.adInfo.getAdPositionType();
                        lastReportedAdDuration = Long.valueOf(adLoadedEvent.adInfo.getAdDuration() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                        lastReportedAdTitle = adLoadedEvent.adInfo.getAdTitle();

                        currentReportedAdDuration = lastReportedAdDuration;
                        currentReportedAdTitle = lastReportedAdTitle;
                        log.d("lastReportedAdDuration: " + lastReportedAdDuration);
                        log.d("lastReportedAdTitle: " + lastReportedAdTitle);
                        playAdHandler();
                        break;
                    case AD_STARTED:
                        log.d("Youbora AD_STARTED");
                        joinAdHandler();
                        break;
                    case AD_PAUSED:
                        pauseAdHandler();
                        break;
                    case AD_RESUMED:
                        if (isFirstPlay) {
                            isFirstPlay = false;
                            populateAdValues();
                            playAdHandler();
                            joinAdHandler();
                        }
                        resumeAdHandler();
                        break;
                    case AD_ENDED:
                        AdEvent.AdEndedEvent adEndedEvent = (AdEvent.AdEndedEvent) event;
                        if (((AdEvent.AdEndedEvent) event).adEndedReason == PKAdEndedReason.COMPLETED) {
                            lastReportedAdPlayhead = lastReportedAdDuration;
                            endedAdHandler();
                        } else if (((AdEvent.AdEndedEvent) event).adEndedReason == PKAdEndedReason.SKIPPED) {
                            skipAdHandler();
                        }
                        break;
                    case AD_BREAK_IGNORED:
                        endedAdHandler();
                        break;
                    case ADS_PLAYBACK_ENDED:
                        endedAdHandler();
                        break;
                    case ERROR:
                        AdEvent.Error errorEvent = (AdEvent.Error) event;
                        handleAdError(errorEvent.error);
                        break;
                    case AD_CLICKED:
                        log.d("learn more clicked");
                        //We are not sending this event to youbora,
                        //so prevent it from dispatching through YouboraEvent.YouboraReport.
                        return;
                    case AD_POSITION_UPDATED:
                        if (event instanceof AdEvent.AdProgressUpdateEvent) {
                            if (((AdEvent.AdProgressUpdateEvent) event).currentPosition < Consts.MILLISECONDS_MULTIPLIER) {
                                lastReportedAdPlayhead = ((AdEvent.AdProgressUpdateEvent) event).currentPosition * 1.0;
                            } else {
                                lastReportedAdPlayhead = Long.valueOf(((AdEvent.AdProgressUpdateEvent) event).currentPosition / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                            }
                            currentReportedAdPlayhead = lastReportedAdPlayhead;
                        }

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
        return lastReportedAdDuration != null ? (lastReportedAdDuration) : 0.0D;
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

        if (adPositionType == UNKNOWN) {
            return adPosition;
        }

        switch (adPositionType) {

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



    void resetAdValues() {
        isFirstPlay = true;
        lastReportedAdDuration = super.getAdDuration();
        lastReportedAdTitle = super.getAdTitle();
        lastReportedAdPlayhead = super.getAdPlayhead();
    }

    private void populateAdValues() {
        lastReportedAdDuration = currentReportedAdDuration;
        lastReportedAdTitle = currentReportedAdTitle;
        lastReportedAdPlayhead = currentReportedAdPlayhead;
        log.d("lastReportedAdDuration: " + lastReportedAdDuration);
        log.d("lastReportedAdTitle: " + lastReportedAdTitle);
        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
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
