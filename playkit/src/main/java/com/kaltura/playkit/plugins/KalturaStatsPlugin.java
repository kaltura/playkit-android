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
import com.kaltura.playkit.backend.ovp.services.StatsService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;

import java.util.TimerTask;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaStatsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("KalturaStatsPlugin");
    private static final String TAG = "KalturaStatsPlugin";

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

    private enum KStatsEvent {
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
    private MessageBus messageBus;
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

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaStats";
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
        log.d("onLoad finished");
//        setExamplePluginConfig(); // Until full implementation of config object
    }

    @Override
    public void onDestroy() {
        log.d("onDestroy");
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

    public void onEvent(PlayerEvent.StateChanged event) {
        log.d(event.newState.toString());
        switch (event.newState) {
            case IDLE:
                sendAnalyticsEvent(KStatsEvent.WIDGET_LOADED);
                break;
            case LOADING:
                if (isBuffering) {
                    isBuffering = false;
                    sendAnalyticsEvent(KStatsEvent.BUFFER_END);
                }
                break;
            case READY:
                if (!isBuffering) {
                    sendAnalyticsEvent(KStatsEvent.MEDIA_LOADED);
                } else {
                    isBuffering = false;
                    sendAnalyticsEvent(KStatsEvent.BUFFER_END);
                }
                if (!intervalOn) {
                    intervalOn = true;
                    startTimerInterval();
                }
                break;
            case BUFFERING:
                isBuffering = true;
                sendAnalyticsEvent(KStatsEvent.BUFFER_START);
                break;
        }
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                log.d(((PlayerEvent) event).type.toString());
                switch (((PlayerEvent) event).type) {
                    case STATE_CHANGED:
                        KalturaStatsPlugin.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    case ERROR:
                        sendAnalyticsEvent(KStatsEvent.ERROR);
                        break;

                    case PLAY:
                        sendAnalyticsEvent(KStatsEvent.PLAY);
                        break;
                    case SEEKED:
                        hasSeeked = true;
                        seekPercent = (float) player.getCurrentPosition() / player.getDuration();
                        sendAnalyticsEvent(KStatsEvent.SEEK);
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
    }

    private void startTimerInterval() {
        if (timer == null) {
            timer = new java.util.Timer();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                float progress = (float) player.getCurrentPosition() / player.getDuration();
                if (progress >= 0.25 && !playReached25 && seekPercent <= 0.25) {
                    playReached25 = true;
                    sendAnalyticsEvent(KStatsEvent.PLAY_REACHED_25);
                } else if (progress >= 0.5 && !playReached50 && seekPercent < 0.5) {
                    playReached50 = true;
                    sendAnalyticsEvent(KStatsEvent.PLAY_REACHED_50);
                } else if (progress >= 0.75 && !playReached75 && seekPercent <= 0.75) {
                    playReached75 = true;
                    sendAnalyticsEvent(KStatsEvent.PLAY_REACHED_75);
                } else if (progress >= 0.98 && !playReached100 && seekPercent < 1) {
                    playReached100 = true;
                    sendAnalyticsEvent(KStatsEvent.PLAY_REACHED_100);
                }
            }
        }, 0, TimerInterval);
    }

    private void sendAnalyticsEvent(final KStatsEvent eventType) {
        String clientVer = pluginConfig.has("clientVer")? pluginConfig.getAsJsonPrimitive("clientVer").getAsString(): "";
        String sessionId = pluginConfig.has("sessionId")? pluginConfig.getAsJsonPrimitive("sessionId").getAsString(): "";
        int uiconfId = pluginConfig.has("uiconfId")? pluginConfig.getAsJsonPrimitive("uiconfId").getAsInt(): 0;
        String referrer = pluginConfig.has("IsFriendlyIframe")? pluginConfig.getAsJsonPrimitive("IsFriendlyIframe").getAsString(): "";
        String baseUrl = pluginConfig.has("baseUrl")? pluginConfig.getAsJsonPrimitive("baseUrl").getAsString(): "";
        int partnerId = pluginConfig.has("partnerId")? pluginConfig.getAsJsonPrimitive("partnerId").getAsInt(): 0;

        // Parameters for the request -
        //        String baseUrl, int partnerId, int eventType, String clientVer, long duration,
        //        String sessionId, long position, String uiConfId, String entryId, String widgetId, String kalsig, boolean isSeek, String referrer
        RequestBuilder requestBuilder = StatsService.sendStatsEvent(baseUrl, partnerId, eventType.getValue(), clientVer, player.getDuration(),
                sessionId, player.getCurrentPosition(), uiconfId, mediaConfig.getMediaEntry().getId(), "_" + partnerId,
                hasSeeked, referrer);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                log.d("onComplete send event: " + eventType.toString());
                messageBus.post(new LogEvent(TAG + " " + eventType.toString()));
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

}
