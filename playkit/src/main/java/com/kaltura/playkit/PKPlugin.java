package com.kaltura.playkit;

import android.content.Context;

public abstract class PKPlugin {

    public interface Factory {
        String getName();
        PKPlugin newInstance();
    }

    protected abstract void load(Player player, PlayerConfig playerConfig, MessageBus messageBus, Context context);
    protected abstract void update(PlayerConfig playerConfig);

    protected abstract void release();

    protected PlayerDecorator getPlayerDecorator() {
        return null;
    }

}

