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

package com.kaltura.playkit.plugins.ovp;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlaybackInfo;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlaykitUtils;
import com.kaltura.playkit.ads.PKAdErrorType;
import com.kaltura.playkit.api.ovp.services.KavaService;
import com.kaltura.playkit.player.PKPlayerErrorType;
import com.kaltura.playkit.pkutils.Consts;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by anton.afanasiev on 27/09/2017.
 */

public class KavaAnalyticsPlugin extends PKPlugin {

    private static final PKLog log = PKLog.get(KavaAnalyticsPlugin.class.getSimpleName());
    private static final long ONE_SECOND_IN_MS = 1000;
    private static final int TEN_SECONDS_IN_MS = 10000;
    private static final long DISTANCE_FROM_LIVE_THRESHOLD = 15000;

    private static final String DELIVERY_TYPE_HLS = "hls";
    private static final String DELIVERY_TYPE_DASH = "dash";
    private static final String DELIVERY_TYPE_OTHER = "url";

    private static final String DVR = "Dvr";

    private Player player;
    private Context context;
    private Timer viewEventTimer;
    private MessageBus messageBus;
    private PKMediaConfig mediaConfig;
    private RequestQueue requestExecutor;
    private KavaAnalyticsConfig pluginConfig;
    private PKEvent.Listener eventListener = initEventListener();

    private boolean playReached25;
    private boolean playReached50;
    private boolean playReached75;
    private boolean playReached100;

    private boolean isPaused = true;
    private boolean isFirstPlay = true;
    private boolean isImpressionSent;

    private int eventIndex;
    private int errorCode = -1;
    private int viewEventTimeCounter;

    private long actualBitrate;
    private long totalBufferTimePerEntry;
    private long lastKnownBufferingTimestamp;
    private long totalBufferTimePerViewEvent;
    private long targetSeekPositionInSeconds;

