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
                        currentAdInfo = ((AdEvent.AdLoadedEvent) event).adInfo;
                        lastReportedAdDuration = currentAdInfo.getAdDuration() / 1000D;
                        lastReportedAdId = currentAdInfo.getAdId();
                        lastReportedAdTitle = currentAdInfo.getAdTitle();
                        lastReportedAdPlayhead = currentAdInfo.getAdPlayHead() / 1000D;
                        log.d("lastReportedAdDuration: " + lastReportedAdDuration);
                        log.d("lastReportedAdId: " + lastReportedAdId);
                        log.d("lastReportedAdTitle: " + lastReportedAdTitle);
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);

                        playAdHandler();
                        break;
                    case STARTED:
                        currentAdInfo = ((AdEvent.AdStartedEvent) event).adInfo;
                        lastReportedAdPlayhead = currentAdInfo.getAdPlayHead() / 1000D;
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);

                        joinAdHandler();
                        break;
                    case PAUSED:
                        currentAdInfo = ((AdEvent.AdPausedEvent) event).adInfo;
                        lastReportedAdPlayhead = currentAdInfo.getAdPlayHead() / 1000D;
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);

                        pauseAdHandler();
                        break;
                    case RESUMED:
                        currentAdInfo = ((AdEvent.AdResumedEvent) event).adInfo;
                        lastReportedAdPlayhead = currentAdInfo.getAdPlayHead() / 1000D;
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
                    case SKIPPED:
                        currentAdInfo = ((AdEvent.AdSkippedEvent) event).adInfo;
                        lastReportedAdPlayhead = currentAdInfo.getAdPlayHead() / 1000D;
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
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

    @Override
    public Double getAdBitrate() {
        return this.adBitrate;
    }

    @Override
    public Double getMediaPlayhead() {
        return this.plugin.getPlayhead() / 1000;
    }

    @Override
    public String getAdTitle() {
        log.d("getAdTitle ");

        return currentAdInfo != null ? currentAdInfo.getAdTitle() : "No Info";
    }

    @Override
    public Double getAdDuration() {
        Double adDuration = currentAdInfo != null ? (currentAdInfo.getAdDuration() / 1000D) : 0.0D;
        log.d("getAdDuration getAdDuration " + adDuration);
        return adDuration;
    }

    @Override
    public String getAdPlayerVersion() {
        log.d("getAdPlayerVersion " + PlayKitManager.CLIENT_TAG);

        return PlayKitManager.CLIENT_TAG;
    }

    private void sendReportEvent(PKEvent event) {
        String reportedEventName = event.eventType().name();
        if (event instanceof AdError ||event instanceof AdEvent) {
            messageBus.post(new YouboraEvent.YouboraReport(reportedEventName));
        }
    }

    @Override
    public Double getAdPlayhead() {
        log.d("getAdPlayhead = " + lastReportedAdPlayhead);
        return lastReportedAdPlayhead;
    }

    @Override
    public String getAdPosition() {
        String adPosition =  "unknown";

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
        }
        log.d("adPosition = " + adPosition);
        return  adPosition;
    }

    @Override
    public String getAdResource() {
        log.d("getAdResource = " + lastReportedAdId);

        return lastReportedAdId;
    }
}
