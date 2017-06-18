package com.kaltura.playkit.plugins;

import android.content.Context;
import android.util.Log;

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
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.api.ovp.services.LiveStatsService;
import com.kaltura.playkit.plugins.configs.KalturaLiveStatsConfig;
import com.kaltura.playkit.plugins.configs.KalturaStatsConfig;
import com.kaltura.playkit.utils.Consts;

import java.util.Date;
import java.util.TimerTask;

import static com.kaltura.playkit.plugins.KalturaLiveStatsPlugin.KLiveStatsEvent.DVR;
import static com.kaltura.playkit.plugins.KalturaLiveStatsPlugin.KLiveStatsEvent.LIVE;


/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaLiveStatsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("KalturaLiveStatsPlugin");
    private static final String TAG = "KalturaLiveStatsPlugin";

    private boolean isBuffering = false;
    private long lastReportedBitrate = -1;
    private Player player;
    private PKMediaConfig mediaConfig;
    private KalturaLiveStatsConfig pluginConfig;
    private MessageBus messageBus;
    private RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();
    private int eventIdx = 1;
    private long bufferTime = 0;
    private long bufferStartTime = 0;
    private KLiveStatsEvent liveStatsEvent = LIVE;
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
        messageBus.listen(mEventListener, PlayerEvent.Type.STATE_CHANGED, PlayerEvent.Type.PAUSE, PlayerEvent.Type.PLAY, PlayerEvent.Type.PLAYBACK_INFO_UPDATED);
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.pluginConfig = parseConfig(config);
        liveStatsEvent = pluginConfig.getIsDVR() ? DVR: LIVE;
        this.messageBus = messageBus;
    }

    @Override
    public void onDestroy() {
        stopLiveEvents();
        eventIdx = 1;
        cancelTimer();
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        eventIdx = 1;
        this.mediaConfig = mediaConfig;
    }

    @Override
    protected void onUpdateConfig(Object config) {
        this.pluginConfig = parseConfig(config);
        liveStatsEvent = pluginConfig.getIsDVR() ? DVR: LIVE;
    }

    private static KalturaLiveStatsConfig parseConfig(Object config) {
        if (config instanceof KalturaStatsConfig) {
            return ((KalturaLiveStatsConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), KalturaLiveStatsConfig.class);
        }
        return null;
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
                        onStateChanged(((PlayerEvent.StateChanged) event).newState);
                        break;
                    case PLAY:
                        startLiveEvents();
                        break;
                    case PAUSE:
                        stopLiveEvents();
                        break;
                    case PLAYBACK_INFO_UPDATED:
                        PlaybackInfo currentPlaybackInfo = ((PlayerEvent.PlaybackInfoUpdated) event).getPlaybackInfo();
                        lastReportedBitrate = currentPlaybackInfo.getVideoBitrate();
                    default:
                        break;
                }
            }
        }
    };

    private void onStateChanged(PlayerState newState) {
        switch (newState) {
            case READY:
                if (isBuffering) {
                    isBuffering = false;
                    calculateBuffer();
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

    private long calculateBuffer() {
        long currTime = new Date().getTime();
        bufferTime = (currTime - bufferStartTime) / 1000;
        if (bufferTime > 10) {
            bufferTime = 10;
        }
        return bufferTime;
    }

    private void startTimerInterval() {
        if (timer == null) {
            timer = new java.util.Timer();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendLiveEvent(bufferTime);
            }
        }, 0, Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_LOW); // server is making assumption that interval is 10 sec
    }

    private void startLiveEvents() {
        startTimerInterval();
    }

    private void stopLiveEvents() {
        cancelTimer();
    }

    private void sendLiveEvent(final long bufferTime) {
        String sessionId = (player.getSessionId() != null) ? player.getSessionId().toString() : "";

        // Parameters for the request -
        // String baseUrl, int partnerId, int eventType, int eventIndex, int bufferTime, int bitrate,
        // String startTime,  String entryId,  boolean isLive, String referrer

        RequestBuilder requestBuilder = LiveStatsService.sendLiveStatsEvent(pluginConfig.getBaseUrl(), pluginConfig.getPartnerId(), liveStatsEvent.value, eventIdx++, bufferTime,
                lastReportedBitrate, sessionId, mediaConfig.getStartPosition(), pluginConfig.getEntryId(), player.isLiveStream(), PlayKitManager.CLIENT_TAG, "NA");

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                Log.d(TAG, "onComplete: liveStatsEvent = " + liveStatsEvent.name());
                messageBus.post(new KalturaLiveStatsEvent.KalturaLiveStatsReport(bufferTime));
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}

