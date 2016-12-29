package com.kaltura.playkit.plugins.Youbora;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
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

    private static YouboraLibraryManager pluginManager;
    private static YouboraAdManager adsManager;

    private PlayerConfig.Media mediaConfig;
    private JsonObject pluginConfig;
    private Context context;
    private Player player;
    private MessageBus messageBus;
    private boolean adAnalytics = false;
    private boolean isMonitoring = false;
    private boolean isAdsMonitoring = false;

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
        stopMonitoring();
        log.d("youbora - onUpdateMedia");
        this.mediaConfig = mediaConfig;
        Map<String, Object> opt  = YouboraConfig.getYouboraConfig(pluginConfig, this.mediaConfig, player);
        // Refresh options with updated media
        pluginManager.setOptions(opt);
        startMonitoring(player);
    }

    @Override
    protected void onUpdateConfig(String key, Object value) {
        log.d("youbora - onUpdateConfig");
        if (pluginConfig.has(key)){
            pluginConfig.addProperty(key, value.toString());
        }
        Map<String, Object> opt  = YouboraConfig.getYouboraConfig(pluginConfig, mediaConfig, player);
        // Refresh options with updated media
        pluginManager.setOptions(opt);
    }

    @Override
    protected void onApplicationPaused() {
        pluginManager.pauseMonitoring();
    }

    @Override
    protected void onApplicationResumed() {
        pluginManager.resumeMonitoring();
    }

    @Override
    public void onDestroy() {
        stopMonitoring();
    }

    @Override
    protected void onLoad(final Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, final MessageBus messageBus, Context context) {
        this.mediaConfig = mediaConfig;
        this.player = player;
        this.pluginConfig = pluginConfig;
        this.context = context;
        this.messageBus = messageBus;
        pluginManager = new YouboraLibraryManager(new Options(), messageBus, mediaConfig, player);
        loadPlugin();
    }

    private void loadPlugin(){
        startMonitoring(this.player);
        if (pluginConfig != null) {
            if (pluginConfig.getAsJsonObject("youboraConfig").has("adsAnalytics")) {
                adAnalytics = pluginConfig.getAsJsonObject("youboraConfig").getAsJsonPrimitive("adsAnalytics").getAsBoolean();
            }
        }
        if (adsManager != null){
            adsManager = new YouboraAdManager(pluginManager, messageBus);
            adsManager.startMonitoring(this.player);
            pluginManager.setAdnalyzer(adsManager);
        }
        log.d("onLoad");
    }

    PKEvent.Listener eventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (player.getDuration() > 0){
                setPluginOptions();
            }
        }
    };

    private void startMonitoring(Player player) {
        log.d("start monitoring");
        if (player.getDuration() < 0){
            messageBus.listen(eventListener, PlayerEvent.Type.DURATION_CHANGE);
        } else if (!pluginManager.getIsLive()){
            setPluginOptions();
        }
    }

    private void setPluginOptions(){
        Map<String, Object> opt  = YouboraConfig.getYouboraConfig(pluginConfig, mediaConfig, player);
        // Set options
        pluginManager.setOptions(opt);
        if (!isMonitoring) {
            pluginManager.startMonitoring(player);
            isMonitoring = true;
        }
        if (adsManager != null && !isAdsMonitoring){
            adsManager.startMonitoring(player);
            isAdsMonitoring = true;
        }
    }

    private void stopMonitoring() {
        log.d("stop monitoring");
        isMonitoring = false;
        pluginManager.stopMonitoring();
        if (adsManager != null){
            adsManager.stopMonitoring();
            isAdsMonitoring = false;
        }
    }
}
