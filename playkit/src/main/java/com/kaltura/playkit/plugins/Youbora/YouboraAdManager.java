package com.kaltura.playkit.plugins.Youbora;

import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdError;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.utils.Consts;
import com.npaw.youbora.adnalyzers.AdnalyzerGeneric;
import com.npaw.youbora.plugins.PluginGeneric;
import com.npaw.youbora.youboralib.BuildConfig;
import com.npaw.youbora.youboralib.utils.YBLog;

import static com.kaltura.playkit.PlayerEvent.Type.STATE_CHANGED;

/**
 * @hide
 */

public class YouboraAdManager extends AdnalyzerGeneric {
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

    public YouboraAdManager(PluginGeneric plugin, MessageBus messageBus) {
        super(plugin);
        this.adnalyzerVersion = BuildConfig.VERSION_NAME + "-" + getAdPlayerVersion();
        this.messageBus = messageBus;

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
            if (event instanceof PlayerEvent) {
                return;
            }
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

                        lastReportedAdPlayhead =  Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
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
                        lastReportedAdPlayhead =  Long.valueOf(currentAdInfo.getAdPlayHead() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                        log.d("lastReportedAdPlayhead: " + lastReportedAdPlayhead);
                        skipAdHandler();
                        break;
                    case PLAY_HEAD_CHANGED:
                        double adPos = Long.valueOf(((AdEvent.AdPlayHeadEvent) event).adPlayHead).doubleValue();
                        lastReportedAdPlayhead = adPos;
                        break;
                }
                sendReportEvent(event);
            } else if (event instanceof AdError) {
                AdError.Type adError =  (((AdError) event).errorType);
                log.d("AdManager Error: " + adError.name());
                switch (adError) {
                    case QUIET_LOG_ERROR:
                        log.d("QUIET_LOG_ERROR avoiding error");
                        break;
                    default:
                        YBLog.debug("onAdError " + adError.name());
                        endedAdHandler();
                }
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
    public String getAdTitle() {
        log.d("getAdTitle ");
        return lastReportedAdTitle != null ? lastReportedAdTitle : "No Info";
    }

    @Override
    public Double getAdDuration() {
        Double adDuration = currentAdInfo != null ? (Long.valueOf(currentAdInfo.getAdDuration() / Consts.MILLISECONDS_MULTIPLIER).doubleValue()) : 0.0D;        
        return adDuration;
    }

    @Override
    public String getAdPlayerVersion() {
        log.d("getAdPlayerVersion " + PlayKitManager.CLIENT_TAG);

        return Consts.KALTURA + "-" + PlayKitManager.CLIENT_TAG;
    }

    private void sendReportEvent(PKEvent event) {
        if (event instanceof AdEvent && (event.eventType() == AdEvent.Type.PLAY_HEAD_CHANGED || event.eventType() == AdEvent.Type.AD_PROGRESS)) {
            return;
        }
        String reportedEventName = event.eventType().name();
        if (event instanceof AdError || event instanceof AdEvent) {
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
            default:
                adPosition = "unknown";
        }
        log.d("adPosition = " + adPosition);
        return  adPosition;
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

    public void resetAdValues() {
        isFirstPlay = true;
        currentAdInfo = null;
        lastReportedAdDuration = super.getAdDuration();;
        lastReportedAdTitle = super.getAdTitle();
        lastReportedAdPlayhead = super.getAdPlayhead();
    }

    public void onUpdateConfig() {
        resetAdValues();
        adBitrate = -1;
        lastReportedAdResource = super.getAdResource();
    }
}
