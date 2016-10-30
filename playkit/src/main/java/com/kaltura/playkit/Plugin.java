package com.kaltura.playkit;

import android.content.Context;

public abstract class Plugin {

    protected abstract void load(Player player, PlayerConfig playerConfig, Context context);

    protected abstract void release();

    public interface Factory {
        String getName();
        Plugin newInstance();
    }
}
