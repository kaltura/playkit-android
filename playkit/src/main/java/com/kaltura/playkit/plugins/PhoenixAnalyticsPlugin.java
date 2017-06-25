package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.Gson;
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
import com.kaltura.playkit.api.phoenix.services.BookmarkService;
import com.kaltura.playkit.plugins.configs.PhoenixAnalyticsConfig;
import com.kaltura.playkit.utils.Consts;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by zivilan on 02/11/2016.
 */

public class PhoenixAnalyticsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("PhoenixAnalyticsPlugin");
    private static final double MEDIA_ENDED_THRESHOLD = 0.98;

    private int mediaHitInterval;
    private String fileId;
    private String baseUrl;
    private String ks;
    private int partnerId;
    private Context mContext;
    private Player player;
    private MessageBus messageBus; // used also by TVPAPI Analytics
    private PhoenixAnalyticsConfig pluginConfig;
    private PKMediaConfig mediaConfig;

    private RequestQueue requestsExecutor;
    private java.util.Timer timer;

    private long mContinueTime;
    private long lastKnownPlayerPosition = 0;

    private boolean isFirstPlay = true;
    private boolean intervalOn = false;
    private boolean timerWasCancelled = false;
    private boolean isMediaFinished = false;

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
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        log.d("onLoad");

        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.mContext = context;
        this.messageBus = messageBus;
        this.pluginConfig = parseConfig(config);
        this.timer = new java.util.Timer();
        this.baseUrl = pluginConfig.getBaseUrl();
        this.partnerId = pluginConfig.getPartnerId();
        this.ks = pluginConfig.getKS();
        this.mediaHitInterval = (pluginConfig.getTimerIntervalSec() > 0) ? pluginConfig.getTimerIntervalSec() : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH ;
        if (baseUrl != null && !baseUrl.isEmpty() && partnerId > 0) {
            messageBus.listen(mEventListener, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.ENDED, PlayerEvent.Type.ERROR, PlayerEvent.Type.LOADED_METADATA, PlayerEvent.Type.STOPPED, PlayerEvent.Type.REPLAY, PlayerEvent.Type.SEEKED, PlayerEvent.Type.SOURCE_SELECTED);
        } else {
            log.e("Error, base url/partnet - incorrect");
        }
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        isFirstPlay = true;
        this.mediaConfig = mediaConfig;
        if (this.mediaConfig.getStartPosition() != -1) {
            this.mContinueTime = this.mediaConfig.getStartPosition();
        }
        isMediaFinished = false;
    }

    @Override
    protected void onUpdateConfig(Object config) {
        this.pluginConfig = parseConfig(config);
        this.baseUrl = pluginConfig.getBaseUrl();
        this.partnerId = pluginConfig.getPartnerId();
        this.ks = pluginConfig.getKS();
        this.mediaHitInterval = (pluginConfig.getTimerIntervalSec() > 0) ? pluginConfig.getTimerIntervalSec() : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH;
        if (baseUrl == null || baseUrl.isEmpty()) {
            messageBus.remove(mEventListener,(Enum[]) PlayerEvent.Type.values());
            cancelTimer();
        }
    }

    @Override
    protected void onApplicationPaused() {
        log.d("onApplicationPaused");
        cancelTimer();

    }

    @Override
    protected void onApplicationResumed() {
        timer = new java.util.Timer();
        log.d("onApplicationResumed");

    }

    @Override
    public void onDestroy() {
        log.d("onDestroy");
        cancelTimer();
        timerWasCancelled = true;
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
                        cancelTimer();
                        timer = new java.util.Timer();
                        break;
                    case ENDED:
                        cancelTimer();
                        timer = new java.util.Timer();
                        sendAnalyticsEvent(PhoenixActionType.FINISH);
                        isMediaFinished = true;
                        break;
                    case ERROR:
                        cancelTimer();
                        timer = new java.util.Timer();
                        sendAnalyticsEvent(PhoenixActionType.ERROR);
                        break;
                    case LOADED_METADATA:
                        sendAnalyticsEvent(PhoenixActionType.LOAD);
                        break;
                    case SOURCE_SELECTED:
                        PlayerEvent.SourceSelected sourceSelected = (PlayerEvent.SourceSelected) event;
                        fileId = sourceSelected.source.getId();
                        break;
                    case PAUSE:
                        if (isMediaFinished) {
                            return;
                        }
                        sendAnalyticsEvent(PhoenixActionType.PAUSE);
                        cancelTimer();
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

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

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
        }, 0, mediaHitInterval * 1000); // Get media hit interval from plugin config
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
                "media", mediaConfig.getMediaEntry().getId(), eventType.name(), lastKnownPlayerPosition, fileId);

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

    public String getFileId() {
        return fileId;
    }

    public PKEvent.Listener getEventListener() {
        return mEventListener;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public Player getPlayer() {
        return player;
    }


    public void setPlayer(Player player) {
        this.player = player;
    }

    public MessageBus getMessageBus() {
        return messageBus;
    }

    public void setMessageBus(MessageBus messageBus) {
        this.messageBus = messageBus;
    }


    public RequestQueue getRequestsExecutor() {
        return requestsExecutor;
    }

    public void setRequestsExecutor(RequestQueue requestsExecutor) {
        this.requestsExecutor = requestsExecutor;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public PKMediaConfig getMediaConfig() {
        return mediaConfig;
    }

    public void setMediaConfig(PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;
    }

    public int getMediaHitInterval() {
        return mediaHitInterval;
    }

    public void setMediaHitInterval(int mediaHitInterval) {
        this.mediaHitInterval = mediaHitInterval;
    }

    private static PhoenixAnalyticsConfig parseConfig(Object config) {
        if (config instanceof PhoenixAnalyticsConfig) {
            return ((PhoenixAnalyticsConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), PhoenixAnalyticsConfig.class);
        }
        return null;
    }
}
