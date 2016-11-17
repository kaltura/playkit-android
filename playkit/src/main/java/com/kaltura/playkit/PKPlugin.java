package com.kaltura.playkit;

import android.content.Context;

import org.json.JSONObject;

public abstract class PKPlugin {

    public interface Factory {
        String getName();
        PKPlugin newInstance();
    }

    protected abstract void onLoad(Player player, PlayerConfig.Media mediaConfig, JSONObject pluginConfig, MessageBus messageBus, Context context);
    protected abstract void onUpdateMedia(PlayerConfig.Media mediaConfig);
    protected abstract void onUpdateConfig(JSONObject pluginConfig);

    protected abstract void onDestroy();

    protected PlayerDecorator getPlayerDecorator() {
        return null;
    }
}