    private String referrer;
    private String deliveryType;
    private String sessionStartTime;
    private String currentCaptionLanguage;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KavaAnalytics";
        }

        @Override
        public PKPlugin newInstance() {
            return new KavaAnalyticsPlugin();
        }

        @Override
        public void warmUp(Context context) {

        }
    };

    private enum KavaEvents {
        IMPRESSION(1),
        PLAY_REQUEST(2),
        PLAY(3),
        RESUME(4),
        PLAY_REACHED_25_PERCENT(11),
        PLAY_REACHED_50_PERCENT(12),
        PLAY_REACHED_75_PERCENT(13),
        PLAY_REACHED_100_PERCENT(14),
        PAUSE(33),
        REPLAY(34),
        SEEK(35),
        CAPTIONS(38),
        SOURCE_SELECTED(39), // video track changed
        ERROR(98),
        VIEW(99);

        private final int value;

        KavaEvents(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    protected void onLoad(Player player, Object config, MessageBus messageBus, Context context) {
        this.player = player;
        this.context = context;
        this.messageBus = messageBus;
        this.requestExecutor = APIOkRequestsExecutor.getSingleton();
        this.messageBus.listen(eventListener, (Enum[]) PlayerEvent.Type.values());
        onUpdateConfig(config);
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;
        sessionStartTime = null;
        eventIndex = 1;
        resetFlags();
    }

    @Override
    protected void onUpdateConfig(Object config) {
        this.pluginConfig = parsePluginConfig(config);
        referrer = pluginConfig.getReferrerAsBase64();
        if (referrer == null) {
            referrer = buildDefaultReferrer();
        }
    }

    @Override
    protected void onApplicationPaused() {
        isPaused = true;
        stopAnalyticsTimer();
    }

    @Override
    protected void onApplicationResumed() {
        startAnalyticsTimer();
    }

    @Override
    protected void onDestroy() {
        stopAnalyticsTimer();
    }

    private PKEvent.Listener initEventListener() {
        return new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                if (event instanceof PlayerEvent) {
                    switch (((PlayerEvent) event).type) {
                        case STATE_CHANGED:
                            handleStateChanged((PlayerEvent.StateChanged) event);
                            break;
                        case LOADED_METADATA:
                            if (!isImpressionSent) {
                                isImpressionSent = true;
                                startAnalyticsTimer();
                                sendAnalyticsEvent(KavaEvents.IMPRESSION);
                            }
                            break;
                        case PLAY:
                            sendAnalyticsEvent(KavaEvents.PLAY_REQUEST);
                            break;
                        case PAUSE:
                            isPaused = true;
                            sendAnalyticsEvent(KavaEvents.PAUSE);
                            break;
                        case PLAYING:
                            if (isFirstPlay) {
                                isFirstPlay = false;
                                sendAnalyticsEvent(KavaEvents.PLAY);
                            } else {
                                if (isPaused) {
                                    sendAnalyticsEvent(KavaEvents.RESUME);
                                }
                            }
                            isPaused = false;
                            break;
                        case SEEKING:
                            PlayerEvent.Seeking seekingEvent = (PlayerEvent.Seeking) event;
                            targetSeekPositionInSeconds = seekingEvent.targetPosition / Consts.MILLISECONDS_MULTIPLIER;
                            sendAnalyticsEvent(KavaEvents.SEEK);
                            break;
                        case REPLAY:
                            sendAnalyticsEvent(KavaEvents.REPLAY);
                            break;
                        case SOURCE_SELECTED:
                            PKMediaSource selectedSource = ((PlayerEvent.SourceSelected) event).source;
                            updateDeliveryType(selectedSource.getMediaFormat());
                            break;
                        case ENDED:
                            maybeSentPlayerReachedEvent();
                            if (!playReached100) {
                                playReached100 = true;
                                sendAnalyticsEvent(KavaEvents.PLAY_REACHED_100_PERCENT);
                            }

                            resetFlags();
                            break;
                        case PLAYBACK_INFO_UPDATED:
                            PlaybackInfo playbackInfo = ((PlayerEvent.PlaybackInfoUpdated) event).playbackInfo;
                            actualBitrate = playbackInfo.getVideoBitrate();
                            break;
                        case VIDEO_TRACK_CHANGED:
                            sendAnalyticsEvent(KavaEvents.SOURCE_SELECTED);
                            break;
                        case TEXT_TRACK_CHANGED:
                            PlayerEvent.TextTrackChanged textTrackChanged = ((PlayerEvent.TextTrackChanged) event);
                            currentCaptionLanguage = textTrackChanged.newTrack.getLanguage();
                            sendAnalyticsEvent(KavaEvents.CAPTIONS);
                            break;
                        case ERROR:
                            PKError error = ((PlayerEvent.Error) event).pkError;
                            if (error.errorType instanceof PKPlayerErrorType) {
                                errorCode = ((PKPlayerErrorType) error.errorType).errorCode;
                            } else if (error.errorType instanceof PKAdErrorType) {
                                errorCode = ((PKAdErrorType) error.errorType).errorCode;
                            } else {
                                errorCode = -1;
                            }
                            sendAnalyticsEvent(KavaEvents.ERROR);
                            break;
                    }
                }
            }
        };
    }

    private void handleStateChanged(PlayerEvent.StateChanged event) {
        switch (event.newState) {
            case BUFFERING:
                if (isImpressionSent) {
                    lastKnownBufferingTimestamp = System.currentTimeMillis();
                }
                break;
            case READY:
                calculateTotalBufferTimePerViewEvent();
                break;
        }
    }

    private void sendAnalyticsEvent(final KavaEvents event) {
        if (!pluginConfig.isPartnerIdValid()) {
            log.w("Can not send analytics event. Mandatory field partnerId is missing");
            return;
        }

        Map<String, String> params = gatherParams(event);

        RequestBuilder requestBuilder = KavaService.sendAnalyticsEvent(pluginConfig.getBaseUrl(), params);
        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                log.d("onComplete: " + event.name());
                if (sessionStartTime == null && response.getResponse() != null) {
                    sessionStartTime = response.getResponse();
                }
                messageBus.post(new KavaAnalyticsEvent.KavaAnalyticsReport(event.name()));
            }
        });
        log.e("request sent " + requestBuilder.build().getUrl());
        requestExecutor.queue(requestBuilder.build());

        eventIndex++;
    }

    private Map<String, String> gatherParams(KavaEvents event) {
        if (pluginConfig == null) {
            log.w("Plugin config was not set! Use default one.");
            pluginConfig = new KavaAnalyticsConfig();
        }

        Map<String, String> params = new HashMap<>();

        String sessionId = player.getSessionId() != null ? player.getSessionId() : "";

        params.put("eventType", Integer.toString(event.getValue()));
        params.put("partnerId", Integer.toString(pluginConfig.getPartnerId()));
        params.put("entryId", mediaConfig.getMediaEntry().getId());
        params.put("sessionId", sessionId);
        params.put("eventIndex", Integer.toString(eventIndex));
        params.put("referrer", referrer);
        params.put("deliveryType", deliveryType);
        params.put("playbackType", getPlaybackType());
        params.put("clientVer", PlayKitManager.CLIENT_TAG);
        params.put("clientTag", PlayKitManager.CLIENT_TAG);
        params.put("position", Long.toString(player.getCurrentPosition()));

        if (sessionStartTime != null) params.put("sessionStartTime", sessionStartTime);

        switch (event) {
            case VIEW:
            case PLAY:
            case RESUME:
                float curBufferTimeInSeconds = totalBufferTimePerViewEvent == 0 ? 0 : totalBufferTimePerViewEvent / 1000.0f;
                float totalBufferTimeInSeconds = totalBufferTimePerEntry == 0 ? 0 : totalBufferTimePerEntry / 1000.0f;
                params.put("bufferTime", Float.toString(curBufferTimeInSeconds));
                params.put("bufferTimeSum", Float.toString(totalBufferTimeInSeconds));
                params.put("actualBitrate", Long.toString(actualBitrate));
                break;
            case SEEK:
                params.put("targetPosition", Long.toString(targetSeekPositionInSeconds));
                break;
            case CAPTIONS:
                params.put("caption", currentCaptionLanguage);
                break;
            case ERROR:
                if (errorCode != -1) {
                    params.put("errorCode", Integer.toString(errorCode));
                    errorCode = -1;
                }
                break;
        }

        addOptionalParams(params);
        return params;
    }

    private void addOptionalParams(Map<String, String> params) {

        if (pluginConfig.hasPlaybackContext()) {
            params.put("playbackContext", pluginConfig.getPlaybackContext());
        }

        if (pluginConfig.hasCustomVar1()) {
            params.put("customVar1", pluginConfig.getCustomVar1());
        }

        if (pluginConfig.hasCustomVar2()) {
            params.put("customVar2", pluginConfig.getCustomVar2());
        }

        if (pluginConfig.hasCustomVar3()) {
            params.put("customVar3", pluginConfig.getCustomVar3());
        }

        if (pluginConfig.hasKs()) {
            params.put("ks", pluginConfig.getKs());
        }

        if(pluginConfig.hasUiConfId()) {
            params.put("uiConfId", Integer.toString(pluginConfig.getUiConfId()));
        }
    }

    private void startAnalyticsTimer() {
        viewEventTimer = new Timer();
        viewEventTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isPaused) {
                    maybeSendViewEvent();
                    maybeSentPlayerReachedEvent();
                }
            }
        }, 0, ONE_SECOND_IN_MS);
    }

    private void stopAnalyticsTimer() {
        if (viewEventTimer == null) {
            return;
        }
        viewEventTimer.cancel();
        viewEventTimer = null;
    }

    private void maybeSendViewEvent() {
        viewEventTimeCounter += ONE_SECOND_IN_MS;
        if (viewEventTimeCounter >= TEN_SECONDS_IN_MS) {
            totalBufferTimePerEntry += totalBufferTimePerViewEvent;
            sendAnalyticsEvent(KavaEvents.VIEW);
            viewEventTimeCounter = 0;
            totalBufferTimePerViewEvent = 0;
        }
    }

    private void maybeSentPlayerReachedEvent() {
        float progress = (float) player.getCurrentPosition() / player.getDuration();

        if (progress < 0.25) {
            return;
        }

        if (!playReached25) {
            playReached25 = true;
            sendAnalyticsEvent(KavaEvents.PLAY_REACHED_25_PERCENT);
        }

        if (!playReached50 && progress >= 0.5) {
            playReached50 = true;
            sendAnalyticsEvent(KavaEvents.PLAY_REACHED_50_PERCENT);
        }

        if (!playReached75 && progress >= 0.75) {
            playReached75 = true;
            sendAnalyticsEvent(KavaEvents.PLAY_REACHED_75_PERCENT);
        }
    }

    private KavaAnalyticsConfig parsePluginConfig(Object config) {
        if (config instanceof KavaAnalyticsConfig) {
            return (KavaAnalyticsConfig) config;
        } else if (config instanceof JsonObject) {
            return new Gson().fromJson((JsonObject) config, KavaAnalyticsConfig.class);
        }

        return null;
    }

    private void calculateTotalBufferTimePerViewEvent() {
        if (lastKnownBufferingTimestamp == 0) return;
        long currentTime = System.currentTimeMillis();
        long bufferTime = currentTime - lastKnownBufferingTimestamp;
        totalBufferTimePerViewEvent += bufferTime;
        totalBufferTimePerEntry += bufferTime;
        lastKnownBufferingTimestamp = currentTime;
    }

    private void updateDeliveryType(PKMediaFormat mediaFormat) {
        if (mediaFormat == PKMediaFormat.dash) {
            deliveryType = DELIVERY_TYPE_DASH;
        } else if (mediaFormat == PKMediaFormat.hls) {
            deliveryType = DELIVERY_TYPE_HLS;
        } else {
            deliveryType = DELIVERY_TYPE_OTHER;
        }
    }

    private String buildDefaultReferrer() {
        String referrer = "app://" + context.getPackageName();
        return PlaykitUtils.toBase64(referrer.getBytes());
    }

    private String getPlaybackType() {

        if (!player.isLiveStream()) {
            return PKMediaEntry.MediaEntryType.Vod.name();
        }

        long distanceFromLive = player.getDuration() - player.getCurrentPosition();
        return distanceFromLive > DISTANCE_FROM_LIVE_THRESHOLD ? DVR : PKMediaEntry.MediaEntryType.Live.name();
    }

    private void resetFlags() {
        isPaused = true;
        isFirstPlay = true;

        playReached25 = false;
        playReached50 = false;
        playReached75 = false;
        playReached100 = false;

        errorCode = -1;
        totalBufferTimePerEntry = 0;
        totalBufferTimePerViewEvent = 0;
    }

}
