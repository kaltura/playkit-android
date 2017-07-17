/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.youbora;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.utils.Consts;
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
        Map<String, Object> opt  = YouboraConfig.getConfig(pluginConfig, this.mediaConfig, player);
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
        Map<String, Object> opt  = YouboraConfig.getConfig(pluginConfig, mediaConfig, player);
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
            if (pluginConfig.has("enableSmartAds")  &&
                    !pluginConfig.get("enableSmartAds").isJsonNull()) {
                adAnalytics = pluginConfig.getAsJsonPrimitive("enableSmartAds").getAsBoolean();
            }
            messageBus.listen(eventListener, PlayerEvent.Type.DURATION_CHANGE, PlayerEvent.Type.SOURCE_SELECTED);
        }

    }

    PKEvent.Listener eventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {

            PlayerEvent playerEvent = (PlayerEvent) event;
            String key = "";
            Object value = null;

            switch (playerEvent.type) {
                case DURATION_CHANGE:

                    key = "duration";
                    value = Long.valueOf(player.getDuration() / Consts.MILLISECONDS_MULTIPLIER).doubleValue();
                    break;

                case SOURCE_SELECTED:
                    key = "resource";
                    PlayerEvent.SourceSelected sourceSelected = (PlayerEvent.SourceSelected) playerEvent;
                    value = sourceSelected.source.getUrl().toString();
                    break;
            }

            if(key.isEmpty() ) {
                return ;
            }

            Map<String, Object> opt  = YouboraConfig.updateMediaConfig(pluginConfig, key, value);
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
