package com.kaltura.playkit.plugins.Youbora;

import android.content.Context;

import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.Plugin;
import com.npaw.youbora.youboralib.data.Options;

/**
 * Created by zivilan on 02/11/2016.
 */

public class YouboraPlugin extends Plugin {
    private static final String TAG = "YouboraPlugin";

    private static YouboraLibraryManager mPluginManager;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "Youbora";
        }

        @Override
        public Plugin newInstance() {
            return new YouboraPlugin();
        }
    };

    @Override
    protected void load(Player player, PlayerConfig playerConfig, Context context) {
        mPluginManager = new YouboraLibraryManager(new Options());
        player.addEventListener(mPluginManager.getEventListener());
    }

    @Override
    public void release() {
        mPluginManager.stopMonitoring();
    }


}
