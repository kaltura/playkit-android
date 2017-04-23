package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.player.PlayerView;
import com.kaltura.playkit.utils.Consts;

import java.util.UUID;

/**
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 */
public interface Player {

    /**
     * Interface used for setting optional Player settings. 
     */
    interface Settings {
        /**
         * Set the Player's contentRequestAdapter.
         * @param contentRequestAdapter 
         * @return Player Settings.
         */
        Settings setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter);

        /**
         * Enable/disable cea-608 text tracks.
         * By default they are disabled.
         * Note! Once set, this value will be applied to all mediaSources for that instance of Player.
         * In order to disable/enable it again, you should update that value once again.
         * Otherwise it will stay in the previous state.
         * @param cea608CaptionsEnabled - should cea-608 track should be enabled.
         * @return Player Settings.
         */
        Settings setCea608CaptionsEnabled(boolean cea608CaptionsEnabled);
    }

    /**
     * Get the Player's {@link Settings} object, for setting some optional properties. 
     * @return Player Settings.
     */
    Settings getSettings();

    /**
     * Prepare the player for playback.
     * @param playerConfig - media configurations to apply on the player.
     */
    void prepare(@NonNull PKMediaConfig playerConfig);

    /**
     * Prepare for playing the next entry. If config.shouldAutoPlay is true, the entry will automatically
     * play when it's ready and the current entry is ended.
     */
    void prepareNext(@NonNull PKMediaConfig mediaConfig);

    void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig);

    /**
     * Load the entry that was prepared with {@link #prepareNext(PKMediaConfig)}.
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
     * stop player and back to initial playback state.
     */
    void stop();

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
    PlayerView getView();

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

    /**
     * Get the Player's SessionId. The SessionId is initialized when the player loads. 
     * @return Player's SessionId, as a UUID object.
     */
    UUID getSessionId();
}

