package com.kaltura.playkit.plugins;

import android.content.Context;

import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.Plugin;

import static android.R.attr.start;

/**
 * Created by zivilan on 02/11/2016.
 */

public class TVPAPIAnalyticsPlugin extends Plugin {
    private boolean isPlaying = false;
    private boolean isConcurrent = false;
    private boolean didFirstPlay = false;
    private int mMediaHitInterval = -1;
    private int mFileId = -1;


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
    }

    @Override
    public void release() {

    }

    private PlayerEvent.Listener mEventListener = new PlayerEvent.Listener() {
        @Override
        public void onPlayerEvent(Player player, PlayerEvent event) {
            switch (event){
                case CAN_PLAY:
                    didFirstPlay = false;

                    break;
                case DURATION_CHANGE:

                    break;
                case ENDED:

                    break;
                case ERROR:

                    break;
                case LOADED_METADATA:

                    break;
                case PAUSE:
                    isPlaying = false;
                    if (didFirstPlay){
                        sendEvent(MEDIA_MARK, event);
                    }
                    break;
                case PLAY:
                    if (!didFirstPlay){
                        didFirstPlay = true;
                        isPlaying = true;
                        sendEvent(MEDIA_MARK, 'first_play');
                    } else {
                        isPlaying = true;
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

}
