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

public class KalturaLiveStatisticsPlugin extends PKPlugin {
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
    private static final int TimerInterval = 10000;
    private int eventIdx = 0;
    private boolean isLive = false;

    private JsonObject examplePluginConfig;

    private void setExamplePluginConfig() {
        examplePluginConfig = new JsonObject();
        examplePluginConfig.addProperty("clientVer", "2.5");
        examplePluginConfig.addProperty("sessionId", "b3460681-b994-6fad-cd8b-f0b65736e837");
        examplePluginConfig.addProperty("uiconfId", 24997472);
        examplePluginConfig.addProperty("IsFriendlyIframe","" );
    }

    private SessionProvider OVPSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return "stats.kaltura.com";
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
            return new KalturaLiveStatisticsPlugin();
        }
    };

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        messageBus.listen(mEventListener, (PlayerEvent.Type[]) PlayerEvent.Type.values());
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

    }

    @Override
    protected void onUpdateConfig(String key, Object value) {

    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch (((PlayerEvent) event).type) {
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

                        break;
                    case PLAY:

                        break;
                    case PLAYING:

                        break;
                    case SEEKED:

                        break;
                    case SEEKING:

                        break;
                    default:

                        break;
                }
            }
        }
    };

    private void startTimeObservorInterval() {
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
        String clientVer = examplePluginConfig.has("clientVer")? examplePluginConfig.get("clientVer").toString(): "";
        String sessionId = examplePluginConfig.has("sessionId")? examplePluginConfig.get("sessionId").toString(): "";
        int uiconfId = examplePluginConfig.has("uiconfId")? Integer.getInteger(examplePluginConfig.get("uiconfId").toString()): 0;
        String referrer = examplePluginConfig.has("IsFriendlyIframe")? examplePluginConfig.get("IsFriendlyIframe").toString(): "";


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
