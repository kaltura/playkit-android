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
import com.kaltura.playkit.backend.phoenix.services.BookmarkService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;

import java.util.TimerTask;

/**
 * Created by zivilan on 02/11/2016.
 */

public class PhoenixAnalyticsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("PhoenixAnalyticsPlugin");
    private static final String TAG = "PhoenixAnalytics";

    private enum PhoenixActionType{
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
    private boolean mDidFirstPlay = false;
    private boolean intervalOn = false;
    private long mContinueTime;
    private PlayerConfig.Media mediaConfig;
    private JsonObject pluginConfig;
    private Context mContext;
    private Player player;
    private RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();
    private MessageBus messageBus;

    private int MediaHitInterval = 30000; //Should be provided in plugin config

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "PhoenixAnalytics";
        }

        @Override
        public PKPlugin newInstance() {
            return new PhoenixAnalyticsPlugin();
        }
    };

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {
        mDidFirstPlay = false;
        this.mediaConfig = mediaConfig;
    }

    @Override
    protected void onUpdateConfig(String key, Object value) {}

    @Override
    public void onDestroy() {
        log.d("onDestroy");
        sendAnalyticsEvent(PhoenixActionType.STOP);
        timer.cancel();
    }

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        this.mediaConfig = mediaConfig;
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.pluginConfig = pluginConfig;
        this.mContext = context;
        this.messageBus = messageBus;
        messageBus.listen(mEventListener, (Enum[]) PlayerEvent.Type.values());
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
                    case CAN_PLAY:
                        mDidFirstPlay = false;
                        break;
                    case ENDED:
                        timer.cancel();
                        sendAnalyticsEvent(PhoenixActionType.FINISH);
                        break;
                    case ERROR:
                        timer.cancel();
                        sendAnalyticsEvent(PhoenixActionType.ERROR);
                        break;
                    case LOADED_METADATA:
                        sendAnalyticsEvent(PhoenixActionType.LOAD);
                        break;
                    case PAUSE:
                        if (mDidFirstPlay) {
                            sendAnalyticsEvent(PhoenixActionType.PAUSE);
                        }
                        break;
                    case FIRST_PLAY:
                    case PLAY:
                        if (!intervalOn){
                            startMediaHitInterval();
                            intervalOn = true;
                        }
                        if (!mDidFirstPlay) {
                            mDidFirstPlay = true;
                            sendAnalyticsEvent(PhoenixActionType.FIRST_PLAY);
                        } else {
                            sendAnalyticsEvent(PhoenixActionType.PLAY);
                        }

                        break;
                    default:

                        break;
                }
            }
        }
    };

    private void startMediaHitInterval(){
        log.d("timer interval");
        MediaHitInterval = pluginConfig.has("timerInterval")? pluginConfig.getAsJsonPrimitive("timerInterval").getAsInt(): 30000;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendAnalyticsEvent(PhoenixActionType.HIT);
                if ((float) player.getCurrentPosition() / player.getDuration() > 0.98){
                    sendAnalyticsEvent(PhoenixActionType.FINISH);
                }
            }
        }, 0, MediaHitInterval); // Get media hit interval from plugin config
    }

    private void sendAnalyticsEvent(final PhoenixActionType eventType){
        String fileId = pluginConfig.has("fileId")? pluginConfig.getAsJsonPrimitive("fileId").getAsString():"464302";
        String baseUrl = pluginConfig.has("baseUrl")? pluginConfig.getAsJsonPrimitive("baseUrl").getAsString():"http://52.210.223.65:8080/v4_0/api_v3/";
        String ks = pluginConfig.has("ks")? pluginConfig.getAsJsonPrimitive("ks").getAsString():"djJ8MTk4fN86RC6KBjyHtmG9bIBounF1ewb1SMnFNtAvaxKIAfHUwW0rT4GAYQf8wwUKmmRAh7G0olZ7IyFS1FTpwskuqQPVQwrSiy_J21kLxIUl_V9J";
        int partnerId = pluginConfig.has("partnerId")? pluginConfig.getAsJsonPrimitive("partnerId").getAsInt():198;


        RequestBuilder requestBuilder = BookmarkService.actionAdd(baseUrl, partnerId, ks,
                "media", mediaConfig.getMediaEntry().getId(), eventType.name(), player.getCurrentPosition(), /*mediaConfig.getMediaEntry().getFileId()*/ fileId);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                log.d("onComplete send event: ");
                messageBus.post(new LogEvent(TAG + " " + eventType.name()));
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }
}
