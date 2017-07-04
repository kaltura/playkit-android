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
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlaybackInfo;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.api.ovp.services.LiveStatsService;
import com.kaltura.playkit.utils.Consts;

import java.util.Date;
import java.util.TimerTask;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaLiveStatsPlugin extends PKPlugin {

    private static final PKLog log = PKLog.get("KalturaLiveStatsPlugin");
    private static final int DISTANCE_FROM_LIVE_THRESHOLD = 15 * 1000; //15 sec

    private Player player;
    private MessageBus messageBus;
    private PKMediaConfig mediaConfig;
    private RequestQueue requestsExecutor;
    private KalturaLiveStatsConfig pluginConfig;
    private java.util.Timer timer = new java.util.Timer();

    private int eventIndex = 1;
    private long bufferTime = 0;
    private long bufferStartTime = 0;
    private long lastReportedBitrate = -1;

    private String playbackProtocol;

    private boolean isLive = false;
    private boolean isBuffering = false;
    private boolean isFirstPlay = true;

    public enum KLiveStatsEvent {
        LIVE(1),
        DVR(2);

        private final int value;

        KLiveStatsEvent(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaLiveStats";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaLiveStatsPlugin();
        }

        @Override
        public void warmUp(Context context) {

        }
    };

    @Override
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        this.player = player;
        this.messageBus = messageBus;
        this.pluginConfig = parseConfig(config);
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.messageBus.listen(mEventListener, PlayerEvent.Type.STATE_CHANGED, PlayerEvent.Type.PAUSE, PlayerEvent.Type.PLAY, PlayerEvent.Type.PLAYBACK_INFO_UPDATED, PlayerEvent.Type.SOURCE_SELECTED);
    }

    @Override
    public void onDestroy() {
        stopLiveEvents();
        eventIndex = 1;
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        eventIndex = 1;
        this.mediaConfig = mediaConfig;
    }

    @Override
    protected void onUpdateConfig(Object config) {
        this.pluginConfig = parseConfig(config);
    }

    @Override
    protected void onApplicationPaused() {
        cancelTimer();
    }

    @Override
    protected void onApplicationResumed() {
        startTimerInterval();
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch (((PlayerEvent) event).type) {
                    case STATE_CHANGED:
                        KalturaLiveStatsPlugin.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    case PLAY:
                        startLiveEvents();
                        break;
                    case PAUSE:
                        stopLiveEvents();
                        break;
                    case PLAYBACK_INFO_UPDATED:
                        PlaybackInfo currentPlaybackInfo = ((PlayerEvent.PlaybackInfoUpdated) event).playbackInfo;
                        lastReportedBitrate = currentPlaybackInfo.getVideoBitrate();
                        log.d("lastReportedBitrate = " + lastReportedBitrate + ", isLiveStream = " + currentPlaybackInfo.getIsLiveStream());
                        break;
                    case SOURCE_SELECTED:
                        PlayerEvent.SourceSelected sourceSelected = (PlayerEvent.SourceSelected) event;
                        switch (sourceSelected.source.getMediaFormat()) {
                            case hls:
                                playbackProtocol = "hls";
                                break;
                            case dash:
                                playbackProtocol = "mpegdash";
                                break;
                            default:
                                playbackProtocol = "NA";
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };

    public void onEvent(PlayerEvent.StateChanged event) {
        switch (event.newState) {
            case READY:
                if (isBuffering) {
                    isBuffering = false;
                    sendLiveEvent(calculateBuffer(false));
                }
                break;
            case BUFFERING:
                isBuffering = true;
                bufferStartTime = new Date().getTime();
                break;
            default:
                break;
        }
    }

    private long calculateBuffer(boolean isBuffering) {
        long currTime = new Date().getTime();
        bufferTime = (currTime - bufferStartTime) / 1000;
        if (bufferTime > 10) {
            bufferTime = 10;
        }
        if (isBuffering) {
            bufferStartTime = new Date().getTime();
        } else {
            bufferStartTime = -1;
        }
        return bufferTime;
    }

    private void startTimerInterval() {
        log.d("startTimerInterval");
        if (timer == null) {
            timer = new java.util.Timer();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendLiveEvent(bufferTime);
            }
        }, Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_LOW, Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_LOW);
    }

    private void startLiveEvents() {
        if (!isLive) {
            startTimerInterval();
            isLive = true;
            if (isFirstPlay) {
                sendLiveEvent(bufferTime);
                isFirstPlay = false;
            }
        }
    }

    private void stopLiveEvents() {
        isLive = false;
        cancelTimer();
    }

    private void sendLiveEvent(final long bufferTime) {
        String sessionId = (player.getSessionId() != null) ? player.getSessionId() : "";

        long distanceFromLive = 0;
        if (player != null) {
            distanceFromLive = player.getDuration() - player.getCurrentPosition();
        }

        RequestBuilder requestBuilder = LiveStatsService.sendLiveStatsEvent(pluginConfig.getBaseUrl(),
                pluginConfig.getPartnerId(),
                (distanceFromLive <= DISTANCE_FROM_LIVE_THRESHOLD) ? KLiveStatsEvent.LIVE.value : KLiveStatsEvent.DVR.value,
                eventIndex++, bufferTime,
                lastReportedBitrate,
                sessionId, mediaConfig.getStartPosition(),
                pluginConfig.getEntryId(),
                isLive,
                PlayKitManager.CLIENT_TAG,
                (playbackProtocol != null) ? playbackProtocol : "NA");

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                log.d("onComplete: " + isLive);
                messageBus.post(new KalturaLiveStatsEvent.KalturaLiveStatsReport(bufferTime));
            }
        });

        requestsExecutor.queue(requestBuilder.build());
    }

    private static KalturaLiveStatsConfig parseConfig(Object config) {
        if (config instanceof KalturaLiveStatsConfig) {
            return ((KalturaLiveStatsConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), KalturaLiveStatsConfig.class);
        }
        return null;
    }

    private void cancelTimer() {
        log.d("cancelTimer");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
