package com.kaltura.playkit.plugins.Youbora;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.npaw.youbora.youboralib.data.Options;

import java.util.Map;

/**
 * Created by zivilan on 02/11/2016.
 */

public class YouboraPlugin extends PKPlugin {
    private static final String TAG = "YouboraPlugin";
    private static final PKLog log = PKLog.get("YouboraPlugin");


    private static YouboraLibraryManager mPluginManager;
    private PlayerConfig.Media mMediaConfig;
    private JsonObject mPluginConfig;
    private Context mContext;
    private Player mPlayer;
    private MessageBus mMessageBus;

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
    protected void onUpdateMedia(PlayerConfig.Media mediaConfig) {

    }

    @Override
    protected void onUpdateConfig(String key, Object value) {

    }

    @Override
    public void onDestroy() {
        stopMonitoring();
    }

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        this.mMediaConfig = mediaConfig;
        this.mPlayer = player;
        this.mPluginConfig = pluginConfig;
        this.mContext = context;
        this.mMessageBus = messageBus;
        mPluginManager = new YouboraLibraryManager(new Options(), messageBus);
        startMonitoring(mPlayer);
    }

    private void startMonitoring(Player player) {

        Map<String, Object> opt  = YouboraConfig.getYouboraConfig(mPluginConfig, mMediaConfig);
        // Set options
        mPluginManager.setOptions(opt);

        mPluginManager.startMonitoring(player);
        mMessageBus.listen(mPluginManager.getEventListener(), (Enum[]) PlayerEvent.Type.values());
    }

    private void stopMonitoring() {
        mPluginManager.stopMonitoring();
    }
}
