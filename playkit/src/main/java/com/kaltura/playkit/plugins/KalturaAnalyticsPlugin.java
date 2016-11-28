package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.LogEvent;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.backend.ovp.services.AnalyticsService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;

import java.util.TimerTask;

/**
 * Created by zivilan on 27/11/2016.
 */

public class KalturaAnalyticsPlugin extends PKPlugin{
    private static final PKLog log = PKLog.get("KalturaAnalyticsPlugin");
    private static final String TAG = "KalturaAnalyticsPlugin";

    private enum KAnalonyEvents {
        IMPRESSION(1),
        PLAY_REQUEST(2),
        PLAY(3),
        RESUME(4),
        PLAY_25PERCENT(11),
        PLAY_50PERCENT(12),
        PLAY_75PERCENT(13),
        PLAY_100PERCENT(14),
        SHARE_CLICKED(21),
        SHARE_NETWORK(22),
        DOWNLOAD(23),
        REPORT_CLICKED(24),
        REPORT_SUBMITED(25),
        ENTER_FULLSCREEN(31),
        EXIT_FULLSCREEN(32),
        PAUSE(33),
        REPLAY(34),
        SEEK(35),
        RELATED_CLICKED(36),
        RELATED_SELECTED(37),
        CAPTIONS(38),
        SOURCE_SELECTED(39),
        INFO(40),
        SPEED(41),
        VIEW(99);

        private final int value;

        KAnalonyEvents(int value) {
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

    private float seekPercent = 0;
    private boolean playReached25 = false;
    private boolean playReached50 = false;
    private boolean playReached75 = false;
    private boolean playReached100 = false;
    private boolean isBuffering = false;
    private boolean isDvr = false;
    private int currentBitrate = -1;
    private int bufferTime = 0;
    private int eventIdx = 0;
    private boolean isFirstPlay = true;
    private boolean intervalOn = false;
    private boolean hasSeeked = false;

    private static final int TimerInterval = 10000;

    private void setExamplePluginConfig() {
        pluginConfig = new JsonObject();
        pluginConfig.addProperty("clientVer", "2.5");
        pluginConfig.addProperty("sessionId", "b3460681-b994-6fad-cd8b-f0b65736e837");
        pluginConfig.addProperty("uiconfId", 24997472);
        pluginConfig.addProperty("IsFriendlyIframe","" );
    }

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaAnalytics";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaStatsPlugin();
        }
    };

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        messageBus.listen(mEventListener, (Enum[]) PlayerEvent.Type.values());
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.mediaConfig = mediaConfig;
        this.pluginConfig = pluginConfig;
        this.messageBus = messageBus;
        setExamplePluginConfig(); // Until full implementation of config object
    }

    @Override
    public void onDestroy() {
        intervalOn = false;
        timer.cancel();
    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {
        isFirstPlay = true;
    }

    @Override
    protected void onUpdateConfig(String key, Object value) {

    }

    public void onEvent(PlayerEvent.StateChanged event) {
        switch (event.newState) {
            case IDLE:

                break;
            case LOADING:
                if (isBuffering) {
                    isBuffering = false;
                }
                break;
            case READY:
                resetPlayerFlags();
                if (!isBuffering) {
                    sendAnalyticsEvent(KAnalonyEvents.IMPRESSION);
                } else {
                    isBuffering = false;
                }
                if (!intervalOn) {
                    intervalOn = true;
                    startTimeObservorInterval();
                }
                break;
            case BUFFERING:
                isBuffering = true;
                break;
        }
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch (((PlayerEvent) event).type) {
                    case STATE_CHANGED:
                        KalturaAnalyticsPlugin.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    case CAN_PLAY:

                        break;
                    case DURATION_CHANGE:

                        break;
                    case ENDED:

                        break;
                    case ERROR:

                        break;
                    case LOADED_METADATA:

                        break;
                    case PAUSE:
                        sendAnalyticsEvent(KAnalonyEvents.PAUSE);
                        break;
                    case PLAY:
                        sendAnalyticsEvent(KAnalonyEvents.PLAY_REQUEST);
                        break;
                    case PLAYING:
                        if (isFirstPlay){
                            sendAnalyticsEvent(KAnalonyEvents.PLAY);
                        } else {
                            sendAnalyticsEvent(KAnalonyEvents.RESUME);
                        }
                        break;
                    case SEEKED:
                        hasSeeked = true;
                        seekPercent = (float) player.getCurrentPosition() / player.getDuration();
                        sendAnalyticsEvent(KAnalonyEvents.SEEK);
                        break;
                    case SEEKING:

                        break;
                    default:

                        break;
                }
            }
        }
    };

    private void resetPlayerFlags() {
        seekPercent = 0;
        playReached25 = false;
        playReached50 = false;
        playReached75 = false;
        playReached100 = false;
        hasSeeked = false;
        eventIdx = 0;
    }

    private void startTimeObservorInterval() {
        if (timer == null) {
            timer = new java.util.Timer();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                float progress = (float) player.getCurrentPosition() / player.getDuration();
                if (progress >= 0.25 && !playReached25 && seekPercent <= 0.25) {
                    playReached25 = true;
                    sendAnalyticsEvent(KAnalonyEvents.PLAY_25PERCENT);
                } else if (progress >= 0.5 && !playReached50 && seekPercent < 0.5) {
                    playReached50 = true;
                    sendAnalyticsEvent(KAnalonyEvents.PLAY_25PERCENT);
                } else if (progress >= 0.75 && !playReached75 && seekPercent <= 0.75) {
                    playReached75 = true;
                    sendAnalyticsEvent(KAnalonyEvents.PLAY_25PERCENT);
                } else if (progress >= 0.98 && !playReached100 && seekPercent < 1) {
                    playReached100 = true;
                    sendAnalyticsEvent(KAnalonyEvents.PLAY_25PERCENT);
                }
            }
        }, 0, TimerInterval);
    }

    private void sendAnalyticsEvent(final KAnalonyEvents eventType) {
        String clientVer = pluginConfig.has("clientVer")? pluginConfig.get("clientVer").toString(): "";
        String sessionId = pluginConfig.has("sessionId")? pluginConfig.get("sessionId").toString(): "";
        int uiconfId = pluginConfig.has("uiconfId")? Integer.valueOf(pluginConfig.get("uiconfId").toString()): 0;
        String referrer = pluginConfig.has("IsFriendlyIframe")? pluginConfig.get("IsFriendlyIframe").toString(): "";
        String baseUrl = pluginConfig.has("baseUrl")? pluginConfig.getAsJsonPrimitive("baseUrl").getAsString(): "";
        int partnerId = pluginConfig.has("partnerId")? pluginConfig.getAsJsonPrimitive("partnerId").getAsInt(): 0;
        String playbackType = isDvr? "dvr":"live";
        int flavourId = -1;

        // Parameters for the request -
//        String baseUrl, int partnerId, int eventType, String clientVer, String playbackType, String sessionId, long position
//        ,int uiConfId, String entryId, int eventIdx, int flavourId, String referrer, int bufferTime, int actualBitrate
        RequestBuilder requestBuilder = AnalyticsService.sendAnalyticsEvent(baseUrl, partnerId, eventType.getValue(), clientVer, playbackType,
                sessionId, player.getCurrentPosition(), uiconfId, mediaConfig.getMediaEntry().getId(), eventIdx++, flavourId, referrer, bufferTime, currentBitrate);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                log.d("onComplete: " + eventType.toString());
                messageBus.post(new LogEvent(TAG + " " + eventType.toString()));
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

}

