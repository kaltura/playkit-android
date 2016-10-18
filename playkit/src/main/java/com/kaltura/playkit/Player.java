package com.kaltura.playkit;

import android.view.View;

/**
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 */
public interface Player {
    
    enum State {
        IDLE, LOADING, READY, BUFFERING
    }

    /**
     * Reset player and load a new config.
     * @param playerConfig
     */
    void load(PlayerConfig playerConfig);

    /**
     * Modify player state with the set fields.
     * @param playerConfig
     */
    void apply(PlayerConfig playerConfig);

    /**
     * The Player's View.
     * @return
     */
    View getView();

    /**
     * Get playback position in msec.
     * @return
     */
    long getPosition();

    /**
     * Play if/when ready.
     */
    void play();

    /**
     * Prepare for playing the next entry. If config.shouldAutoPlay is true, the entry will automatically
     * play when it's ready and the current entry is ended.
     * @param playerConfig
     */
    void prepareNext(PlayerConfig playerConfig);

    /**
     * Load the entry that was prepared with {@link #prepareNext(PlayerConfig)}. If the prepared
     * config has {@link PlayerConfig#shouldAutoPlay}=true, this will trigger playback.
     */
    void loadNext();
    
    
}


