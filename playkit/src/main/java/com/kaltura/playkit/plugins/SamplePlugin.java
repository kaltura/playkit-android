package com.kaltura.playkit.plugins;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;

/**
 * Created by Noam Tamim @ Kaltura on 26/10/2016.
 */

public class SamplePlugin extends PKPlugin {

    private static final String TAG = "SamplePlugin";

    private Player player;
    private Context context;
    private long delay;

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
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, MessageBus messageBus, Context context) {
        this.player = player;
        this.context = context;
        this.delay = pluginConfig.get("delay").getAsLong();
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                Log.d(TAG, "PKEvent:" + event);
            }
        });
    }

    @Override
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {
        
    }

    @Override
    protected void onUpdateConfig(String key, JsonElement value) {

    }

    @Override
    protected void onDestroy() {

    }
}
