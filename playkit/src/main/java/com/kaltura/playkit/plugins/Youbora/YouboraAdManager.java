package com.kaltura.playkit.plugins.Youbora;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdError;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.utils.Consts;
import com.npaw.youbora.adnalyzers.AdnalyzerGeneric;
import com.npaw.youbora.plugins.PluginGeneric;
import com.npaw.youbora.youboralib.utils.YBLog;

import static com.kaltura.playkit.PlayerEvent.Type.STATE_CHANGED;

/**
 * @hide
 */

public class YouboraAdManager extends AdnalyzerGeneric {
    private static final PKLog log = PKLog.get("YouboraAdManager");

    private Player player;
    private boolean isBuffering = false;
    private MessageBus messageBus;
    private double adBitrate = -1;
    private AdInfo currentAdInfo;

    private String lastReportedAdId;
    private String lastReportedAdTitle;
    private Double lastReportedAdPlayhead;
    private Double lastReportedAdDuration;

//
//    ads: {
//                title: "adTitleTest",
//                duration: 10,
//                resource: "http://yourhost.com/youradmedia.m3u8",
//                position: 2,
//                campaign: "Christmas"
//    },

    public YouboraAdManager(PluginGeneric plugin, JsonObject pluginConfig, MessageBus messageBus, Player player) {
        super(plugin);
        this.messageBus = messageBus;
        this.player = player;
        this.messageBus.listen(mEventListener, STATE_CHANGED);
        this.messageBus.listen(mEventListener, (Enum[]) AdEvent.Type.values());
        this.messageBus.listen(mEventListener, (Enum[]) AdError.Type.values());

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
        sendReportEvent(event);
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof AdEvent) {
                log.d("AdManager: " + ((AdEvent) event).type.toString());
                switch (((AdEvent) event).type) {
                    case LOADED:
                        playAdHandler();
                        break;
                    case STARTED:
                        joinAdHandler();
                        currentAdInfo = ((AdEvent.AdStartedEvent) event).adInfo;
                        lastReportedAdDuration = currentAdInfo.getAdDuration() / 1000D;
                        lastReportedAdId = currentAdInfo.getAdId();
                        lastReportedAdTitle = currentAdInfo.getAdTitle();

                        break;
                    case PAUSED:
                        pauseAdHandler();
                        break;
                    case RESUMED:
                        resumeAdHandler();
                        break;
                    case COMPLETED:
                        endedAdHandler();
                        break;
                    case SKIPPED:
                        skipAdHandler();
                        break;
                    case FIRST_QUARTILE:
                    case MIDPOINT:
                    case THIRD_QUARTILE:
                    case CLICKED:
                    case TAPPED:
                    case ICON_TAPPED:
                    case AD_BREAK_READY:
                    case AD_PROGRESS:
                        break;
                    case AD_BREAK_STARTED:
                        break;
                    case AD_BREAK_ENDED:
                        break;
                    case CUEPOINTS_CHANGED:
                        break;
                    case CONTENT_PAUSE_REQUESTED:
                        break;
                    case CONTENT_RESUME_REQUESTED:
                        break;
                    case ALL_ADS_COMPLETED:
                        //stopMonitoring();
                        break;
                }
                sendReportEvent(event);
            } else if (event instanceof AdError) {
                AdError.Type adError =  (((AdError) event).errorType);
                YBLog.debug("onAdError");

            } else if (event instanceof PlayerEvent) {
                switch (((PlayerEvent) event).type) {
                    case STATE_CHANGED:
                        YouboraAdManager.this.onEvent((PlayerEvent.StateChanged) event);
                }
            }
        }
    };

    public void startMonitoring(Object player) {
        log.d("startMonitoring");
        super.startMonitoring(player);
    }

    public void stopMonitoring() {
        log.d("stopMonitoring");
        super.stopMonitoring();
        this.messageBus.remove(mEventListener, STATE_CHANGED);
        messageBus.remove(mEventListener,(Enum[]) AdEvent.Type.values());
        messageBus.remove(mEventListener,(Enum[]) AdError.Type.values());

    }

    public void setAdBitrate(Double bitrate) {
        this.adBitrate = bitrate;
    }

    public Double getAdBitrate() {
        return this.adBitrate;
    }

    public Double getMediaPlayhead() {
        return this.plugin.getPlayhead() / 1000;
    }

    public String getAdTitle() {
        return currentAdInfo != null? currentAdInfo.getAdTitle() : "";
    }

    public Double getAdDuration() {
        return  currentAdInfo != null ? (currentAdInfo.getAdDuration() / 1000D) : 0.0D;
    }

    public String getAdPlayerVersion() {
        return PlayKitManager.CLIENT_TAG;
    }

    private void sendReportEvent(PKEvent event) {
        String reportedEventName = event.eventType().name();
        log.d(reportedEventName);
        messageBus.post(new YouboraEvent.YouboraReport(reportedEventName));
    }

    public Double getAdPlayhead() {
        if (player != null && player.getAdController() != null) {
            return Long.valueOf(player.getAdController().getAdCurrentPosition()).doubleValue() / 1000;
        }
        return Consts.POSITION_UNSET * 1D;
    }

    public String getAdPosition() {
            return "pre";
    }

    public String getAdResource() {
        return lastReportedAdId;
    }
}
