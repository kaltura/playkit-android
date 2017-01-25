package com.kaltura.playkit.plugins;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerDecorator;

/**
 * @hide
 */

public class SamplePlugin extends PKPlugin {

    private static final String TAG = "SamplePlugin";
    private static final PKLog log = PKLog.get("SamplePlugin");

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
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        log.i("Loading");
        this.player = player;
        this.context = context;
        delay = pluginConfig.getAsJsonPrimitive("delay").getAsInt();
        log.v("delay=" + delay);
        
        messageBus.listen(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("onEvent: " + event);
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
    protected void onApplicationPaused() {

    }

    @Override
    protected void onApplicationResumed() {

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
