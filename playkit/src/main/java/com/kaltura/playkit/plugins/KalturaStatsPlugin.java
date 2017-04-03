package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.backend.ovp.services.StatsService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.utils.Consts;

import java.util.TimerTask;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaStatsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("KalturaStatsPlugin");
    private static final String TAG = "KalturaStatsPlugin";
    private final String BASE_URL = "https://stats.kaltura.com/api_v3/index.php";


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
    private PKMediaConfig mediaConfig;
    private JsonObject pluginConfig;
    private MessageBus messageBus;
    private RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();
    private AdInfo currentAdInfo;
    private int adCounter = 1;

    private float seekPercent = 0;
    private boolean playReached25 = false;
    private boolean playReached50 = false;
    private boolean playReached75 = false;
    private boolean playReached100 = false;
    private boolean isBuffering = false;
    private boolean intervalOn = false;
    private boolean hasSeeked = false;
    private boolean isWidgetLoaded = false;
    private boolean isMediaLoaded = false;
    private boolean isFirstPlay = true;
    private boolean durationValid = false;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaStats";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaStatsPlugin();
        }

        @Override
        public void warmUp(Context context) {
            
        }
    };

    @Override
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        messageBus.listen(mEventListener, (Enum[]) PlayerEvent.Type.values());
        messageBus.listen(mEventListener, (Enum[]) AdEvent.Type.values());
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.pluginConfig = (JsonObject) config;
        this.messageBus = messageBus;
        log.d("onLoad finished");
    }

    @Override
    public void onDestroy() {
        log.d("onDestroy");
        intervalOn = false;
        timer.cancel();
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;
        resetPlayerFlags();
    }

    @Override
    protected void onUpdateConfig(Object config) {
        this.pluginConfig = (JsonObject) config;
    }

    @Override
    protected void onApplicationPaused() {

    }

    @Override
    protected void onApplicationResumed() {

    }

    private void onEvent(PlayerEvent.StateChanged event) {
        log.d(event.newState.toString());
        switch (event.newState) {
            case IDLE:
                sendWidgetLoaded();
                break;
            case LOADING:
                sendWidgetLoaded();
                if (isBuffering) {
                    isBuffering = false;
                    sendAnalyticsEvent(KStatsEvent.BUFFER_END);
                }
                break;
            case READY:
                if (isBuffering) {
                    isBuffering = false;
                    sendAnalyticsEvent(KStatsEvent.BUFFER_END);
                }
                if (!intervalOn) {
                    intervalOn = true;
                    startTimerInterval();
                }
                sendWidgetLoaded();
                sendMediaLoaded();
                break;
            case BUFFERING:
                sendWidgetLoaded();
                isBuffering = true;
                sendAnalyticsEvent(KStatsEvent.BUFFER_START);
                break;
        }
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (player.getDuration() < 0){
                return;
            }
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
                        sendWidgetLoaded();
                        sendMediaLoaded();
                        if (isFirstPlay) {
                            sendAnalyticsEvent(KStatsEvent.PLAY);
                            isFirstPlay = false;
                        }
                        break;
                    case SEEKED:
                        hasSeeked = true;
                        seekPercent = (float) player.getCurrentPosition() / player.getDuration();
                        sendAnalyticsEvent(KStatsEvent.SEEK);
                        break;
                    case CAN_PLAY:
                        sendWidgetLoaded();
                        sendMediaLoaded();
                        break;
                    case REPLAY:
                        sendAnalyticsEvent(KStatsEvent.REPLAY);
                        break;
                    case DURATION_CHANGE:
                        long currDuration = ((PlayerEvent.DurationChanged) event).duration;
                        if (currDuration >= 0) {
                            durationValid = true;
                        }
                        break;
                    default:
                        break;
                }
            } else if (event instanceof AdEvent){
                KalturaStatsPlugin.this.onEvent((AdEvent) event);
            }
        }
    };


    public void onEvent(AdEvent event) {
        log.d(event.type.toString());
        switch (event.type) {
            case STARTED:
                currentAdInfo = ((AdEvent.AdStartedEvent) event).adInfo;
                if (adCounter == 1){
                    sendAnalyticsEvent(KStatsEvent.PREROLL_STARTED);
                } else if (adCounter == 2){
                    sendAnalyticsEvent(KStatsEvent.MIDROLL_STARTED);
                } else if (adCounter == 3){
                    sendAnalyticsEvent(KStatsEvent.POSTROLL_STARTED);
                }
                break;
            case PAUSED:

                break;
            case RESUMED:

                break;
            case COMPLETED:

                break;
            case FIRST_QUARTILE:
                if (adCounter == 1){
                    sendAnalyticsEvent(KStatsEvent.PREROLL_25);
                } else if (adCounter == 2){
                    sendAnalyticsEvent(KStatsEvent.MIDROLL_25);
                } else if (adCounter == 3){
                    sendAnalyticsEvent(KStatsEvent.POSTROLL_25);
                }
                break;
            case MIDPOINT:
                if (adCounter == 1){
                    sendAnalyticsEvent(KStatsEvent.PREROLL_50);
                } else if (adCounter == 2){
                    sendAnalyticsEvent(KStatsEvent.MIDROLL_50);
                } else if (adCounter == 3){
                    sendAnalyticsEvent(KStatsEvent.POSTROLL_50);
                }
                break;
            case THIRD_QUARTILE:
                if (adCounter == 1){
                    sendAnalyticsEvent(KStatsEvent.PREROLL_75);
                } else if (adCounter == 2){
                    sendAnalyticsEvent(KStatsEvent.MIDROLL_75);
                } else if (adCounter == 3){
                    sendAnalyticsEvent(KStatsEvent.POSTROLL_75);
                }
                break;
            case CLICKED:
                if (adCounter == 1){
                    sendAnalyticsEvent(KStatsEvent.PREROLL_CLICKED);
                } else if (adCounter == 2){
                    sendAnalyticsEvent(KStatsEvent.MIDROLL_CLICKED);
                } else if (adCounter == 3){
                    sendAnalyticsEvent(KStatsEvent.POSTROLL_CLICKED);
                }
                break;
            case ALL_ADS_COMPLETED:
                adCounter++;
                break;
            default:
                break;
        }
        messageBus.post(new KalturaStatsEvent.KalturaStatsReport(event.eventType().toString()));
    }


    private void sendWidgetLoaded() {
        if (!isWidgetLoaded && durationValid) {
            isWidgetLoaded = true;
            sendAnalyticsEvent(KStatsEvent.WIDGET_LOADED);
        }
    }

    private void sendMediaLoaded() {
        if (!isMediaLoaded && durationValid) {
            isMediaLoaded = true;
            sendAnalyticsEvent(KStatsEvent.MEDIA_LOADED);
        }
    }

    /**
     * Reset the flags in case of media change or media ended
     */
    private void resetPlayerFlags() {
        seekPercent = 0;
        playReached25 = false;
        playReached50 = false;
        playReached75 = false;
        playReached100 = false;
        hasSeeked = false;
        isWidgetLoaded = false;
        isMediaLoaded = false;
        isFirstPlay = true;
    }

    /**
     * Time interval handling play reached events
     */
    private void startTimerInterval() {
        int timerInterval = pluginConfig.has("timerInterval") ? pluginConfig.getAsJsonPrimitive("timerInterval").getAsInt() * (int)Consts.MILLISECONDS_MULTIPLIER : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_LOW;
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
        }, 0, timerInterval);
    }

    /**
     * Send stats event to Kaltura stats DB
     *
     * @param eventType - Enum stating Kaltura state events
     */
    private void sendAnalyticsEvent(final KStatsEvent eventType) {
        String sessionId = pluginConfig.has("sessionId") ? pluginConfig.getAsJsonPrimitive("sessionId").getAsString() : "";
        int uiconfId = pluginConfig.has("uiconfId") ? pluginConfig.getAsJsonPrimitive("uiconfId").getAsInt() : 0;
        String baseUrl = pluginConfig.has("baseUrl") ? pluginConfig.getAsJsonPrimitive("baseUrl").getAsString() : BASE_URL;
        int partnerId = pluginConfig.has("partnerId") ? pluginConfig.getAsJsonPrimitive("partnerId").getAsInt() : 0;
        long duration = player.getDuration() == Consts.TIME_UNSET ? -1 : player.getDuration() / 1000;

        // Parameters for the request -
        //        String baseUrl, int partnerId, int eventType, String clientVer, long duration,
        //        String sessionId, long position, String uiConfId, String entryId, String widgetId,  boolean isSeek
        final RequestBuilder requestBuilder = StatsService.sendStatsEvent(baseUrl, partnerId, eventType.getValue(), PlayKitManager.CLIENT_TAG, duration,
                sessionId, player.getCurrentPosition(), uiconfId, mediaConfig.getMediaEntry().getId(), "_" + partnerId, hasSeeked);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                log.d("onComplete send event: " + eventType.toString());
                messageBus.post(new KalturaStatsEvent.KalturaStatsReport(eventType.toString()));
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }
}
