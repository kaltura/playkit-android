package com.kaltura.playkit.plugins.youbora;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.youbora.pluginconfig.YouboraConfig;

/**
 * Created by zivilan on 02/11/2016.
 */

public class YouboraPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("YouboraPlugin");

    private static YouboraLibraryManager pluginManager;
    private static YouboraAdManager adsManager;

    private PKMediaConfig mediaConfig;
    private YouboraConfig pluginConfig;
    private NPAWPlugin npawPlugin;
    private Player player;
    private MessageBus messageBus;

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


        // Refresh options with updated media
        npawPlugin.setOptions(pluginConfig.getYouboraOptions());
        if (!isMonitoring) {
            isMonitoring = true;
            pluginManager = new YouboraLibraryManager(player, messageBus, mediaConfig, pluginConfig);
            npawPlugin.setAdapter(pluginManager);
        }
        if (!isAdsMonitoring){
            adsManager = new YouboraAdManager(player, messageBus);
            npawPlugin.setAdsAdapter(adsManager);
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
        this.pluginConfig = parseConfig(config);
        // Refresh options with updated media
        npawPlugin.setOptions(pluginConfig.getYouboraOptions());
    }

    @Override
    protected void onApplicationPaused() {
        log.d("YOUBORA onApplicationPaused");
        if (adsManager != null) {
            npawPlugin.getAdsAdapter().fireStop();
            adsManager.resetAdValues();
        }
        if (pluginManager != null) {
            npawPlugin.getAdapter().fireStop();
            pluginManager.resetValues();
        }

    }

    @Override
    protected void onApplicationResumed() {
        log.d("YOUBORA onApplicationResumed");
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
        this.messageBus = messageBus;

        this.pluginConfig = parseConfig(config);
        pluginManager = new YouboraLibraryManager(player, messageBus, mediaConfig, pluginConfig);
        npawPlugin = new NPAWPlugin(pluginConfig.getYouboraOptions());
        npawPlugin.setAdapter(pluginManager);
        loadPlugin();
    }

    private void loadPlugin() {
        log.d("loadPlugin");
        messageBus.listen(eventListener, PlayerEvent.Type.DURATION_CHANGE, PlayerEvent.Type.SOURCE_SELECTED);
    }

    PKEvent.Listener eventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {

            PlayerEvent playerEvent = (PlayerEvent) event;
            switch (playerEvent.type) {
                case SOURCE_SELECTED:
                    PlayerEvent.SourceSelected sourceSelected = (PlayerEvent.SourceSelected) playerEvent;
                    pluginConfig.getMedia().setResource(sourceSelected.source.getUrl());
                    npawPlugin.setOptions(pluginConfig.getYouboraOptions());
                    break;
                default:
                    return;
            }
        }
    };

    private void stopMonitoring() {
        log.d("stop monitoring");
        if (adsManager != null && isAdsMonitoring) {
            npawPlugin.removeAdsAdapter();
            isAdsMonitoring = false;
        }
        if (isMonitoring) {
            npawPlugin.removeAdapter();
            isMonitoring = false;
        }
    }

    private static YouboraConfig parseConfig(Object config) {
        if (config instanceof YouboraConfig) {
            return ((YouboraConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), YouboraConfig.class);
        }
        return null;
    }
}