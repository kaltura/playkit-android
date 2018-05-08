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

import com.kaltura.playkit.BuildConfig;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.PKAdErrorType;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.utils.Consts;
import com.npaw.youbora.lib6.adapter.PlayerAdapter;

import java.util.HashMap;

import static com.kaltura.playkit.PlayerEvent.Type.PLAYHEAD_UPDATED;

/**
 * @hide
 */

class YouboraAdManager extends PlayerAdapter<Player> {
    private static final PKLog log = PKLog.get("YouboraAdManager");

    private boolean isFirstPlay = true;
    private boolean isBuffering = false;
    private MessageBus messageBus;
    private Long adBitrate = -1L;

    private AdInfo currentAdInfo;
    private String lastReportedAdResource;
    private String lastReportedAdTitle;
    private Double lastReportedAdPlayhead;
    private Double lastReportedAdDuration;

    YouboraAdManager(Player player, MessageBus messageBus) {
        super(player);
        this.messageBus = messageBus;
        registerListeners();
    }

    private void onEvent(PlayerEvent.StateChanged event) {
        log.d(event.newState.toString());
        switch (event.newState) {
            case READY:
                if (isBuffering) {
                    isBuffering = false;
                    fireBufferEnd();
                }
                break;
            case BUFFERING:
                isBuffering = true;
                fireBufferBegin();
                break;
            default:
                break;
        }
        messageBus.post(new YouboraEvent.YouboraReport(event.eventType().name()));
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {

            if (event.eventType() != AdEvent.Type.PLAY_HEAD_CHANGED && event.eventType() != PLAYHEAD_UPDATED) {
                log.d("YouboraAdManager on event " + event.eventType());
            }

            if (event instanceof AdEvent) {
                if (getPlugin() == null || getPlugin().getAdapter() == null) {
                    log.e("Player Adapter is null return");
                    return;
                }

                switch (((AdEvent) event).type) {
                    case AD_REQUESTED:
                        lastReportedAdResource = ((AdEvent.AdRequestedEvent) event).adTagUrl;
                        log.d("lastReportedAdResource: " + lastReportedAdResource);
                        break;
                    case LOADED:
                        if (isFirstPlay) {
                            isFirstPlay = false;
                            getPlugin().getAdapter().fireStart();
                        }
                        currentAdInfo = ((AdEvent.AdLoadedEvent) event).adInfo;
                        populateAdValues();
                        fireStart();
                        break;
                    case STARTED:
                        currentAdInfo = ((AdEvent.AdStartedEvent) event).adInfo;
                        lastReportedAdPlayhead = Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
                        fireJoin();
                        break;
                    case PAUSED:
                        currentAdInfo = ((AdEvent.AdPausedEvent) event).adInfo;
                        lastReportedAdPlayhead = Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
                        firePause();
                        break;
                    case RESUMED:
                        currentAdInfo = ((AdEvent.AdResumedEvent) event).adInfo;
                        if (isFirstPlay) {
                            isFirstPlay = false;
                            fireStart();
                            fireJoin();
                            populateAdValues();
                        }

                        lastReportedAdPlayhead = Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
                        fireResume();
                        break;
                    case COMPLETED:
                        lastReportedAdPlayhead = lastReportedAdDuration;
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
                        fireStop();
                        break;
                    case AD_BREAK_IGNORED:
                        fireStop();
                        break;
                    case CONTENT_RESUME_REQUESTED:
                        fireStop();
                        break;
                    case SKIPPED:
                        currentAdInfo = ((AdEvent.AdSkippedEvent) event).adInfo;
                        lastReportedAdPlayhead = Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
                        fireStop(new HashMap<String, String>(){{put("skipped","true");}});
                        break;
                    case ERROR:
                        AdEvent.Error errorEvent = (AdEvent.Error) event;
                        log.e("ERROR " + errorEvent.error.errorType);
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

    @Override
    public void registerListeners() {
        super.registerListeners();
        messageBus.listen(mEventListener, (PlayerEvent.Type.ERROR));
        messageBus.listen(mEventListener, (Enum[]) AdEvent.Type.values());
    }

    @Override
    public void unregisterListeners() {
        messageBus.remove(mEventListener, (Enum[]) AdEvent.Type.values());
        messageBus.remove(mEventListener, (PlayerEvent.Type.ERROR));
        super.unregisterListeners();
    }

    public void setAdBitrate(Long bitrate) {
        this.adBitrate = bitrate;
    }

    @Override
    public Long getBitrate() {
        return this.adBitrate;
    }

    @Override
    public String getTitle() {
        log.d("getAdTitle ");
        return lastReportedAdTitle != null ? lastReportedAdTitle : "No Info";
    }

    @Override
    public Double getDuration() {
        return currentAdInfo != null ? (Long.valueOf(currentAdInfo.getAdDuration() / Consts.MILLISECONDS_MULTIPLIER).doubleValue()) : 0D;
    }

    @Override
    public String getPlayerVersion() {
        log.d("getAdPlayerVersion " + PlayKitManager.CLIENT_TAG);

        return Consts.KALTURA + "-" + PlayKitManager.CLIENT_TAG;
    }

    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME + "-" + getPlayerVersion();
    }

    @Override
    public Double getPlayhead() {
        log.d("getAdPlayhead = " + lastReportedAdPlayhead);
        return lastReportedAdPlayhead;
    }

    @Override
    public AdPosition getPosition() {
        AdPosition adPosition = AdPosition.UNKNOWN;

        if (currentAdInfo == null) {
            return adPosition;
        }

        switch (currentAdInfo.getAdPositionType()) {
            case PRE_ROLL:
                adPosition = AdPosition.PRE;
                break;
            case MID_ROLL:
                adPosition = AdPosition.MID;
                break;
            case POST_ROLL:
                adPosition = AdPosition.POST;
                break;
            default:
                break;
        }
        log.d("adPosition = " + adPosition);
        return adPosition;
    }

    @Override
    public String getResource() {
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
        lastReportedAdDuration = super.getDuration();
        lastReportedAdTitle = super.getTitle();
        lastReportedAdPlayhead = super.getPlayhead();
    }

    public void onUpdateConfig() {
        resetAdValues();
        adBitrate = -1L;
        lastReportedAdResource = super.getResource();
    }

    private void handleAdError(PKError error) {

        PKAdErrorType adErrorType = (PKAdErrorType) error.errorType;

        switch (adErrorType) {
            case QUIET_LOG_ERROR:
                log.d("QUIET_LOG_ERROR. Avoid sending to Youbora.");
                fireError(error.message, PKAdErrorType.QUIET_LOG_ERROR.name(), null, null);
                return;
            default:
                log.e("onAdError " + adErrorType.name());
                Exception adException = null;
                if (error.exception instanceof  Exception) {
                    adException = (Exception) error.exception;
                }
                fireFatalError(error.message, adErrorType.name(), null, adException);
        }

        messageBus.post(new YouboraEvent.YouboraReport(adErrorType.name()));
    }
}
