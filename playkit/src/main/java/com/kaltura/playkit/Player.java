package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.view.View;

/**
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 */
public interface Player {

    /**
     * Reset player and load a new config.
     * @param playerConfig
     */
    void load(@NonNull PlayerConfig playerConfig);

    /**
     * Modify player state with the set fields.
     * @param playerConfig
     */
    void apply(@NonNull PlayerConfig playerConfig);

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
    void prepareNext(@NonNull PlayerConfig playerConfig);

    /**
     * Load the entry that was prepared with {@link #prepareNext(PlayerConfig)}. If the prepared
     * config has {@link PlayerConfig#shouldAutoPlay}=true, this will trigger playback.
     */
    void loadNext();
    
    void addEventListener(@NonNull PlayerEvent.Listener listener, PlayerEvent... events);

    void addStateChangeListener(@NonNull PlayerState.Listener listener);

    class RelativeTime {
        
        enum Origin {
            START, END
        }

        Origin origin;
        long offset;
    }
    
    interface TimeListener {
        void onTimeReached(Player player, RelativeTime.Origin origin, long offset);
    }

    void addBoundaryTimeListener(@NonNull RelativeTime[] times, boolean wait, @NonNull TimeListener listener);
    void addTimeProgressListener(long interval, @NonNull TimeListener listener);
}


