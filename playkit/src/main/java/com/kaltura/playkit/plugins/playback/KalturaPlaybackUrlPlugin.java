package com.kaltura.playkit.plugins.playback;

import android.content.Context;

import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;

/**
 * Created by Noam Tamim @ Kaltura on 30/03/2017.
 */

public class KalturaPlaybackUrlPlugin extends PKPlugin {

    public static Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaPlaybackUrlPlugin";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaPlaybackUrlPlugin();
        }

        @Override
        public void warmUp(Context context) {

        }
    };

    @Override
    protected void onLoad(Player player, Object config, MessageBus messageBus, Context context) {
        player.getSettings()
                .setContentRequestDecorator(new KalturaPlaybackRequestDecorator(player));
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {

    }

    @Override
    protected void onUpdateConfig(Object config) {

    }

    @Override
    protected void onApplicationPaused() {

    }

    @Override
    protected void onApplicationResumed() {

    }

    @Override
    protected void onDestroy() {

    }
}
    
