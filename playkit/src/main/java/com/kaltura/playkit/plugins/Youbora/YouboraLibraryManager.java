package com.kaltura.playkit.plugins.Youbora;

import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlaybackParamsInfo;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.npaw.youbora.plugins.PluginGeneric;
import com.npaw.youbora.youboralib.managers.ViewManager;

import org.json.JSONException;

import java.util.Map;

import static com.kaltura.playkit.PlayerEvent.Type.STATE_CHANGED;

/**
 * @hide
 */

public class YouboraLibraryManager extends PluginGeneric {

    private static final PKLog log = PKLog.get("YouboraLibraryManager");

    private static final long MONITORING_INTERVAL = 200L;

    private Player player;
    private MessageBus messageBus;
    private PKMediaConfig mediaConfig;

    private boolean isFirstPlay = true;
    private boolean isBuffering = false;
    private boolean allowSendingYouboraBufferEvents = false; //When false will prevent from sending bufferUnderrun event.

    private String mediaUrl = "unknown";
    private Double lastReportedBitrate = -1.0;
    private Double lastReportedthroughput = super.getThroughput();


    public YouboraLibraryManager(String options) throws JSONException {
        super(options);
    }

    public YouboraLibraryManager(Map<String, Object> options, MessageBus messageBus, PKMediaConfig mediaConfig, Player player) {
        super(options);
        this.player = player;
        this.messageBus = messageBus;
        this.mediaConfig = mediaConfig;

        messageBus.listen(mEventListener, (Enum[]) PlayerEvent.Type.values());
        messageBus.listen(mEventListener, (Enum[]) AdEvent.Type.values());
    }

    protected void init() {
        super.init();
        this.pluginName = PlayKitManager.CLIENT_TAG;
        this.pluginVersion = "5.3.0-" + PlayKitManager.CLIENT_TAG;
        ViewManager.setMonitoringInterval(MONITORING_INTERVAL);
    }

    private void onEvent(PlayerEvent.StateChanged event) {
        //If it is first play, do not continue with the flow.
        if (isFirstPlay) {
            return;
        }

        switch (event.newState) {
            case READY:
                if (isBuffering) {
                    isBuffering = false;
                    bufferedHandler();
                }
                break;
            case BUFFERING:
                if (allowSendingYouboraBufferEvents) {
                    isBuffering = true;
                    bufferingHandler();
                } else {
                    allowSendingYouboraBufferEvents = true;
                }
                break;
            default:
                break;
        }
        sendReportEvent(event);
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {

            if (event instanceof PlayerEvent.PlaybackParamsUpdated) {
                PlaybackParamsInfo currentPlaybackParams = ((PlayerEvent.PlaybackParamsUpdated) event).getPlaybackParamsInfo();
                lastReportedBitrate = Long.valueOf(currentPlaybackParams.getVideoBitrate()).doubleValue();
                mediaUrl = currentPlaybackParams.getMediaUrl();
                return;
            }

            if (event instanceof PlayerEvent && viewManager != null) {
                log.d(((PlayerEvent) event).type.toString());
                switch (((PlayerEvent) event).type) {
                    case DURATION_CHANGE:
                        log.d("new duration = " + ((PlayerEvent.DurationChanged) event).duration);
                        break;
                    case STATE_CHANGED:
                        YouboraLibraryManager.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    case ENDED:
                        if (!isFirstPlay) {
                            endedHandler();
                        }
                        break;
                    case ERROR:
                        if (!isFirstPlay) {
                            errorHandler(event.eventType().toString());
                        }
                        break;
                    case PAUSE:
                        pauseHandler();
                        break;
                    case PLAY:
                        if (!isFirstPlay) {
                            resumeHandler();
                        } else {
                            isFirstPlay = false;
                            playHandler();
                        }
                        break;
                    case PLAYING:
                        if (isFirstPlay) {
                            isFirstPlay = false;
                            playHandler();
                        }
                        playingHandler();
                        break;
                    case SEEKED:
                        seekedHandler();
                        break;
                    case SEEKING:
                        seekingHandler();
                        break;
                    case TRACKS_AVAILABLE:
                        log.d("onEvent: ");
                        break;
                    default:
                        break;
                }
                if (((PlayerEvent) event).type != STATE_CHANGED) {
                    sendReportEvent(event);
                }
            } else if (event instanceof AdEvent) {
                onAdEvent((AdEvent) event);
            }
        }
    };

    private void onAdEvent(AdEvent event) {
        switch (event.type) {
            case STARTED:
                ignoringAdHandler();
                allowSendingYouboraBufferEvents = false;
                break;
            case CONTENT_RESUME_REQUESTED:
                ignoredAdHandler();
                break;
            default:
                break;
        }
        sendReportEvent(event);
    }

    @Override
    public void pauseMonitoring() {
        super.pauseMonitoring();
        allowSendingYouboraBufferEvents = false;
    }

    public void startMonitoring(Object player) {
        log.d("startMonitoring");
        super.startMonitoring(player);
        allowSendingYouboraBufferEvents = false;
    }

    public void stopMonitoring() {
        log.d("stopMonitoring");
        super.stopMonitoring();
    }

    public Double getBitrate() {
        return this.lastReportedBitrate;
    }

    public Double getThroughput() {
        return this.lastReportedthroughput;
    }

    public Double getMediaDuration() {
        return mediaConfig == null ? 0 : Long.valueOf(mediaConfig.getMediaEntry().getDuration()).doubleValue();
    }

    public String getRendition() {
        return null;
    }

    public String getPlayerVersion() {
        return PlayKitManager.CLIENT_TAG;
    }

    public String getResource() {
        return this.mediaUrl;
    }

    public Double getPlayhead() {
        return Long.valueOf(player.getCurrentPosition()).doubleValue() / 1000;
    }

    public Boolean getIsLive() {
        return mediaConfig != null && (mediaConfig.getMediaEntry().getMediaType() == PKMediaEntry.MediaEntryType.Live);
    }

    private void sendReportEvent(PKEvent event) {
        String reportedEventName = event.eventType().name();
        log.d(reportedEventName);
        messageBus.post(new YouboraEvent.YouboraReport(reportedEventName));
    }

}
