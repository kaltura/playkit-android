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

package com.kaltura.playkit.plugins.ott;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.api.phoenix.services.BookmarkService;
import com.kaltura.playkit.utils.Consts;

import java.util.Timer;
import java.util.TimerTask;


public class PhoenixAnalyticsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("PhoenixAnalyticsPlugin");
    private static final double MEDIA_ENDED_THRESHOLD = 0.98;

    // Fields shared with TVPAPIAnalyticsPlugin
    int mediaHitInterval;
    Timer timer;
    Player player;
    Context context;
    MessageBus messageBus;
    PKMediaConfig mediaConfig;
    RequestQueue requestsExecutor;

    String fileId;
    String baseUrl;
    long lastKnownPlayerPosition = 0;

    private String ks;
    private int partnerId;
    private boolean intervalOn = false;
    private boolean isFirstPlay = true;
    private boolean isMediaFinished = false;
    private boolean timerWasCancelled = false;

    enum PhoenixActionType {
        HIT,
        PLAY,
        STOP,
        PAUSE,
        FIRST_PLAY,
        SWOOSH,
        LOAD,
        FINISH,
        BITRATE_CHANGE,
        ERROR
    }

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "PhoenixAnalytics";
        }

        @Override
        public PKPlugin newInstance() {
            return new PhoenixAnalyticsPlugin();
        }

        @Override
        public void warmUp(Context context) {

        }
    };

    @Override
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        log.d("onLoad");

        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.context = context;
        this.messageBus = messageBus;
        this.timer = new Timer();
        setConfigMembers(config);
        if (baseUrl != null && !baseUrl.isEmpty() && partnerId > 0) {
            messageBus.listen(mEventListener, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.ENDED, PlayerEvent.Type.ERROR, PlayerEvent.Type.LOADED_METADATA, PlayerEvent.Type.STOPPED, PlayerEvent.Type.REPLAY, PlayerEvent.Type.SEEKED, PlayerEvent.Type.SOURCE_SELECTED);
        } else {
            log.e("Error, base url/partner - incorrect");
        }
    }

    private void setConfigMembers(Object config) {
        PhoenixAnalyticsConfig pluginConfig = parseConfig(config);
        this.baseUrl = pluginConfig.getBaseUrl();
        this.partnerId = pluginConfig.getPartnerId();
        this.ks = pluginConfig.getKS();
        this.mediaHitInterval = (pluginConfig.getTimerInterval() > 0) ? pluginConfig.getTimerInterval() * (int) Consts.MILLISECONDS_MULTIPLIER : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH;
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;
        isFirstPlay = true;
        isMediaFinished = false;
    }

    @Override
    protected void onUpdateConfig(Object config) {
        setConfigMembers(config);
        if (baseUrl == null || baseUrl.isEmpty() || partnerId >= 0) {
            cancelTimer();
            messageBus.remove(mEventListener, (Enum[]) PlayerEvent.Type.values());
        }
    }

    @Override
    protected void onApplicationPaused() {
        log.d("onApplicationPaused");
        cancelTimer();
    }

    @Override
    protected void onApplicationResumed() {
        log.d("onApplicationResumed");
        timer = new Timer();
    }

    @Override
    public void onDestroy() {
        log.d("onDestroy");
        cancelTimer();
        timerWasCancelled = true;
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                log.d("Player Event = " + ((PlayerEvent) event).type.name() + " , lastKnownPlayerPosition = " + lastKnownPlayerPosition);
                switch (((PlayerEvent) event).type) {
                    case STOPPED:
                        if (isMediaFinished) {
                            return;
                        }
                        sendAnalyticsEvent(PhoenixActionType.STOP);
                        resetTimer();
                        break;
                    case ENDED:
                        resetTimer();
                        sendAnalyticsEvent(PhoenixActionType.FINISH);
                        isMediaFinished = true;
                        break;
                    case ERROR:
                        resetTimer();
                        sendAnalyticsEvent(PhoenixActionType.ERROR);
                        break;
                    case LOADED_METADATA:
                        sendAnalyticsEvent(PhoenixActionType.LOAD);
                        break;
                    case SOURCE_SELECTED:
                        PlayerEvent.SourceSelected sourceSelected = (PlayerEvent.SourceSelected) event;
                        fileId = sourceSelected.source.getId();
                        break;
                    case PAUSE:
                        if (isMediaFinished) {
                            return;
                        }
                        sendAnalyticsEvent(PhoenixActionType.PAUSE);
                        resetTimer();
                        intervalOn = false;
                        break;
                    case PLAY:
                        if (isMediaFinished) {
                            return;
                        }
                        if (isFirstPlay) {
                            isFirstPlay = false;
                            sendAnalyticsEvent(PhoenixActionType.FIRST_PLAY);
                        } else {
                            sendAnalyticsEvent(PhoenixActionType.PLAY);
                        }
                        if (!intervalOn || !timerWasCancelled) {
                            startMediaHitInterval();
                            intervalOn = true;
                        }
                        break;
                    case SEEKED:
                    case REPLAY:
                        //Receiving one of this events, mean that media position was reset.
                        isMediaFinished = false;
                        break;
                    default:
                        break;
                }
            }
        }
    };

    void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void resetTimer() {
        cancelTimer();
        timer = new Timer();
    }

    /**
     * Media Hit analytics event
     */
    private void startMediaHitInterval() {
        log.d("timer interval");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendAnalyticsEvent(PhoenixActionType.HIT);
                lastKnownPlayerPosition = player.getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER;
                if ((float) lastKnownPlayerPosition / player.getDuration() > MEDIA_ENDED_THRESHOLD) {
                    sendAnalyticsEvent(PhoenixActionType.FINISH);
                    isMediaFinished = true;
                }
            }
        }, 0, mediaHitInterval); // Get media hit interval from plugin config
    }

    /**
     * Send Bookmark/add event using Kaltura Phoenix Rest API
     *
     * @param eventType - Enum stating the event type to send
     */
    protected void sendAnalyticsEvent(final PhoenixActionType eventType) {

        if (eventType != PhoenixActionType.STOP) {
            lastKnownPlayerPosition = player.getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER;
        }
        RequestBuilder requestBuilder = BookmarkService.actionAdd(baseUrl, partnerId, ks,
                "media", mediaConfig.getMediaEntry().getId(), eventType.name(), lastKnownPlayerPosition, fileId);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if (response.isSuccess() && response.getError() != null && response.getError().getCode().equals("4001")) {
                    messageBus.post(new OttEvent(OttEvent.OttEventType.Concurrency));
                    messageBus.post(new PhoenixAnalyticsEvent.PhoenixAnalyticsReport(eventType.toString()));
                }

                log.d("onComplete send event: " + eventType);
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

    PKEvent.Listener getEventListener() {
        return mEventListener;
    }


    private static PhoenixAnalyticsConfig parseConfig(Object config) {
        if (config instanceof PhoenixAnalyticsConfig) {
            return ((PhoenixAnalyticsConfig) config);

        } else if (config instanceof JsonObject) {
            JsonObject params = (JsonObject) config;
            String baseUrl = params.get("baseUrl").getAsString();
            int partnerId = params.get("partnerId").getAsInt();
            int timerInterval = params.get("timerInterval").getAsInt();
            String ks = params.get("ks").getAsString();

            return new PhoenixAnalyticsConfig(partnerId, baseUrl, ks, timerInterval);
        }
        return null;
    }
}
