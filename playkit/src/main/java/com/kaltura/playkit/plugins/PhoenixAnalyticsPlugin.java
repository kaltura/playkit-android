package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.OttEvent;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.api.phoenix.services.BookmarkService;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.utils.errors.PKError;

import java.util.TimerTask;


/**
 * Created by zivilan on 02/11/2016.
 */

public class PhoenixAnalyticsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("PhoenixAnalyticsPlugin");
    private static final String TAG = "PhoenixAnalytics";
    private static final double MEDIA_ENDED_THRESHOLD = 0.98;
    private static String DEFAULT_BASE_URL = "http://api-preprod.ott.kaltura.com/v4_1/api_v3/";

    private int mediaHitInterval;
    private String fileId;
    private String baseUrl;
    private String ks;
    private int partnerId;

    public enum PhoenixActionType {
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

    private Context mContext;
    public Player player;
    public MessageBus messageBus; // used also by TVPAI Analytics
    public JsonObject pluginConfig;
    public PKMediaConfig mediaConfig;

    public RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();

    private long mContinueTime;
    private long lastKnownPlayerPosition = 0;

    private boolean isFirstPlay = true;
    private boolean intervalOn = false;
    private boolean timerWasCancelled = false;
    private boolean isMediaFinished = false;


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
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        if (Utils.isJsonObjectValueValid(pluginConfig, "timerInterval")) {
            mediaHitInterval = pluginConfig.getAsJsonPrimitive("timerInterval").getAsInt() * (int) Consts.MILLISECONDS_MULTIPLIER;
        } else {
            mediaHitInterval = Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH;
        }
        if (Utils.isJsonObjectValueValid(pluginConfig, "fileId")) {
            fileId = pluginConfig.getAsJsonPrimitive("fileId").getAsString();
        } else {
            fileId = "0";
            String errorMessage = TAG + " fileId was not set";
            sendError(PKError.PKErrorType.INVALID_INIT_OBJECT, errorMessage, new IllegalArgumentException(errorMessage));
        }
        if (Utils.isJsonObjectValueValid(pluginConfig, "baseUrl")) {
            baseUrl = pluginConfig.getAsJsonPrimitive("baseUrl").getAsString();
        } else {
            baseUrl = DEFAULT_BASE_URL;
        }
        if (Utils.isJsonObjectValueValid(pluginConfig, "ks")) {
            ks =  pluginConfig.getAsJsonPrimitive("ks").getAsString();
        } else {
            log.w("Warning PhoenixAnalytics KS was not given");
            ks = "";
        }
        if (Utils.isJsonObjectValueValid(pluginConfig, "partnerId")) {
            partnerId = pluginConfig.getAsJsonPrimitive("partnerId").getAsInt();
        } else {
            partnerId = 0;
        }

        isFirstPlay = true;
        this.mediaConfig = mediaConfig;
        if (this.mediaConfig.getStartPosition() != -1) {
            this.mContinueTime = this.mediaConfig.getStartPosition();
        }
        isMediaFinished = false;
    }

    @Override
    protected void onUpdateConfig(Object config) {
        this.pluginConfig = (JsonObject) config;
    }

    @Override
    protected void onApplicationPaused() {
        log.d("onApplicationPaused");

    }

    @Override
    protected void onApplicationResumed() {
        timer = new java.util.Timer();
        log.d("onApplicationResumed");

    }

    @Override
    public void onDestroy() {
        log.d("onDestroy");
        timer.cancel();
        timerWasCancelled = true;
    }

    @Override
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.pluginConfig = (JsonObject) config;
        this.mContext = context;
        this.messageBus = messageBus;
        messageBus.listen(mEventListener, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.ENDED, PlayerEvent.Type.ERROR, PlayerEvent.Type.LOADED_METADATA, PlayerEvent.Type.STOPPED, PlayerEvent.Type.REPLAY, PlayerEvent.Type.SEEKED);

        log.d("onLoad");
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
                        timer.cancel();
                        timer = new java.util.Timer();
                        break;
                    case ENDED:
                        timer.cancel();
                        timer = new java.util.Timer();
                        sendAnalyticsEvent(PhoenixActionType.FINISH);
                        isMediaFinished = true;
                        break;
                    case ERROR:
                        timer.cancel();
                        timer = new java.util.Timer();
                        sendAnalyticsEvent(PhoenixActionType.ERROR);
                        break;
                    case LOADED_METADATA:
                        sendAnalyticsEvent(PhoenixActionType.LOAD);
                        break;
                    case PAUSE:
                        if (isMediaFinished) {
                            return;
                        }
                        sendAnalyticsEvent(PhoenixActionType.PAUSE);
                        timer.cancel();
                        timer = new java.util.Timer();
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
        String action = eventType.name().toLowerCase(); // used only for copmare

        if (!"stop".equals(action)) {
            lastKnownPlayerPosition = player.getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER;
        }
        RequestBuilder requestBuilder = BookmarkService.actionAdd(baseUrl, partnerId, ks,
                "media", mediaConfig.getMediaEntry().getId(), eventType.name(), lastKnownPlayerPosition, /*mediaConfig.getMediaEntry().getFileId()*/ fileId);

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

    private void sendError(PKError.PKErrorType errorType, String errorMessage, Throwable cause) {
        log.e(errorMessage);
        PKError error = new PKError(errorType, errorMessage, cause);
        messageBus.post(new PlayerEvent.ExceptionInfo(error));
    }

}
