package com.kaltura.playkit;

import android.content.Context;

public abstract class PKPlugin {

    public interface Factory {
        String getName();
        PKPlugin newInstance();
        void warmUp(Context context);
    }

    protected abstract void onLoad(Player player, Object settings, MessageBus messageBus, Context context);
    protected abstract void onUpdateMedia(PlayerConfig.Media mediaConfig);
    protected abstract void onUpdateSettings(Object settings);
    protected abstract void onApplicationPaused();
    protected abstract void onApplicationResumed();

    protected abstract void onDestroy();

    protected PlayerDecorator getPlayerDecorator() {
        return null;
    }
}
