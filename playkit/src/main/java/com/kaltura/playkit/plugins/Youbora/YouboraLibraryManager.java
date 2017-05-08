package com.kaltura.playkit.plugins.Youbora;

import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlaybackParamsInfo;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.npaw.youbora.plugins.PluginGeneric;
import com.npaw.youbora.youboralib.BuildConfig;
import com.npaw.youbora.youboralib.utils.Utils;

import org.json.JSONException;

import java.util.Map;

import static com.kaltura.playkit.PlayerEvent.Type.STATE_CHANGED;

/**
 * @hide
 */

public class YouboraLibraryManager extends PluginGeneric {

    private static final PKLog log = PKLog.get("YouboraLibraryManager");

    //private static final long MONITORING_INTERVAL = 200L;

    private Player player;
    private MessageBus messageBus;
    private PKMediaConfig mediaConfig;

    private boolean isFirstPlay = true;
    private boolean isBuffering = false;
    private boolean allowSendingYouboraBufferEvents = false; //When false will prevent from sending bufferUnderrun event.

    private String lastReportedResource = "unknown";
    private Double lastReportedBitrate = -1.0;
    private Double lastReportedThroughput;
    private String lastReportedRendition;
    private AdCuePoints adCuePoints;

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
        this.pluginName = YouboraPlugin.factory.getName();
        this.pluginVersion = BuildConfig.VERSION_NAME + "-"+ PlayKitManager.CLIENT_TAG;
        //ViewManager.setMonitoringInterval(MONITORING_INTERVAL); // needed only if  bufferedHandler() is not used
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
                lastReportedBitrate    = Long.valueOf(currentPlaybackParams.getVideoBitrate()).doubleValue();
                lastReportedThroughput = Long.valueOf(currentPlaybackParams.getVideoThroughput()).doubleValue();
                lastReportedResource  = currentPlaybackParams.getMediaUrl();
                lastReportedRendition = generateRendition(lastReportedBitrate, (int)currentPlaybackParams.getVideoWidth(), (int)currentPlaybackParams.getVideoHeight());
                return;
            }

            if (event instanceof PlayerEvent && viewManager != null) {
                log.d("PlayerEvent: " + ((PlayerEvent) event).type.toString());
                switch (((PlayerEvent) event).type) {
                    case DURATION_CHANGE:
                        log.d("new duration = " + ((PlayerEvent.DurationChanged) event).duration);
                        break;
                    case STATE_CHANGED:
                        YouboraLibraryManager.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    case ENDED:
                        if (!isFirstPlay && ((adCuePoints == null) || (adCuePoints != null && !adCuePoints.hasPostRoll()))) {
                            endedHandler();
                            adCuePoints = null;
                        }
                        break;
                    case ERROR:
                        if (!isFirstPlay) {
                            errorHandler(event.eventType().toString());
                        }
                        adCuePoints = null;
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
        log.d("Ad Event: " + ((AdEvent) event).type.toString());

        switch (event.type) {
            case STARTED:
                ignoringAdHandler();
                allowSendingYouboraBufferEvents = false;
                break;
            case CONTENT_RESUME_REQUESTED:
                ignoredAdHandler();
                break;
            case CUEPOINTS_CHANGED:
                AdEvent.AdCuePointsUpdateEvent cuePointsList = (AdEvent.AdCuePointsUpdateEvent) event;
                if (cuePointsList != null) {
                    adCuePoints = cuePointsList.cuePoints;
                }
                break;
            case ALL_ADS_COMPLETED:
                if (adCuePoints != null && adCuePoints.hasPostRoll()) {
                    endedHandler();
                    adCuePoints = null;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void pauseMonitoring() {
        super.pauseMonitoring();
        allowSendingYouboraBufferEvents = false;
    }

    public void startMonitoring(Object player) {
        log.d("startMonitoring");
        super.startMonitoring(player);
        isFirstPlay = true;
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
        log.d("getThroughput = " + lastReportedThroughput);
        return this.lastReportedThroughput;
    }

    public String getRendition() {
        log.d("getRendition = " + lastReportedRendition);
        return lastReportedRendition;
    }

    public String getPlayerVersion() {
        return PlayKitManager.CLIENT_TAG;
    }

    public Double getPlayhead() {
        double currPos = Long.valueOf(player.getCurrentPosition()).doubleValue();
        log.d("getPlayhead currPos = " + currPos);
        return currPos;
    }

//    public String getResource() {
//        return this.mediaUrl;
//    }

//    public Double getMediaDuration() {
//        double lastReportedMediaDuration  =  (mediaConfig == null) ? 0 : Long.valueOf(mediaConfig.getMediaEntry().getDuration()).doubleValue();
//        log.d("lastReportedMediaDuration = " + lastReportedMediaDuration);
//        return lastReportedMediaDuration;
//    }

//    public Boolean getIsLive() {
//        return mediaConfig != null && (mediaConfig.getMediaEntry().getMediaType() == PKMediaEntry.MediaEntryType.Live);
//    }

    private void sendReportEvent(PKEvent event) {
        String reportedEventName = event.eventType().name();
        messageBus.post(new YouboraEvent.YouboraReport(reportedEventName));
    }

    public String generateRendition(double bitrate,  int width, int height) {

        if ((width <= 0 || height <= 0) && bitrate <= 0) {
            return super.getRendition();
        } else {
            return Utils.buildRenditionString(width, height, bitrate);
        }
    }

    public void resetValues() {
        lastReportedResource = "unknown";
        lastReportedBitrate = super.getBitrate();
        lastReportedRendition = super.getRendition();
        lastReportedThroughput = super.getThroughput();
        isFirstPlay = false;
    }

}
