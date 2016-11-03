package com.kaltura.playkit.plugins;

import android.content.Context;

import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.Plugin;

/**
 * Created by zivilan on 02/11/2016.
 */

public class TVPAPIAnalyticsPlugin extends Plugin implements SendBeacon{
    private boolean mIsPlaying = false;
    private boolean mIsConcurrent = false;
    private boolean mDidFirstPlay = false;
    private int mMediaHitInterval = -1;
    private int mFileId = -1;
    private long mContinueTime;
    private PlayerConfig mPlayerConfig;
    private Context mContext;
    private Player mPlayer;
    private boolean mPlayFromContinue = false;


    private static final String TAG = "TVPAPIAnalytics";

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "TVPAPIAnalytics";
        }

        @Override
        public Plugin newInstance() {
            return new TVPAPIAnalyticsPlugin();
        }
    };

    @Override
    protected void load(Player player, PlayerConfig playerConfig, Context context) {
        player.addEventListener(mEventListener);
        this.mPlayerConfig = playerConfig;
        this.mContext = context;
        this.mPlayer = player;
        if (mPlayerConfig.startPosition != -1){
            this.mContinueTime = mPlayerConfig.startPosition;
            this.mPlayFromContinue = true;
        }
    }

    @Override
    public void release() {

    }

    private PlayerEvent.Listener mEventListener = new PlayerEvent.Listener() {
        @Override
        public void onPlayerEvent(Player player, PlayerEvent event) {
            switch (event){
                case CAN_PLAY:
                    mDidFirstPlay = false;
//                    mFileId = mPlayerConfig.getFileId();
                    break;
                case DURATION_CHANGE:

                    break;
                case ENDED:
                    mIsPlaying = false;
                    sendEvent(MEDIA_MARK, 'finish');
                    break;
                case ERROR:

                    break;
                case LOADED_METADATA:
                    sendEvent(MEDIA_MARK, 'load');
                    break;
                case PAUSE:
                    mIsPlaying = false;
                    if (mDidFirstPlay){
                        sendEvent(MEDIA_MARK, event);
                    }
                    break;
                case PLAY:
                    if (!mDidFirstPlay){
                        mDidFirstPlay = true;
                        mIsPlaying = true;
                        sendEvent(MEDIA_MARK, 'first_play');
                    } else {
                        mIsPlaying = true;
                        startMediaHitInterval();
                        sendEvent(MEDIA_MARK, event);
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
    };

    private void bindContinueToTime(){

    }

}
