package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.LogEvent;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.OttEvent;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.backend.phoenix.services.BookmarkService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.utils.Consts;

import java.util.TimerTask;

/**
 * Created by zivilan on 02/11/2016.
 */

public class PhoenixAnalyticsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("PhoenixAnalyticsPlugin");
    private static final String TAG = "PhoenixAnalytics";

    public enum PhoenixActionType{
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
    private boolean isFirstPlay = true;
    private boolean intervalOn = false;
    private long mContinueTime;
    public PKMediaConfig mediaConfig;
    public JsonObject settings;
    private Context mContext;
    public Player player;
    public RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();
    public MessageBus messageBus;

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
        isFirstPlay = false;
        this.mediaConfig = mediaConfig;
    }

    @Override
    protected void onUpdateSettings(Object settings) {
        // TODO: is this the right fix?
        this.settings = (JsonObject) settings;

//        if (pluginConfig.has(key)){
//            pluginConfig.addProperty(key, settings.toString());
//        }
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
        sendAnalyticsEvent(PhoenixActionType.STOP);
        timer.cancel();
    }

    @Override
    protected void onLoad(Player player, Object settings, final MessageBus messageBus, Context context) {
        this.mediaConfig = mediaConfig;
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.settings = (JsonObject) settings;
        this.mContext = context;
        this.messageBus = messageBus;
        messageBus.listen(mEventListener, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.ENDED, PlayerEvent.Type.ERROR, PlayerEvent.Type.LOADED_METADATA);
        if (this.mediaConfig.getStartPosition() != -1){
            this.mContinueTime = this.mediaConfig.getStartPosition();
        }
        log.d("onLoad");
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                log.d(((PlayerEvent) event).type.toString());
                switch (((PlayerEvent) event).type) {
                    case ENDED:
                        timer.cancel();
                        timer = new java.util.Timer();
                        sendAnalyticsEvent(PhoenixActionType.FINISH);
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
                        sendAnalyticsEvent(PhoenixActionType.PAUSE);
                        timer.cancel();
                        timer = new java.util.Timer();
                        intervalOn = false;
                        break;
                    case PLAY:
                        if (isFirstPlay) {
                            isFirstPlay = false;
                            sendAnalyticsEvent(PhoenixActionType.FIRST_PLAY);
                        } else {
                            sendAnalyticsEvent(PhoenixActionType.PLAY);
                        }
                        if (!intervalOn){
                            startMediaHitInterval();
                            intervalOn = true;
                        }
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
    private void startMediaHitInterval(){
        log.d("timer interval");
        int mediaHitInterval = settings.has("timerInterval")? settings.getAsJsonPrimitive("timerInterval").getAsInt() * (int) Consts.MILLISECONDS_MULTIPLIER : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendAnalyticsEvent(PhoenixActionType.HIT);
                if ((float) player.getCurrentPosition() / player.getDuration() > 0.98){
                    sendAnalyticsEvent(PhoenixActionType.FINISH);
                }
            }
        }, 0, mediaHitInterval); // Get media hit interval from plugin config
    }

    /**
     * Send Bookmark/add event using Kaltura Phoenix Rest API
     * @param eventType - Enum stating the event type to send
     */
    protected void sendAnalyticsEvent(final PhoenixActionType eventType){
        String fileId = settings.has("fileId")? settings.getAsJsonPrimitive("fileId").getAsString():"464302";
        String baseUrl = settings.has("baseUrl")? settings.getAsJsonPrimitive("baseUrl").getAsString():"http://api-preprod.ott.kaltura.com/v4_1/api_v3/";
        String ks = settings.has("ks")? settings.getAsJsonPrimitive("ks").getAsString():"djJ8MTk4fN86RC6KBjyHtmG9bIBounF1ewb1SMnFNtAvaxKIAfHUwW0rT4GAYQf8wwUKmmRAh7G0olZ7IyFS1FTpwskuqQPVQwrSiy_J21kLxIUl_V9J";
        int partnerId = settings.has("partnerId")? settings.getAsJsonPrimitive("partnerId").getAsInt():198;


        RequestBuilder requestBuilder = BookmarkService.actionAdd(baseUrl, partnerId, ks,
                "media", mediaConfig.getMediaEntry().getId(), eventType.name(), player.getCurrentPosition(), /*mediaConfig.getMediaEntry().getFileId()*/ fileId);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if (response.isSuccess() && response.getError() != null && response.getError().getCode().equals("4001")){
                    messageBus.post(new OttEvent(OttEvent.OttEventType.Concurrency));
                }
                log.d("onComplete send event: ");
            }
        });
        requestsExecutor.queue(requestBuilder.build());
        messageBus.post(new LogEvent(TAG + " " + eventType.toString(), requestBuilder.build().getBody()));
    }

}
