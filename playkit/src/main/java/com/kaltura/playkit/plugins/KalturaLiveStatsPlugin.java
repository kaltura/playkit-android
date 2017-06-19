package com.kaltura.playkit.plugins;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlaybackInfo;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.api.ovp.services.LiveStatsService;
import com.kaltura.playkit.plugins.configs.KalturaLiveStatsConfig;
import com.kaltura.playkit.plugins.configs.KalturaStatsConfig;
import com.kaltura.playkit.utils.Consts;

import java.util.Date;
import java.util.TimerTask;

import static android.util.Base64.NO_WRAP;
import static com.kaltura.playkit.plugins.KalturaLiveStatsPlugin.KLiveStatsEvent.LIVE;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaLiveStatsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("KalturaLiveStatsPlugin");
    private static final String TAG = "KalturaLiveStatsPlugin";

    private static final int FIFTEEN_MIN = 15 * 60 * 1000;
    private static final int FIFTEEN_SEC = 15 * 1000;

    private Context context;
    private boolean isBuffering = false;
    private long lastReportedBitrate = -1;
    private Player player;
    private PKMediaConfig mediaConfig;
    private KalturaLiveStatsConfig pluginConfig;
    private MessageBus messageBus;
    private RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();
    private int eventIdx = 1;
    private long bufferTime = 0;
    private long bufferStartTime = 0;
    private boolean isLive = false;
    private KLiveStatsEvent liveStatsEvent = LIVE;
    private boolean isLiveStream;
    private boolean isFirstPlay = true;
    private String playbackProtocol ;

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

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaLiveStats";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaLiveStatsPlugin();
        }

        @Override
        public void warmUp(Context context) {

        }
    };

    @Override
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        messageBus.listen(mEventListener, PlayerEvent.Type.STATE_CHANGED, PlayerEvent.Type.PAUSE, PlayerEvent.Type.PLAY, PlayerEvent.Type.PLAYBACK_INFO_UPDATED, PlayerEvent.Type.SOURCE_SELECTED);
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.pluginConfig = parseConfig(config);
        this.messageBus = messageBus;
        this.context = context;
    }

    @Override
    public void onDestroy() {
        stopLiveEvents();
        eventIdx = 1;
        timer.cancel();
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        eventIdx = 1;
        this.mediaConfig = mediaConfig;
    }

    @Override
    protected void onUpdateConfig(Object config) {
        this.pluginConfig = parseConfig(config);
    }

    @Override
    protected void onApplicationPaused() {

    }

    @Override
    protected void onApplicationResumed() {

    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch (((PlayerEvent) event).type) {
                    case STATE_CHANGED:
                        KalturaLiveStatsPlugin.this.onEvent((PlayerEvent.StateChanged) event);
                        break;
                    case PLAY:
                        startLiveEvents();
                        break;
                    case PAUSE:
                        stopLiveEvents();
                        break;
                    case PLAYBACK_INFO_UPDATED:
                        PlaybackInfo currentPlaybackInfo = ((PlayerEvent.PlaybackInfoUpdated) event).getPlaybackInfo();
                        lastReportedBitrate = currentPlaybackInfo.getVideoBitrate();
                        isLiveStream = currentPlaybackInfo.getIsLiveStream();
                        break;
                    case SOURCE_SELECTED:
                        PlayerEvent.SourceSelected sourceSelected = (PlayerEvent.SourceSelected) event;
                        switch (sourceSelected.sourceExtention) {
                            case "m3u8":
                                playbackProtocol = "hls";
                                break;
                            case "mpd":
                                playbackProtocol = "mpegdash";
                                break;
                            case "mp4":
                                playbackProtocol = "http";
                                break;
                            default:
                                playbackProtocol = "NA";
                                break;

                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };

    public void onEvent(PlayerEvent.StateChanged event) {
        switch (event.newState) {
            case READY:
                startTimerInterval();
                if (isBuffering) {
                    isBuffering = false;
                    sendLiveEvent(calculateBuffer(false));
                }
                break;
            case BUFFERING:
                isBuffering = true;
                bufferStartTime = new Date().getTime();
                break;
            default:
                break;
        }
    }

    private long calculateBuffer(boolean isBuffering) {
        long currTime = new Date().getTime();
        bufferTime = (currTime - bufferStartTime) / 1000;
        if (bufferTime > 10) {
            bufferTime = 10;
        }
        if (isBuffering) {
            bufferStartTime = new Date().getTime();
        } else {
            bufferStartTime = -1;
        }
        return bufferTime;
    }

    private void startTimerInterval() {
        if (timer == null) {
            timer = new java.util.Timer();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendLiveEvent(bufferTime);
            }
        }, Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_LOW, Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_LOW);
    }


    private void startLiveEvents() {
        if (!isLive) {
            startTimerInterval();
            isLive = true;
            if (isFirstPlay) {
                sendLiveEvent(bufferTime);
                isFirstPlay = false;
            }

        }
    }

    private void stopLiveEvents() {
        isLive = false;
    }

    private void sendLiveEvent(final long bufferTime) {
        String sessionId = (player.getSessionId() != null) ? player.getSessionId() : "";

        // Parameters for the request -
        // String baseUrl, int partnerId, int eventType, int eventIndex, int bufferTime, int bitrate,
        // String startTime,  String entryId,  boolean isLive, String referrer
        long distanceFromLive = 0;
        if (player != null) {
            distanceFromLive = player.getDuration() - player.getCurrentPosition();
        }

        RequestBuilder requestBuilder = LiveStatsService.sendLiveStatsEvent(pluginConfig.getBaseUrl(),
                pluginConfig.getPartnerId(),
                (distanceFromLive <= FIFTEEN_SEC) ? KLiveStatsEvent.LIVE.value : KLiveStatsEvent.DVR.value,
                eventIdx++, bufferTime,
                lastReportedBitrate,
                sessionId, mediaConfig.getStartPosition(),
                pluginConfig.getEntryId(),
                isLive,
                PlayKitManager.CLIENT_TAG,
                (playbackProtocol != null) ? playbackProtocol : "NA",
                pluginConfig.getContextId(),
                Base64.encodeToString(context.getPackageName().getBytes(), NO_WRAP),
                pluginConfig.getUserId());

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                Log.d(TAG, "onComplete: " + isLive);
                messageBus.post(new KalturaLiveStatsEvent.KalturaLiveStatsReport(bufferTime));
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

    private static KalturaLiveStatsConfig parseConfig(Object config) {
        if (config instanceof KalturaStatsConfig) {
            return ((KalturaLiveStatsConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), KalturaLiveStatsConfig.class);
        }
        return null;
    }
}
