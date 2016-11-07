package com.kaltura.playkit.plugins.Youbora;

import android.content.Context;

import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKit;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.npaw.youbora.youboralib.data.Options;

/**
 * Created by zivilan on 02/11/2016.
 */

public class YouboraPlugin extends PKPlugin {
    private static final String TAG = "YouboraPlugin";

    private static YouboraLibraryManager mPluginManager;
    private PlayerConfig mPlayerConfig;
    private Context mContext;
    private Player mPlayer;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "Youbora";
        }

        @Override
        public PKPlugin newInstance(PlayKit playKitManager) {
            return new YouboraPlugin();
        }
    };

    @Override
    protected void update(Player player, PlayerConfig playerConfig, Context context){

    }

    @Override
    protected void load(Player player, PlayerConfig playerConfig, Context context) {
        this.mPlayerConfig = playerConfig;
        this.mPlayer = player;
        this.mContext = context;
        mPluginManager = new YouboraLibraryManager(new Options());
        startMonitoring(mPlayer);
    }

    @Override
    public void release() {
        stopMonitoring();
    }

    public void startMonitoring(Player player) {
        mPluginManager.startMonitoring(player);
        player.addEventListener(mPluginManager.getEventListener());
        player.addStateChangeListener(mPluginManager.getStateChangeListener());
    }

    public void stopMonitoring() {
        mPluginManager.stopMonitoring();
    }
}
