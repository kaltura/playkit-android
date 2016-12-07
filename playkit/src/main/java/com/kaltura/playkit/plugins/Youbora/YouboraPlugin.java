package com.kaltura.playkit.plugins.Youbora;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.npaw.youbora.youboralib.data.Options;

import java.util.Map;

/**
 * Created by zivilan on 02/11/2016.
 */

public class YouboraPlugin extends PKPlugin {
    private static final String TAG = "YouboraPlugin";
    private static final PKLog log = PKLog.get("YouboraPlugin");

    private static YouboraLibraryManager pluginManager;
    private static YouboraAdManager adsManager;

    private PlayerConfig.Media mediaConfig;
    private JsonObject pluginConfig;
    private Context context;
    private Player player;
    private MessageBus messageBus;
    private boolean adAnalytics = false;

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
        this.mediaConfig = mediaConfig;
        Map<String, Object> opt  = YouboraConfig.getYouboraConfig(pluginConfig, this.mediaConfig);
        // Refresh options with updated media
        pluginManager.setOptions(opt);
    }

    @Override
    protected void onUpdateConfig(String key, Object value) {
        if (pluginConfig.has(key)){
            pluginConfig.addProperty(key, value.toString());
        }
        Map<String, Object> opt  = YouboraConfig.getYouboraConfig(pluginConfig, mediaConfig);
        // Refresh options with updated media
        pluginManager.setOptions(opt);
    }

    @Override
    protected void onApplicationPaused() {
        stopMonitoring();
    }

    @Override
    protected void onApplicationResumed() {
        startMonitoring(this.player);
    }

    @Override
    public void onDestroy() {
        stopMonitoring();
    }

    @Override
    protected void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        this.mediaConfig = mediaConfig;
        this.player = player;
        this.pluginConfig = pluginConfig;
        this.context = context;
        this.messageBus = messageBus;
        pluginManager = new YouboraLibraryManager(new Options(), messageBus, mediaConfig, player);
        startMonitoring(this.player);
        if (pluginConfig != null) {
            if (pluginConfig.has("adsAnalytics")) {
                adAnalytics = pluginConfig.getAsJsonPrimitive("adsAnalytics").getAsBoolean();
            }
        }
        if (adsManager != null){
            adsManager = new YouboraAdManager(pluginManager, messageBus);
            adsManager.startMonitoring(this.player);
            pluginManager.setAdnalyzer(adsManager);
        }
        log.d("onLoad");
    }

    private void startMonitoring(Player player) {
        log.d("start monitoring");
        Map<String, Object> opt  = YouboraConfig.getYouboraConfig(pluginConfig, mediaConfig);
        // Set options
        pluginManager.setOptions(opt);
        pluginManager.startMonitoring(player);
        if (adsManager != null){
            adsManager.startMonitoring(player);
        }
    }

    private void stopMonitoring() {
        log.d("stop monitoring");
        pluginManager.stopMonitoring();
        if (adsManager != null){
            adsManager.stopMonitoring();
        }
    }
}
