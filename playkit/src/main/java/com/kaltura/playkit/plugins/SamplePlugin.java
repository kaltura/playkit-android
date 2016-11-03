package com.kaltura.playkit.plugins;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKit;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.DecoratedPlayerProvider;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;

/**
 * Created by Noam Tamim @ Kaltura on 26/10/2016.
 */

public class SamplePlugin extends PKPlugin implements DecoratedPlayerProvider {

    private static final String TAG = "SamplePlugin";

    private Player mPlayer;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "Sample";
        }

        @Override
        public PKPlugin newInstance(PlayKit playKitManager) {
            return new SamplePlugin();
        }
    };
    private Context mContext;

    @Override
    protected void load(Player player, PlayerConfig playerConfig, Context context) {
        mPlayer = player;
        mContext = context;
        player.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {
                Log.d(TAG, "PlayerEvent:" + event);
            }
        });
    }

    @Override
    public void release() {

    }

    @Override
    public Player getDecoratedPlayer() {
        return new Player() {
            @Override
            public void load(@NonNull PlayerConfig playerConfig) {
                mPlayer.load(playerConfig);
            }

            @Override
            public void apply(@NonNull PlayerConfig playerConfig) {
                mPlayer.apply(playerConfig);
            }

            @Override
            public View getView() {
                return mPlayer.getView();
            }

            @Override
            public long getDuration() {
                return mPlayer.getDuration();
            }

            @Override
            public long getCurrentPosition() {
                return mPlayer.getCurrentPosition();
            }

            @Override
            public void seekTo(long position) {
                mPlayer.seekTo(position);
            }

            @Override
            public boolean getAutoPlay() {
                return mPlayer.getAutoPlay();
            }

            @Override
            public void setAutoPlay(boolean autoPlay) {
                mPlayer.setAutoPlay(autoPlay);
            }

            @Override
            public void play() {
                Toast.makeText(mContext, "Delaying playback by 5000 ms", Toast.LENGTH_SHORT).show();
                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPlayer.play();
                    }
                }, 5000);
            }

            @Override
            public void pause() {
                Toast.makeText(mContext, "Will now pause", Toast.LENGTH_SHORT).show();
                mPlayer.pause();
            }

            @Override
            public void prepareNext(@NonNull PlayerConfig playerConfig) {
                mPlayer.prepareNext(playerConfig);
            }

            @Override
            public void loadNext() {
                mPlayer.loadNext();
            }

            @Override
            public void addEventListener(@NonNull PlayerEvent.Listener listener, PlayerEvent... events) {
                mPlayer.addEventListener(listener, events);
            }

            @Override
            public void addStateChangeListener(@NonNull PlayerState.Listener listener) {
                mPlayer.addStateChangeListener(listener);
            }
        };
    }
}
