package com.kaltura.playkit.plugins.Youbora;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.npaw.youbora.youboralib.data.Options;

import java.util.Map;

/**
 * Created by zivilan on 02/11/2016.
 */

public class YouboraPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("YouboraPlugin");

    private static YouboraLibraryManager pluginManager;
    private static YouboraAdManager adsManager;

    private PKMediaConfig mediaConfig;
    private JsonObject pluginConfig;
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

        @Override
        public void warmUp(Context context) {

        }
    };


    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        stopMonitoring();
        log.d("youbora - onUpdateMedia");
        this.mediaConfig = mediaConfig;
        Map<String, Object> opt  = YouboraConfig.getConfig(pluginConfig, this.mediaConfig);
        // Refresh options with updated media
        pluginManager.setOptions(opt);
        if (!isMonitoring) {
            isMonitoring = true;
            pluginManager.startMonitoring(player);
        }
        if (adAnalytics && !isAdsMonitoring){
            adsManager = new YouboraAdManager(pluginManager, messageBus);
            adsManager.startMonitoring(this.player);
            pluginManager.setAdnalyzer(adsManager);
            isAdsMonitoring = true;
        }
    }

    @Override
    protected void onUpdateConfig(Object config) {
        log.d("youbora - onUpdateConfig");
        pluginManager.onUpdateConfig();
        if (adsManager != null) {
            adsManager.onUpdateConfig();
        }
        this.pluginConfig = (JsonObject) config;
        Map<String, Object> opt  = YouboraConfig.getConfig(pluginConfig, mediaConfig);
        // Refresh options with updated media
        pluginManager.setOptions(opt);
    }

    @Override
    protected void onApplicationPaused() {
        log.d("YOUBORA onApplicationPaused");
        if (adsManager != null) {
            adsManager.endedAdHandler();
            adsManager.resetAdValues();
        }
        if (pluginManager != null) {
            pluginManager.endedHandler();
            pluginManager.resetValues();
        }

    }

    @Override
    protected void onApplicationResumed() {
        if (pluginManager != null) {
            pluginManager.playHandler();
        }
    }

    @Override
    public void onDestroy() {
        if (isMonitoring) {
            stopMonitoring();
        }
    }

    @Override
    protected void onLoad(final Player player, Object config, final MessageBus messageBus, Context context) {
        log.d("onLoad");
        this.player = player;
        this.pluginConfig = (JsonObject) config;
        this.messageBus = messageBus;
        pluginManager = new YouboraLibraryManager(new Options(), messageBus, mediaConfig, player);
        loadPlugin();
    }

    private void loadPlugin(){
        log.d("loadPlugin");
        if (pluginConfig != null) {
            if (!pluginConfig.has("youboraConfig") || pluginConfig.get("youboraConfig").isJsonNull() ) {
                log.e("Youbora PluginConfig is missing the youboraConfig key in json object");
                return;
            }
            if (pluginConfig.getAsJsonObject("youboraConfig").has("enableSmartAds")  &&
                    !pluginConfig.getAsJsonObject("youboraConfig").getAsJsonPrimitive("enableSmartAds").isJsonNull()) {
                adAnalytics = pluginConfig.getAsJsonObject("youboraConfig").getAsJsonPrimitive("enableSmartAds").getAsBoolean();
            }
            messageBus.listen(eventListener, PlayerEvent.Type.DURATION_CHANGE, PlayerEvent.Type.SOURCE_SELECTED);
        }

    }

    PKEvent.Listener eventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {

            PlayerEvent playerEvent = (PlayerEvent) event;
            String key = "";

            switch (playerEvent.type) {
                case DURATION_CHANGE:

                    key = "duration";
                    break;

                case SOURCE_SELECTED:
                    key = "resource";
                    break;
            }

            if(key.isEmpty()) {
                return ;
            }

            Map<String, Object> opt  = YouboraConfig.updateMediaConfig(pluginConfig, key, Long.valueOf(player.getDuration() / 1000).doubleValue());
            pluginManager.setOptions(opt);
        }
    };

    private void stopMonitoring() {
        log.d("stop monitoring");
        if (adsManager != null && isAdsMonitoring) {
            adsManager.stopMonitoring();
            isAdsMonitoring = false;
        }
        if (isMonitoring) {
            pluginManager.stopMonitoring();
            isMonitoring = false;
        }

    }
}
