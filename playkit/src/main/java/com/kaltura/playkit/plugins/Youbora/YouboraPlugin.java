package com.kaltura.playkit.plugins.Youbora;

import android.content.Context;

import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.npaw.youbora.youboralib.data.Options;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by zivilan on 02/11/2016.
 */

public class YouboraPlugin extends PKPlugin {
    private static final String TAG = "YouboraPlugin";

    private static YouboraLibraryManager mPluginManager;
    private PlayerConfig.Media mMediaConfig;
    private JSONObject mPluginConfig;
    private Context mContext;
    private Player mPlayer;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "Youbora";
        }

        @Override
        public PKPlugin newInstance() {
            return new YouboraPlugin();
        }
    };

    @Override
    protected void update(PlayerConfig playerConfig){

    }

    @Override
    protected void load(Player player, PlayerConfig.Media mediaConfig, JSONObject pluginConfig, MessageBus messageBus, Context context) {
        this.mMediaConfig = mediaConfig;
        this.mPlayer = player;
        this.mPluginConfig = pluginConfig;
        this.mContext = context;
        mPluginManager = new YouboraLibraryManager(new Options());
        startMonitoring(mPlayer);
    }

    @Override
    public void release() {
        stopMonitoring();
    }

    public void startMonitoring(Player player) {
        Map<String, Object> opt = YouboraConfig.getYouboraConfig(mContext.getApplicationContext(), mPluginConfig);
        Map<String, Object> media = (Map<String, Object>) opt.get("media");

        media.put("resource", mMediaConfig.getMediaEntry().getId());
        media.put("title", mMediaConfig.getMediaEntry().getId()); //name?
        media.put("duration", mMediaConfig.getMediaEntry().getDuration());

        // Set options
        mPluginManager.setOptions(opt);

        mPluginManager.startMonitoring(player);
        player.addEventListener(mPluginManager.getEventListener());
        player.addStateChangeListener(mPluginManager.getStateChangeListener());
    }

    public void stopMonitoring() {
        mPluginManager.stopMonitoring();
    }
}
