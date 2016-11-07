package com.kaltura.playkit.plugins;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKit;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.PlayerEvent;

/**
 * Created by Noam Tamim @ Kaltura on 26/10/2016.
 */

public class SamplePlugin extends PKPlugin {

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

        player.addStateChangeListener(new PlayerState.Listener(){

            @Override
            public void onPlayerStateChanged(Player player, PlayerState newState) {
                Log.d(TAG, "PlayerState:" + newState);
            }
        });
    }

    @Override
    protected void update(Player player, PlayerConfig playerConfig, Context context) {

    }

    @Override
    public void release() {

    }

    @Override
    public PlayerDecorator getPlayerDecorator() {
        return new PlayerDecorator() {
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
        };
    }
}
