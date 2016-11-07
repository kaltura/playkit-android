package com.kaltura.playkit;

import android.content.Context;

public abstract class PKPlugin {

    public interface Factory {
        String getName();
        PKPlugin newInstance(PlayKit playKitManager);
    }

    protected abstract void load(Player player, PlayerConfig playerConfig, Context context);
    protected abstract void update(Player player, PlayerConfig playerConfig, Context context);

    protected abstract void release();

    protected PlayerDecorator getPlayerDecorator() {
        return null;
    }

}

