package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.utils.Consts;

/**
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 */
public interface Player {


    /**
     * Prepare the player for playback.
     * @param playerConfig - media configurations to apply on the player.
     */
    void prepare(@NonNull PlayerConfig.Media playerConfig);

    /**
     * Prepare for playing the next entry. If config.shouldAutoPlay is true, the entry will automatically
     * play when it's ready and the current entry is ended.
     */
    void prepareNext(@NonNull PlayerConfig.Media mediaConfig);

    void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable Object value);

    /**
     * Load the entry that was prepared with {@link #prepareNext(PlayerConfig.Media)}.
     */
    void skip();

    /**
     * Player lifecycle method. Should be used when the application went to onPause();
     */
    void onApplicationPaused();

    /**
     * Player lifecycle method. Should be used when the application went to onResume();
     */
    void onApplicationResumed();

    /**
     * Should be called when you want to destroy the player.
     */
    void destroy();
    /**
     * Start playback of the media.
     */
    void play();

    /**
     * Pause playback of the media.
     */
    void pause();

    /**
     * Replay the media.
     */
    void replay();

    /**
     * The Player's View.
     * @return - the view player attached to.
     */
    View getView();

    /**
     * Getter for the current playback position.
     * @return - position of the player or {@link Consts#POSITION_UNSET} if position is unknown or player engine is null.
     */
    long getCurrentPosition();

    /**
     * @return - The total duration of current media
     * or {@link Consts#TIME_UNSET} if the duration is unknown or player engine is null.
     */
    long getDuration();

    /**
     * @return - The buffered position of the current media,
     * or {@link Consts#POSITION_UNSET} if the position is unknown or player engine is null.
     */
    long getBufferedPosition();

    /**
     * Change the volume of the current audio track.
     * Accept values between 0 and 1. Where 0 is mute and 1 is maximum volume.
     * If the volume parameter is higher then 1, it will be converted to 1.
     * If the volume parameter is lower then 0, it be converted to 0.
     * @param volume - volume to set.
     */
    void setVolume(float volume);

    /**
     * @return - true if player is playing, otherwise return false;
     */
    boolean isPlaying();

    /**
     * Add event listener to the player.
     * @param listener - event listener.
     * @param events - events the subscriber interested in.
     */
    void addEventListener(@NonNull PKEvent.Listener listener, Enum... events);

    /**
     * Add state changed listener to the player.
     * @param listener - state changed listener
     */
    void addStateChangeListener(@NonNull PKEvent.Listener listener);

    /**
     * Change current track, with specified one.
     * If uniqueId is not valid or null, this will throw {@link IllegalArgumentException}.
     * Example of the valid uniqueId for regular video track: Video:0,0,1.
     * Example of the valid uniqueId for adaptive video track: Video:0,0,adaptive.
     * @param uniqueId - the unique id of the new track that will play instead of the old one.
     */
    void changeTrack(String uniqueId);

    /**
     * Seek player to the specified position.
     * @param position - desired position.
     */
    void seekTo(long position);

    AdController getAdController();
}

