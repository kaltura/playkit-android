package com.kaltura.playkit;

import android.content.Context;

public abstract class Plugin {

    /**
     * Called by the library to load the plugin, providing it with a player. The plugin should use
     * this method to 
     * @param context
     * @param player
     */
    protected abstract void load(Context context, PlayerConfig playerConfig, Player player);

    protected abstract void release();

    public interface Factory {
        String getName();
        Plugin newInstance();
    }
}
