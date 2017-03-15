package com.kaltura.playkit;

import android.content.Context;

public abstract class PKPlugin {

    public interface Factory {
        String getName();
        PKPlugin newInstance();
        void warmUp(Context context);
    }

    protected abstract void onLoad(Player player, Object config, MessageBus messageBus, Context context);
    protected abstract void onUpdateMedia(PKMediaConfig mediaConfig);
    protected abstract void onUpdateConfig(Object config);
    protected abstract void onApplicationPaused();
    protected abstract void onApplicationResumed();

    protected abstract void onDestroy();

    protected PlayerDecorator getPlayerDecorator() {
        return null;
    }
}
