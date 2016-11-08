package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.view.View;

/**
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 */
public interface Player {

    void prepare(@NonNull PlayerConfig.Media playerConfig);
    
//    /**
//     * Modify player settings with the set fields.
//     * @param playerConfig
//     */
//    void update(@NonNull PlayerConfig playerConfig);
    
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
     * 
     * @return
     */
    boolean getAutoPlay();

    /**
     * Begin playing automatically when ready.
     * @param autoPlay
     */
    void setAutoPlay(boolean autoPlay);

    /**
     * Play if/when ready. Calls {@link #setAutoPlay(boolean)} with true.
     */
    void play();

    /**
     * Pause. Calls {@link #setAutoPlay(boolean)} with false.
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
}


