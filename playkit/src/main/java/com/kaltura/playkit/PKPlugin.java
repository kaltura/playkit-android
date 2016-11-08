package com.kaltura.playkit;

import android.content.Context;

import org.json.JSONObject;

public abstract class PKPlugin {

    public interface Factory {
        String getName();
        PKPlugin newInstance();
    }

    protected abstract void load(Player player, PlayerConfig.Media mediaConfig, JSONObject pluginConfig, MessageBus messageBus, Context context);
    protected abstract void update(PlayerConfig playerConfig);

    protected abstract void release();

    protected PlayerDecorator getPlayerDecorator() {
        return null;
    }

}

