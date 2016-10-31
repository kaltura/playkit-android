package com.kaltura.playkit.plugins;

import android.content.Context;
import android.util.Log;

import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.Plugin;

/**
 * Created by Noam Tamim @ Kaltura on 26/10/2016.
 */

public class SamplePlugin extends Plugin {

    private static final String TAG = "SamplePlugin";

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "Sample";
        }

        @Override
        public Plugin newInstance() {
            return new SamplePlugin();
        }
    };
    
    @Override
    protected void load(Player player, PlayerConfig playerConfig, Context context) {
        player.addEventListener(new PlayerEvent.Listener() {
            @Override
            public void onPlayerEvent(Player player, PlayerEvent event) {
                Log.d(TAG, "PlayerEvent:" + event);
            }
        });

        player.addBoundaryTimeListener(new Player.TimeListener() {
            @Override
            public void onTimeReached(Player player, Player.RelativeTime.Origin origin, long offset) {

            }
        }, true, Player.RelativeTime.START);
    }

    @Override
    public void release() {

    }
}
