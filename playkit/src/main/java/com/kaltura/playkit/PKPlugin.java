package com.kaltura.playkit;

import android.content.Context;

import com.google.gson.JsonObject;

public abstract class PKPlugin {

    public interface Factory {
        String getName();
        PKPlugin newInstance();
    }

    protected abstract void onLoad(Player player, PlayerConfig.Media mediaConfig, JsonObject pluginConfig, MessageBus messageBus, Context context);
    protected abstract void onUpdateMedia(PlayerConfig.Media mediaConfig);
    protected abstract void onUpdateConfig(String key, Object value);

    protected abstract void onDestroy();

    protected PlayerDecorator getPlayerDecorator() {
        return null;
    }
}
