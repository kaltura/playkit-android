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
import com.kaltura.playkit.backend.phoenix.services.BookmarkService;
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

public class PhoenixAnalyticsPlugin extends PKPlugin {
    enum PhoenixActionType{
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
    private boolean mIsPlaying = false;
    private boolean mIsConcurrent = false;
    private boolean mDidFirstPlay = false;
    private boolean intervalOn = false;
    private int mMediaHitInterval = -1;
    private int mFileId = -1;
    private long mContinueTime;
    private PlayerConfig.Media mMediaConfig;
    private JsonObject mPluginConfig;
    private Context mContext;
    private Player mPlayer;
    private boolean mPlayFromContinue = false;
    private RequestQueue requestsExecutor;
    private java.util.Timer timer = new java.util.Timer();
    private final static int MediaHitInterval = 30000;

    SessionProvider ksSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return "http://52.210.223.65:8080/v4_0/api_v3/";
        }

        @Override
        public String getKs() {
            return "djJ8MTk4fN86RC6KBjyHtmG9bIBounF1ewb1SMnFNtAvaxKIAfHUwW0rT4GAYQf8wwUKmmRAh7G0olZ7IyFS1FTpwskuqQPVQwrSiy_J21kLxIUl_V9J";
        }

        @Override
        public int partnerId() {
            return 198;
        }
    };

    private static final String TAG = "PhoenixAnalytics";

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

    }

    @Override
    protected void onUpdateConfig(String key, Object value) {

    }

    @Override
    public void onDestroy() {
        setMessageParams(PhoenixActionType.STOP);
    }

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        this.mMediaConfig = mediaConfig;
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.mPlayer = player;
        this.mPluginConfig = pluginConfig;
        this.mContext = context;
        messageBus.listen(mEventListener, (PKEvent[]) PlayerEvent.values());
        if (mMediaConfig.getStartPosition() != -1){
            this.mContinueTime = mMediaConfig.getStartPosition();
            this.mPlayFromContinue = true;
        }
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof PlayerEvent) {
                switch ((PlayerEvent) event) {
                    case CAN_PLAY:
                        mDidFirstPlay = false;
                        break;
                    case DURATION_CHANGE:

                        break;
                    case ENDED:
                        timer.cancel();
                        mIsPlaying = false;
                        setMessageParams(PhoenixActionType.FINISH);
                        break;
                    case ERROR:
                        timer.cancel();
                        setMessageParams(PhoenixActionType.ERROR);
                        break;
                    case LOADED_METADATA:
                        setMessageParams(PhoenixActionType.LOAD);
                        break;
                    case PAUSE:
                        mIsPlaying = false;
                        if (mDidFirstPlay) {
                            setMessageParams(PhoenixActionType.PAUSE);
                        }
                        break;
                    case PLAY:
                        if (!intervalOn){
                            startMediaHitInterval();
                            intervalOn = true;
                        }
                        if (!mDidFirstPlay) {
                            mDidFirstPlay = true;
                            mIsPlaying = true;
                            setMessageParams(PhoenixActionType.FIRST_PLAY);
                        } else {
                            mIsPlaying = true;
                            setMessageParams(PhoenixActionType.PLAY);
                        }

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

    private void startMediaHitInterval(){
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                setMessageParams(PhoenixActionType.HIT);
                if ((float) mPlayer.getCurrentPosition() / mPlayer.getDuration() > 0.98){
                    setMessageParams(PhoenixActionType.FINISH);
                }
            }
        }, 0, MediaHitInterval); // Get media hit interval from plugin config
    }

    private void setMessageParams(PhoenixActionType eventType){
        RequestBuilder requestBuilder = BookmarkService.actionAdd(ksSessionProvider.baseUrl(), ksSessionProvider.partnerId(), ksSessionProvider.getKs(),
                "media", "258656", eventType.name(), mPlayer.getCurrentPosition(), "464302");

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                Log.d(TAG, "onComplete: ");
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

}
