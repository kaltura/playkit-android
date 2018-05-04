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
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlaybackInfo;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.utils.Consts;
import com.npaw.youbora.plugins.PluginGeneric;
import com.npaw.youbora.youboralib.BuildConfig;
import com.npaw.youbora.youboralib.utils.Utils;

import org.json.JSONException;

import java.util.LinkedHashSet;
import java.util.Map;

import static com.kaltura.playkit.PlayerEvent.Type.PLAYHEAD_UPDATED;
import static com.kaltura.playkit.PlayerEvent.Type.STATE_CHANGED;

/**
 * @hide
 */

class YouboraLibraryManager extends PluginGeneric {

    private static final PKLog log = PKLog.get("YouboraLibraryManager");
    private static final String KALTURA_ANDROID = "Kaltura-Android";
    private static final String PLAYER_ERROR_STR = "Player error occurred";


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

    YouboraLibraryManager(String options) throws JSONException {
        super(options);
    }

    YouboraLibraryManager(Map<String, Object> options, MessageBus messageBus, PKMediaConfig mediaConfig, Player player) {
        super(options);
        this.player = player;
        this.messageBus = messageBus;
        this.mediaConfig = mediaConfig;

        messageBus.listen(mEventListener, (Enum[]) PlayerEvent.Type.values());
        messageBus.listen(mEventListener, (Enum[]) AdEvent.Type.values());
    }

    protected void init() {
        super.init();
        this.pluginName = KALTURA_ANDROID;
        this.pluginVersion = BuildConfig.VERSION_NAME + "-" + getPlayerVersion();
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

            if (event.eventType() == PlayerEvent.Type.PLAYBACK_INFO_UPDATED) {
                PlaybackInfo currentPlaybackInfo = ((PlayerEvent.PlaybackInfoUpdated) event).playbackInfo;
                lastReportedBitrate = Long.valueOf(currentPlaybackInfo.getVideoBitrate()).doubleValue();
                lastReportedThroughput = Long.valueOf(currentPlaybackInfo.getVideoThroughput()).doubleValue();
                lastReportedRendition = generateRendition(lastReportedBitrate, (int) currentPlaybackInfo.getVideoWidth(), (int) currentPlaybackInfo.getVideoHeight());
                return;
            }

            if (event instanceof PlayerEvent && viewManager != null) {
                if (event.eventType() != PLAYHEAD_UPDATED) {
                    log.d("New PKEvent = " + event.eventType().name());
                }
                switch (((PlayerEvent) event).type) {
                    case DURATION_CHANGE:
                        log.d("new duration = " + ((PlayerEvent.DurationChanged) event).duration);
                        break;
                    case STATE_CHANGED:
                        YouboraLibraryManager.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    case ENDED:
                        if (!isFirstPlay && ((adCuePoints == null) || !adCuePoints.hasPostRoll())) {
                            endedHandler();
                            isFirstPlay = true;
                            adCuePoints = null;
                        }
                        break;
                    case ERROR:
                        sendErrorHandler(event);
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
                    case SOURCE_SELECTED:
                        PlayerEvent.SourceSelected sourceSelected = ((PlayerEvent.SourceSelected) event);
                        lastReportedResource = sourceSelected.source.getUrl();
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

    private void sendErrorHandler(PKEvent event) {

        PlayerEvent.Error errorEvent = (PlayerEvent.Error) event;
        String errorMetadata = (errorEvent != null && errorEvent.error != null) ? errorEvent.error.message : PLAYER_ERROR_STR;
        PKError error = errorEvent.error;
        if (error.exception == null) {
            errorHandler(errorMetadata, event.eventType().name());
            return;
        }

        Exception playerErrorException = (Exception) error.exception;
        String exceptionClass = "";
        String exceptionCause = "";

        if (playerErrorException.getCause() != null && playerErrorException.getCause().getClass() != null) {
            exceptionClass = playerErrorException.getCause().getClass().getName();
            errorMetadata = (playerErrorException.getCause().toString() != null) ? playerErrorException.getCause().toString() : errorMetadata;
        } else {
            if (error.exception.getClass() != null) {
                exceptionClass = error.exception.getClass().getName();
            }
        }

        LinkedHashSet<String> causeMessages = getExceptionMessageChain(playerErrorException);
        if (causeMessages.isEmpty()) {
            exceptionCause = playerErrorException.toString();
        } else {
            for (String cause : causeMessages)
                exceptionCause += cause + "\n";
        }

        String errorCode = (errorEvent != null && errorEvent.error != null && errorEvent.error.errorType != null) ?  errorEvent.error.errorType + " - " : "";
        errorHandler(exceptionCause, errorCode + exceptionClass, errorMetadata);
    }

    public static LinkedHashSet<String> getExceptionMessageChain(Throwable throwable) {
        LinkedHashSet<String> result = new LinkedHashSet();
        while (throwable != null) {
            if (throwable.getMessage() != null){
                result.add(throwable.getMessage());
            }
            throwable = throwable.getCause();
        }
        return result;
    }

    private void onAdEvent(AdEvent event) {
        if (event.type != AdEvent.Type.PLAY_HEAD_CHANGED) {
            log.d("Ad Event: " + event.type.name());
        }

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
                adCuePoints = cuePointsList.cuePoints;
                break;
            case ALL_ADS_COMPLETED:
                if (adCuePoints != null && adCuePoints.hasPostRoll()) {
                    endedHandler();
                    isFirstPlay = true;
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
        return this.lastReportedThroughput;
    }

    public String getRendition() {
        return lastReportedRendition;
    }

    public String getPlayerVersion() {
        return Consts.KALTURA + "-" + PlayKitManager.CLIENT_TAG;
    }

    public Double getPlayhead() {
        double currPos = Long.valueOf(player.getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
        return (currPos >= 0) ? currPos : 0;
    }

    public String getResource() {
        return lastReportedResource;
    }

    public Double getMediaDuration() {
        double lastReportedMediaDuration = (mediaConfig == null) ? 0 : Long.valueOf(mediaConfig.getMediaEntry().getDuration() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
        log.d("lastReportedMediaDuration = " + lastReportedMediaDuration);
        return lastReportedMediaDuration;
    }

    public String getTitle() {
        if (mediaConfig == null || mediaConfig.getMediaEntry() == null) {
            return "unknown";
        } else {
            return mediaConfig.getMediaEntry().getId();
        }
    }

    public Boolean getIsLive() {
        return mediaConfig != null && (mediaConfig.getMediaEntry().getMediaType() == PKMediaEntry.MediaEntryType.Live);
    }

    private void sendReportEvent(PKEvent event) {
        if (event.eventType() != PLAYHEAD_UPDATED) {
            String reportedEventName = event.eventType().name();
            messageBus.post(new YouboraEvent.YouboraReport(reportedEventName));
        }
    }

    public String generateRendition(double bitrate, int width, int height) {

        if ((width <= 0 || height <= 0) && bitrate <= 0) {
            return super.getRendition();
        } else {
            return Utils.buildRenditionString(width, height, bitrate);
        }
    }

    public void resetValues() {
        lastReportedBitrate = super.getBitrate();
        lastReportedRendition = super.getRendition();
        lastReportedThroughput = super.getThroughput();
        isFirstPlay = true;
    }

    public void onUpdateConfig() {
        resetValues();
        adCuePoints = null;
        lastReportedResource = "unknown";
    }
}
