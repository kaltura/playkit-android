package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.SessionProvider;

import java.util.TimerTask;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaLiveStatsPlugin extends PKPlugin {
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

    private static final String TAG = "KalturaLiveStatsPlugin";

    private Player player;
    private PlayerConfig.Media mediaConfig;
    private JsonObject pluginConfig;
    private MessageBus messageBus;
    private RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();
    private int eventIdx = 0;
    private int currentBitrate = -1;
    private int bufferTime = 0;
    private boolean isLive = false;
    private boolean isFirstPlay = true;

    private static final int TimerInterval = 10000;

    private void setExamplePluginConfig() {
        pluginConfig = new JsonObject();
        pluginConfig.addProperty("clientVer", "2.5");
        pluginConfig.addProperty("sessionId", "b3460681-b994-6fad-cd8b-f0b65736e837");
        pluginConfig.addProperty("deliveryType", 24997472);
        pluginConfig.addProperty("IframeParentUrl","" );
    }

    private SessionProvider OVPSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return "livestats.kaltura.com";
        }

        @Override
        public String getKs() {
            return "djJ8MjIxOTY4MXzAAQiLqlMdKikGdTFiGfnsMLUrAO7_E2zPIlHY9ujBkStzvj9rpgGYW1tA-qC8SNJeTdvES6YeB5ToOsgwAmu_0B2U5OXKQlJqpT2cZGfKG8PP7Zodjl3SYtzoSDe4e_cmiTyWtwo_Dknoh8L6X9_Q";
        }

        @Override
        public int partnerId() {
            return 2219681;
        }
    };

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaLiveStatistics";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaLiveStatsPlugin();
        }
    };

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        messageBus.listen(mEventListener, PlayerEvent.Type.STATE_CHANGED);
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.mediaConfig = mediaConfig;
        this.pluginConfig = pluginConfig;
        this.messageBus = messageBus;
        setExamplePluginConfig(); // Until full implementation of config object
    }

    @Override
    public void onDestroy() {

    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {
        eventIdx = 0;
    }

    @Override
    protected void onUpdateConfig(String key, Object value) {

    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch (((PlayerEvent) event).type) {
                    case STATE_CHANGED:
                        KalturaLiveStatsPlugin.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    default:
                        break;
                }
            }
        }
    };

    public void onEvent(PlayerEvent.StateChanged event) {
        switch (event.newState) {
            case IDLE:

                break;
            case LOADING:

                break;
            case READY:
                startTimerInterval();
                break;
            case BUFFERING:

                break;
        }
    }

    private void startTimerInterval() {
        if (timer == null) {
            timer = new java.util.Timer();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

            }
        }, 0, TimerInterval);
    }

    /*
    private void setMessageParams(final KalturaStatisticsPlugin.KStatsEvent eventType) {
        String clientVer = pluginConfig.has("clientVer")? pluginConfig.get("clientVer").toString(): "";
        String sessionId = pluginConfig.has("sessionId")? pluginConfig.get("sessionId").toString(): "";
        int uiconfId = pluginConfig.has("uiconfId")? Integer.getInteger(pluginConfig.get("uiconfId").toString()): 0;
        String referrer = pluginConfig.has("IsFriendlyIframe")? pluginConfig.get("IsFriendlyIframe").toString(): "";


        // Parameters for the request -
        // String baseUrl, int partnerId, int eventType, int eventIndex, int bufferTime, int bitrate,
        // String sessionId, String startTime,  String entryId,  boolean isLive, String referrer
        RequestBuilder requestBuilder = StatsService.sendStatsEvent(OVPSessionProvider.baseUrl(), OVPSessionProvider.partnerId(), eventType.getValue(), eventIdx++, player.getBufferedPosition(),
                player. , sessionId, mediaConfig.getStartPosition(), mediaConfig.getMediaEntry().getId(), isLive, referrer);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                Log.d(TAG, "onComplete: " + eventType.toString());
                messageBus.post(new LogEvent(TAG + " " + eventType.toString()));
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }
    */
}
