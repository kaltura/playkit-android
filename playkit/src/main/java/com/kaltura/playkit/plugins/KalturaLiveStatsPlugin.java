package com.kaltura.playkit.plugins;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.kaltura.playkit.LogEvent;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.backend.ovp.services.LiveStatsService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;

import java.util.Date;
import java.util.TimerTask;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaLiveStatsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("KalturaLiveStatsPlugin");
    private static final String TAG = "KalturaLiveStatsPlugin";

    private boolean isBuffering = false;

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


    private Player player;
    private PlayerConfig.Media mediaConfig;
    private JsonObject pluginConfig;
    private MessageBus messageBus;
    private RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();
    private int eventIdx = 0;
    private int currentBitrate = -1;
    private long bufferTime = 0;
    private long bufferStartTime = 0;
    private boolean isLive = false;
    private boolean isFirstPlay = true;

    private static final int TimerInterval = 10000;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaLiveStats";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaLiveStatsPlugin();
        }
    };

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        messageBus.listen(mEventListener, PlayerEvent.Type.STATE_CHANGED, PlayerEvent.Type.PAUSE, PlayerEvent.Type.PLAY);
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.mediaConfig = mediaConfig;
        this.pluginConfig = pluginConfig;
        this.messageBus = messageBus;
    }

    @Override
    public void onDestroy() {
        stopLiveEvents();
        eventIdx = 0;
    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {
        eventIdx = 0;
        this.mediaConfig = mediaConfig;
    }

    @Override
    protected void onUpdateConfig(String key, Object value) {
        if (pluginConfig.has(key)){
            pluginConfig.addProperty(key, value.toString());
        }
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
                    default:
                        break;
                }
            }
        }
    };

    public void onEvent(PlayerEvent.StateChanged event) {
        switch (event.newState) {
            case READY:
                startTimerInterval();
                if (isBuffering){
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

    private long calculateBuffer(boolean isBuffering){
        long currTime = new Date().getTime();
        bufferTime =  (currTime - bufferStartTime) / 1000;
        if (bufferTime > 10){
            bufferTime = 10;
        }
        if (isBuffering){
            bufferStartTime = new Date().getTime();
        } else {
            bufferStartTime = -1;
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
        }, 0, TimerInterval);
    }

    private void startLiveEvents(){
        if (!isLive) {
            startTimerInterval();
            isLive = true;
            if (isFirstPlay){
                sendLiveEvent(bufferTime);
                isFirstPlay = false;
            }

        }
    }

    private void stopLiveEvents(){
        isLive = false;
        timer.cancel();
    }

    private void sendLiveEvent(long bufferTime) {
        String sessionId = pluginConfig.has("sessionId")? pluginConfig.getAsJsonPrimitive("sessionId").getAsString(): "";
        String baseUrl = pluginConfig.has("baseUrl")? pluginConfig.getAsJsonPrimitive("baseUrl").getAsString(): "";
        int partnerId = pluginConfig.has("partnerId")? pluginConfig.getAsJsonPrimitive("partnerId").getAsInt(): 0;

        // Parameters for the request -
        // String baseUrl, int partnerId, int eventType, int eventIndex, int bufferTime, int bitrate,
        // String sessionId, String startTime,  String entryId,  boolean isLive, String referrer
        RequestBuilder requestBuilder = LiveStatsService.sendLiveStatsEvent(baseUrl, partnerId, isLive? 1:2, eventIdx++, bufferTime,
                /*player.getBitrate() */ 0, sessionId, mediaConfig.getStartPosition(), mediaConfig.getMediaEntry().getId(), isLive);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                Log.d(TAG, "onComplete: " + isLive);
                messageBus.post(new LogEvent(TAG + " " + isLive));
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }
}
