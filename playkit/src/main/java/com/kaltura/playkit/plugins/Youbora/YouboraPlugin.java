package com.kaltura.playkit.plugins.Youbora;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.npaw.youbora.youboralib.data.Options;

import java.util.Map;

/**
 * Created by zivilan on 02/11/2016.
 */

public class YouboraPlugin extends PKPlugin {
    private static final String TAG = "YouboraPlugin";
    private static final PKLog log = PKLog.get("YouboraPlugin");

    private static YouboraLibraryManager pluginManager;
    private PlayerConfig.Media mediaConfig;
    private JsonObject pluginConfig;
    private Context context;
    private Player player;
    private MessageBus messageBus;

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

    }

    @Override
    protected void onApplicationResumed() {

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
        pluginManager = new YouboraLibraryManager(new Options(), messageBus);
        startMonitoring(this.player);
        log.d("onLoad");
    }

    private void startMonitoring(Player player) {
        log.d("start monitoring");
        Map<String, Object> opt  = YouboraConfig.getYouboraConfig(pluginConfig, mediaConfig);
        // Set options
        pluginManager.setOptions(opt);
        pluginManager.startMonitoring(player);
        messageBus.listen(pluginManager.getEventListener(), (Enum[]) PlayerEvent.Type.values());
        messageBus.listen(pluginManager.getEventListener(), (Enum[]) AdEvent.Type.values());
    }

    private void stopMonitoring() {
        log.d("stop monitoring");
        pluginManager.stopMonitoring();
    }
}
