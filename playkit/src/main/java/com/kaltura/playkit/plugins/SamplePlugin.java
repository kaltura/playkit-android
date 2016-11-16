package com.kaltura.playkit.plugins;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;
import com.kaltura.playkit.PlayerEvent;

import org.json.JSONObject;

/**
 * Created by Noam Tamim @ Kaltura on 26/10/2016.
 */

public class SamplePlugin extends PKPlugin {

    private static final String TAG = "SamplePlugin";

    private Player player;
    private Context context;
    private int delay;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "Sample";
        }

        @Override
        public PKPlugin newInstance() {
            return new SamplePlugin();
        }
    };

    @Override
    protected void load(Player player, PlayerConfig.Media mediaConfig, JSONObject pluginConfig, MessageBus messageBus, Context context) {
        this.player = player;
        this.context = context;
        delay = pluginConfig.optInt("delay");
        player.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {
                Log.d(TAG, "PlayerEvent:" + event);
            }
        });
    }

    @Override
    protected void update(PlayerConfig playerConfig) {
        
    }

    @Override
    public void release() {

    }

    @Override
    public PlayerDecorator getPlayerDecorator() {
        return new PlayerDecorator() {
            @Override
            public void play() {
                player.play();
                Toast.makeText(context, "Delaying playback by " + delay + " ms", Toast.LENGTH_SHORT).show();
                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        player.play();
                    }
                }, delay);
            }
        };
    }
}
