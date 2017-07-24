/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.player.PlayerController;
import com.kaltura.playkit.player.PlayerView;
import com.kaltura.playkit.utils.Consts;

/**
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 */
public interface Player {

    /**
     * Prepare the player for playback.
     *
     * @param mediaConfig - media configurations to apply on the player.
     */
    void prepare(@NonNull PKMediaConfig mediaConfig);

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
     * stop player and back to initial playback state.
     */
    void stop();

    /**
     * Seek player to the specified position.
     *
     * @param position - desired position.
     */
    void seekTo(long position);

    /**
     * Change the volume of the current audio track.
     * Accept values between 0 and 1. Where 0 is mute and 1 is maximum volume.
     * If the volume parameter is higher then 1, it will be converted to 1.
     * If the volume parameter is lower then 0, it be converted to 0.
     *
     * @param volume - volume to set.
     */
    void setVolume(float volume);

    /**
     * Change current track, with specified one.
     * If uniqueId is not valid or null, this will throw {@link IllegalArgumentException}.
     * Example of the valid uniqueId for regular video track: Video:0,0,1.
     * Example of the valid uniqueId for adaptive video track: Video:0,0,adaptive.
     *
     * @param uniqueId - the unique id of the new track that will play instead of the old one.
     */
    void changeTrack(String uniqueId);

    /**
     * @return - true if player is playing, otherwise return false;
     */
    boolean isPlaying();

    /**
     * @return - The total duration of current media
     * or {@link Consts#TIME_UNSET} if the duration is unknown or player engine is null.
     */
    long getDuration();

    /**
     * Getter for the current playback position.
     *
     * @return - position of the player or {@link Consts#POSITION_UNSET} if position is unknown or player engine is null.
     */
    long getCurrentPosition();

    /**
     * @return - The buffered position of the current media,
     * or {@link Consts#POSITION_UNSET} if the position is unknown or player engine is null.
     */
    long getBufferedPosition();

    /**
     * The Player's View.
     *
     * @return - the view player attached to.
     */
    PlayerView getView();

    /**
     * Should be called when you want to destroy the player.
     */
    void destroy();

    /**
     * Update plugin configurations.
     *
     * @param pluginName   - name of the plugin to update.
     * @param pluginConfig - new configurations to use.
     */
    void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig);

    /**
     * Add event listener to the player.
     *
     * @param listener - event listener.
     * @param events   - events the subscriber interested in.
     */
    void addEventListener(@NonNull PKEvent.Listener listener, Enum... events);

    /**
     * Add state changed listener to the player.
     *
     * @param listener - state changed listener
     */
    void addStateChangeListener(@NonNull PKEvent.Listener listener);

    /**
     * Player lifecycle method. Should be used when the application went to onPause();
     */
    void onApplicationPaused();

    /**
     * Player lifecycle method. Should be used when the application went to onResume();
     */
    void onApplicationResumed();

    /**
     * Get the Player's SessionId. The SessionId is generated each time new media is set.
     *
     * @return Player's SessionId, as a String object.
     */
    String getSessionId();

    /**
     * Generic getters for playkit controllers.
     *
     * @param type - type of the controller you want to obtain.
     * @return - the {@link PKController} instance if specified controller type exist,
     * otherwise return null.
     */
    PKController getController(@Nullable Class<? extends PKController> type);

    /**
     * Get the Player's {@link Settings} object, for setting some optional properties.
     *
     * @return Player Settings.
     */
    Settings getSettings();

    /**
     * Interface used for setting optional Player settings.
     */
    interface Settings {
        /**
         * Set the Player's contentRequestAdapter.
         *
         * @param contentRequestAdapter - request adapter.
         * @return - Player Settings.
         */
        Settings setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter);

        /**
         * Enable/disable cea-608 text tracks.
         * By default they are disabled.
         * Note! Once set, this value will be applied to all mediaSources for that instance of Player.
         * In order to disable/enable it again, you should update that value once again.
         * Otherwise it will stay in the previous state.
         *
         * @param cea608CaptionsEnabled - should cea-608 track should be enabled.
         * @return - Player Settings.
         */
        Settings setCea608CaptionsEnabled(boolean cea608CaptionsEnabled);

        /**
         * Decide if player should use {@link android.view.TextureView} as primary surface
         * to render the video content. If set to false, will use the {@link android.view.SurfaceView} instead.
         * Note!!! Use this carefully, because {@link android.view.TextureView} is more expensive and not DRM
         * protected. But it allows dynamic animations/scaling e.t.c on the player. By default it will be always set
         * to false.
         *
         * @param useTextureView - true if should use {@link android.view.TextureView}.
         * @return - Player Settings.
         */
        Settings useTextureView(boolean useTextureView);
    }

}


