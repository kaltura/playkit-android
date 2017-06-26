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


public class PhoenixAnalyticsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("PhoenixAnalyticsPlugin");
    private static final double MEDIA_ENDED_THRESHOLD = 0.98;

    private int mediaHitInterval;
    private String fileId;
    private String baseUrl;
    private String ks;
    private int partnerId;
    private Context context;
    private Player player;
    private MessageBus messageBus; // used also by TVPAPI Analytics
    private PKMediaConfig mediaConfig;

    private RequestQueue requestsExecutor;
    private Timer timer;

    private long continueTime;
    private long lastKnownPlayerPosition = 0;

    private boolean isFirstPlay = true;
    private boolean intervalOn = false;
    private boolean timerWasCancelled = false;
    private boolean isMediaFinished = false;

    enum PhoenixActionType {
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
        this.context = context;
        this.messageBus = messageBus;
        this.timer = new Timer();
        setConfigMembers(config);
        if (baseUrl != null && !baseUrl.isEmpty() && partnerId > 0) {
            messageBus.listen(mEventListener, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.ENDED, PlayerEvent.Type.ERROR, PlayerEvent.Type.LOADED_METADATA, PlayerEvent.Type.STOPPED, PlayerEvent.Type.REPLAY, PlayerEvent.Type.SEEKED, PlayerEvent.Type.SOURCE_SELECTED);
        } else {
            log.e("Error, base url/partner - incorrect");
        }
    }

    private void setConfigMembers(Object config) {
        PhoenixAnalyticsConfig pluginConfig = parseConfig(config);
        this.baseUrl = pluginConfig.getBaseUrl();
        this.partnerId = pluginConfig.getPartnerId();
        this.ks = pluginConfig.getKS();
        this.mediaHitInterval = (pluginConfig.getTimerInterval() > 0) ? pluginConfig.getTimerInterval() * (int) Consts.MILLISECONDS_MULTIPLIER : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH;
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        isFirstPlay = true;
        this.mediaConfig = mediaConfig;
        if (this.mediaConfig.getStartPosition() != -1) {
            this.continueTime = this.mediaConfig.getStartPosition();
        }
        isMediaFinished = false;
    }

    @Override
    protected void onUpdateConfig(Object config) {
        setConfigMembers(config);
        if (baseUrl == null || baseUrl.isEmpty() ||  partnerId >= 0) {
            cancelTimer();
            messageBus.remove(mEventListener,(Enum[]) PlayerEvent.Type.values());
        }
    }

    @Override
    protected void onApplicationPaused() {
        log.d("onApplicationPaused");
        cancelTimer();

    }

    @Override
    protected void onApplicationResumed() {
        timer = new Timer();
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
                        timer = new Timer();
                        break;
                    case ENDED:
                        cancelTimer();
                        timer = new Timer();
                        sendAnalyticsEvent(PhoenixActionType.FINISH);
                        isMediaFinished = true;
                        break;
                    case ERROR:
                        cancelTimer();
                        timer = new Timer();
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
                        timer = new Timer();
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

    void cancelTimer() {
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

    String getFileId() {
        return fileId;
    }

    PKEvent.Listener getEventListener() {
        return mEventListener;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context mContext) {
        this.context = mContext;
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


    RequestQueue getRequestsExecutor() {
        return requestsExecutor;
    }

    void setRequestsExecutor(RequestQueue requestsExecutor) {
        this.requestsExecutor = requestsExecutor;
    }

    Timer getTimer() {
        return timer;
    }

    void setTimer(Timer timer) {
        this.timer = timer;
    }

    PKMediaConfig getMediaConfig() {
        return mediaConfig;
    }

    void setMediaConfig(PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;
    }

    int getMediaHitInterval() {
        return mediaHitInterval;
    }

    void setMediaHitInterval(int mediaHitInterval) {
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
