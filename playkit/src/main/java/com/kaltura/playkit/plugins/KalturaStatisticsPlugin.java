package com.kaltura.playkit.plugins;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.backend.ovp.services.StatsService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.connect.SessionProvider;

import java.util.TimerTask;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaStatisticsPlugin extends PKPlugin {

    /*
         * Kaltura event types that are presently not usable in the
		 *  player at this point in time:
		 *
		 * OPEN_EDIT = 8;
		 * OPEN_VIRAL = 9;
		 * OPEN_DOWNLOAD = 10;
		 * OPEN_REPORT = 11;
		 * OPEN_UPLOAD = 18;
		 * SAVE_PUBLISH = 19;
		 * CLOSE_EDITOR = 20;
		 *
		 * PRE_BUMPER_PLAYED = 21;
		 * POST_BUMPER_PLAYED = 22;
		 * BUMPER_CLICKED = 23;
		 */

    public enum KStatsEvent {
        WIDGET_LOADED(1),
        MEDIA_LOADED(2),
        PLAY(3),
        PLAY_REACHED_25(4),
        PLAY_REACHED_50(5),
        PLAY_REACHED_75(6),
        PLAY_REACHED_100(7),
        OPEN_EDIT(8),
        OPEN_VIRAL(9),
        OPEN_DOWNLOAD(10),
        OPEN_REPORT(11),
        BUFFER_START(12),
        BUFFER_END(13),
        OPEN_FULL_SCREEN(14),
        CLOSE_FULL_SCREEN(15),
        REPLAY(16),
        SEEK(17),
        OPEN_UPLOAD(18),
        SAVE_PUBLISH(19),
        CLOSE_EDITOR(20),
        PRE_BUMPER_PLAYED(21),
        POST_BUMPER_PLAYED(22),
        BUMPER_CLICKED(23),
        PREROLL_STARTED(24),
        MIDROLL_STARTED(25),
        POSTROLL_STARTED(26),
        OVERLAY_STARTED(27),
        PREROLL_CLICKED(28),
        MIDROLL_CLICKED(29),
        POSTROLL_CLICKED(30),
        OVERLAY_CLICKED(31),
        PREROLL_25(32),
        PREROLL_50(33),
        PREROLL_75(34),
        MIDROLL_25(35),
        MIDROLL_50(36),
        MIDROLL_75(37),
        POSTROLL_25(38),
        POSTROLL_50(39),
        POSTROLL_75(40),
        ERROR(99);

        private final int value;

        KStatsEvent(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private Player player;
    private PlayerConfig.Media mediaConfig;
    private JsonObject pluginConfig;
    private RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();
    private float seekPercent = 0;
    private boolean playReached25 = false;
    private boolean playReached50 = false;
    private boolean playReached75 = false;
    private boolean playReached100 = false;
    private boolean isBuffering = false;
    private boolean intervalOn = false;
    private boolean hasSeeked = false;
    private static final int TimerInterval = 10000;

    private JsonObject examplePluginConfig;

    public JsonObject getExamplePluginConfig() {
        examplePluginConfig = new JsonObject();
        examplePluginConfig.addProperty("clientVer","2.5");
        examplePluginConfig.addProperty("sessionId","b3460681-b994-6fad-cd8b-f0b65736e837");
        examplePluginConfig.addProperty("uiconfId","24997472");
        examplePluginConfig.addProperty("IsFriendlyIframe",);
        return examplePluginConfig;
    }

    SessionProvider ksSessionProvider = new SessionProvider() {
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

    private static final String TAG = "KalturaStatisticsPlugin";

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaStatistics";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaStatisticsPlugin();
        }
    };

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        messageBus.listen(mEventListener, (PKEvent[]) PlayerEvent.values());
        messageBus.listen(mStateChangeListener, PlayerState.EVENT_TYPE);
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.mediaConfig = mediaConfig;
        this.pluginConfig = pluginConfig;
    }

    @Override
    public void onDestroy() {
        intervalOn = false;
        timer.cancel();
    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {
        resetPlayerFlags();
    }

    @Override
    protected void onUpdateConfig(String key, Object value) {

    }

    private PKEvent.Listener mStateChangeListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerState.Event) {
                switch (((PlayerState.Event) event).newState) {
                    case IDLE:
                        setMessageParams(KStatsEvent.WIDGET_LOADED);
                        break;
                    case LOADING:
                        if (isBuffering) {
                            isBuffering = false;
                            setMessageParams(KStatsEvent.BUFFER_END);
                        }
                        break;
                    case READY:
                        if (!isBuffering) {
                            setMessageParams(KStatsEvent.MEDIA_LOADED);
                        } else {
                            isBuffering = false;
                            setMessageParams(KStatsEvent.BUFFER_END);
                        }
                        if (!intervalOn) {
                            intervalOn = true;
                            startTimeObservorInterval();
                        }
                        break;
                    case BUFFERING:
                        isBuffering = true;
                        setMessageParams(KStatsEvent.BUFFER_START);
                        break;
                }
            }
        }
    };

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch ((PlayerEvent) event) {
                    case CAN_PLAY:

                        break;
                    case DURATION_CHANGE:

                        break;
                    case ENDED:

                        break;
                    case ERROR:
                        setMessageParams(KStatsEvent.ERROR);
                        break;
                    case LOADED_METADATA:

                        break;
                    case PAUSE:

                        break;
                    case PLAY:
                        setMessageParams(KStatsEvent.PLAY);
                        break;
                    case PLAYING:

                        break;
                    case SEEKED:
                        hasSeeked = true;
                        seekPercent = (float) player.getCurrentPosition() / player.getDuration();
                        setMessageParams(KStatsEvent.SEEK);
                        break;
                    case SEEKING:

                        break;
                    default:

                        break;
                }
            }
        }
    };

    private void resetPlayerFlags(){
        seekPercent = 0;
        playReached25 = false;
        playReached50 = false;
        playReached75 = false;
        playReached100 = false;
        hasSeeked = false;
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
                    setMessageParams(KStatsEvent.PLAY_REACHED_25);
                } else if (progress >= 0.5 && !playReached50 && seekPercent < 0.5) {
                    playReached50 = true;
                    setMessageParams(KStatsEvent.PLAY_REACHED_50);
                } else if (progress >= 0.75 && !playReached75 && seekPercent <= 0.75) {
                    playReached75 = true;
                    setMessageParams(KStatsEvent.PLAY_REACHED_75);
                } else if (progress >= 0.98 && !playReached100 && seekPercent < 1) {
                    playReached100 = true;
                    setMessageParams(KStatsEvent.PLAY_REACHED_100);
                }
            }
        }, 0, TimerInterval);
    }

    private void setMessageParams(KStatsEvent eventType) {
        // Parameters for the request -
        //        String baseUrl, int partnerId, int eventType, String clientVer, long duration,
        //        String sessionId, long position, String uiConfId, String entryId, String widgetId, String kalsig, boolean isSeek, String referrer
        RequestBuilder requestBuilder = StatsService.sendStatsEvent(ksSessionProvider.baseUrl(), ksSessionProvider.partnerId(), eventType.getValue(), "2.5", player.getDuration(),
                "b3460681-b994-6fad-cd8b-f0b65736e837", player.getCurrentPosition(), "24997472", mediaConfig.getMediaEntry().getId(), "_" + ksSessionProvider.partnerId(),
                hasSeeked, "");

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                Log.d(TAG, "onComplete: ");
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

}
