package com.kaltura.playkit.plugins.Youbora;

import com.kaltura.playkit.LogEvent;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.AnalyticsEvent;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.npaw.youbora.adnalyzers.AdnalyzerGeneric;
import com.npaw.youbora.plugins.PluginGeneric;

/**
 * @hide
 */

public class YouboraAdManager extends AdnalyzerGeneric {
    private static final PKLog log = PKLog.get("YouboraAdManager");
    private static final String TAG = "YouboraAdManager";

    private boolean isBuffering = false;
    private MessageBus messageBus;
    private double adBitrate = -1;
    private AdInfo currentAdInfo;

    public YouboraAdManager(PluginGeneric plugin, MessageBus messageBus) {
        super(plugin);
        this.messageBus = messageBus;
        this.messageBus.listen(mEventListener, PlayerEvent.Type.STATE_CHANGED);
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
        log.d(event.newState.toString());
        messageBus.post(new LogEvent(TAG + " " + event.newState.toString()));
        messageBus.post(new AnalyticsEvent.BaseAnalyticsReportEvent(AnalyticsEvent.Type.YOUBORA_REPORT, event.newState.toString()));

    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof AdEvent) {
                log.d(((AdEvent) event).type.toString());
                switch (((AdEvent) event).type) {
                    case STARTED:
                        currentAdInfo = ((AdEvent.AdStartedEvent) event).adInfo;
                        joinAdHandler();
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
                    case FIRST_QUARTILE:

                        break;
                    case MIDPOINT:

                        break;
                    case THIRD_QUARTILE:
                        break;
                    case SKIPPED:
                        skipAdHandler();
                        break;
                    case CLICKED:
                        break;
                    case TAPPED:
                        break;
                    case ICON_TAPPED:
                        break;
                    case AD_BREAK_READY:
                        break;
                    case AD_PROGRESS:
                        break;
                    case AD_BREAK_STARTED:
                        break;
                    case AD_BREAK_ENDED:
                        break;
                    case CUEPOINTS_CHANGED:
                        break;
                    case LOADED:
                        playAdHandler();
                        break;
                    case CONTENT_PAUSE_REQUESTED:
                        break;
                    case CONTENT_RESUME_REQUESTED:
                        break;
                    case ALL_ADS_COMPLETED:
                        break;
                }
                log.d(event.eventType().name());
                messageBus.post(new LogEvent(TAG + " " + ((AdEvent) event).type.toString()));
                messageBus.post(new AnalyticsEvent.BaseAnalyticsReportEvent(AnalyticsEvent.Type.YOUBORA_REPORT, event.eventType().toString()));
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

    public String getAdPosition() {
        return "unknown";
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
}
