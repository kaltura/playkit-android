package com.kaltura.playkit.plugins;

import android.content.Context;
import android.util.Log;

import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;

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
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JSONObject pluginConfig, final MessageBus messageBus, Context context) {
        this.player = player;
        this.context = context;
        delay = pluginConfig.optInt("delay");
        
        messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Log.d(TAG, "PlayerEvent:" + event);
            }
        });
    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {
        
    }

    @Override
    protected void onUpdateConfig(String key, Object value) {
        
    }

    @Override
    public void onDestroy() {

    }

    @Override
    protected PlayerDecorator getPlayerDecorator() {
        return new PlayerDecorator() {
            @Override
            public void play() {
                super.play();
            }
        };
    }
}
