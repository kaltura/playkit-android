package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.google.gson.JsonElement;

/**
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 */
public interface Player {

    void prepare(@NonNull PlayerConfig.Media playerConfig);
    
    void release();

    /**
     * The Player's View.
     * @return
     */
    View getView();

    
    long getDuration();
    
    /**
     * Get playback position in msec.
     * @return
     */
    long getCurrentPosition();
    
    void seekTo(long position);

    /**
     * Play if/when ready.
     */
    void play();

    /**
     * Pause, or don't play when ready.
     */
    void pause();

    /**
     * Prepare for playing the next entry. If config.shouldAutoPlay is true, the entry will automatically
     * play when it's ready and the current entry is ended.
     */
    void prepareNext(@NonNull PlayerConfig.Media mediaConfig);

    /**
     * Load the entry that was prepared with {@link #prepareNext(PlayerConfig.Media)}.
     */
    void skip();
    
    void addEventListener(@NonNull PlayerEvent.Listener listener, PlayerEvent... events);

    void addStateChangeListener(@NonNull PlayerState.Listener listener);
    
    void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable JsonElement value);
}


