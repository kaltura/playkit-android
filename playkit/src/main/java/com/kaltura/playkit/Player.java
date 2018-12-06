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

import com.kaltura.playkit.player.LoadControlBuffers;
import com.kaltura.playkit.player.PlayerView;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.playkit.utils.Consts;

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
         *
         * @param contentRequestAdapter - request adapter.
         * @return - Player Settings.
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter);

        /**
         * Set the Player's licenseRequestAdapter.
         *
         * @param licenseRequestAdapter - request adapter.
         * @return - Player Settings.
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter);

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
        @SuppressWarnings("UnusedReturnValue")
        Settings setCea608CaptionsEnabled(boolean cea608CaptionsEnabled);

        /**
         * Enable/disable MPGA audio tracks.
         * By default they are disabled.
         * Note! Once set, this value will be applied to all mediaSources for that instance of Player.
         * In order to disable/enable it again, you should update that value once again.
         * Otherwise it will stay in the previous state.
         *
         * @param mpgaAudioFormatEnabled - should Enable MPGA Audio track.
         * @return - Player Settings.
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setMpgaAudioFormatEnabled(boolean mpgaAudioFormatEnabled);

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
        @SuppressWarnings("UnusedReturnValue")
        Settings useTextureView(boolean useTextureView);

        /**
         * Decide if player should do cross protocol redirect or not. By default it will be always set
         * to false.
         *
         * @param crossProtocolRedirectEnabled - true if should do cross protocol redirect.
         * @return - Player Settings.
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setAllowCrossProtocolRedirect(boolean crossProtocolRedirectEnabled);

        /**
         * Decide if player should use secure rendering on the surface.
         * Known limitation - when useTextureView set to true and isSurfaceSecured set to true -
         * secure rendering will have no effect.
         *
         * @param isSurfaceSecured - should enable/disable secure rendering
         * @return - Player Settings.
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setSecureSurface(boolean isSurfaceSecured);

        /**
         * Decide the Ad will be auto played when comes to foreground from background
         *
         * @param autoPlayOnResume true if it is autoplayed or else false, default is TRUE
         * @return Player Settings
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setAdAutoPlayOnResume(boolean autoPlayOnResume);

        /**
         * Set the player buffers size
         *
         * @param loadControlBuffers LoadControlBuffers
         * @return Player Settings
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setPlayerBuffers(LoadControlBuffers loadControlBuffers);

        /**
         * Set the Player's VR/360 support
         *
         * @param vrPlayerEnabled - If 360 media should be played on VR player or default player - default == true.
         * @return - Player Settings.
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setVRPlayerEnabled(boolean vrPlayerEnabled);

        /**
         * Set the Player's preferredAudioTrackConfig.
         *
         * @param preferredAudioTrackConfig - AudioTrackConfig.
         * @return - Player Settings.
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig);

        /**
         * Set the Player's preferredTextTrackConfig.
         *
         * @param preferredTextTrackConfig - TextTrackConfig.
         * @return - Player Settings.
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig);

        /**
         * Set the Player's setPreferredMediaFormat.
         *
         * @param preferredMediaFormat - PKMediaFormat.
         * @return - Player Settings.
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setPreferredMediaFormat(PKMediaFormat preferredMediaFormat);

        /**
         * Set the Player's Subtitles
         *
         * @param subtitleStyleSettings - SubtitleStyleSettings
         * @return - Player Settings
         */
        @SuppressWarnings("UnusedReturnValue")
        Settings setSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings);
    }

    /**
     * Get the Player's {@link Settings} object, for setting some optional properties.
     *
     * @return Player Settings.
     */
    Settings getSettings();

    /**
     * Prepare the player for playback.
     *
     * @param playerConfig - media configurations to apply on the player.
     */
    void prepare(@NonNull PKMediaConfig playerConfig);

    void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig);

    /**
     * Player lifecycle method. Should be used when the application went to onPause();
     */
    void onApplicationPaused();

    /**
     * Player lifecycle method. Should be used when the application went to onResume();
     */
    void onApplicationResumed();

    /**
     * Player lifecycle method. Must be called when Activity onConfigurationChanged(Configuration newConfig) called.
     */
    void onOrientationChanged();

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
     *
     * @return - the view player attached to.
     */
    PlayerView getView();

    /**
     * Getter for the current playback position.
     *
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
     *
     * @param volume - volume to set.
     */
    void setVolume(float volume);

    /**
     * @return - true if player is playing, otherwise return false;
     */
    boolean isPlaying();

    /**
     * Change current track, with specified one by uniqueId.
     * If uniqueId is not valid or null, this will throw {@link IllegalArgumentException}.
     * Example of the valid uniqueId for regular video track: Video:0,0,1.
     * Example of the valid uniqueId for adaptive video track: Video:0,0,adaptive.
     *
     * @param uniqueId - the unique id of the new track that will play instead of the old one.
     */
    void changeTrack(String uniqueId);

    /**
     * Seek player to the specified position.
     *
     * @param position - desired position.
     */
    void seekTo(long position);

    /**
     * Get the Player's SessionId. The SessionId is generated each time new media is set.
     *
     * @return Player's SessionId, as a String object.
     */
    String getSessionId();

    /**
     * Checks if the stream is live or not
     *
     * @return flag for live
     */
    boolean isLive();

    /**
     * @return - Getter for the current mediaFormat
     * or {@link null} if the media format is not set yet
     */
    PKMediaFormat getMediaFormat();

    /**
     * Change player speed (pitch = 1.0f by default)
     *
     * @param rate - desired rate (ex. 0.5f 1.0f 1.5f, 2.0f).
     */
    void setPlaybackRate(float rate);

    /**
     * get current player speed
     */
    float getPlaybackRate();

    /**
     * Generic getters for playkit controllers.
     *
     * @param type - type of the controller you want to obtain.
     * @return - the {@link PKController} instance if specified controller type exist,
     * otherwise return null.
     */
    <T extends PKController> T getController(Class<T> type);

    /**
     * Update Subtitle Styles
     */
    void updateSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings);


    /**
     * Add a {@link PlayerListener} for player-only events. Remove with {@link #removeListener(PKListener)}.
     * Use {@link #addAdsListener(AdsListener)} for ad events and {@link #addEventListener(PKEvent.Listener, Enum[])} for events emitted by plugins.
     */
    @SuppressWarnings("UnusedReturnValue")
    PlayerListener addPlayerListener(PlayerListener playerListener);

    /**
     * Add an {@link AdsListener} for ads-only events. Remove with {@link #removeListener(PKListener)}.
     * Use {@link #addPlayerListener(PlayerListener)} for player events and {@link #addEventListener(PKEvent.Listener, Enum[])} for events emitted by plugins.
     */
    @SuppressWarnings("UnusedReturnValue")
    AdsListener addAdsListener(AdsListener adsListener);

    /**
     * Add event listener to the player.
     * NOTE: please use {@link #addPlayerListener(PlayerListener)} and {@link #addAdsListener(AdsListener)}
     * for player and ads events, respectively. The {@link PlayerEvent} and {@link com.kaltura.playkit.plugins.ads.AdEvent} classes
     * are deprecated.
     *
     * @param listener - event listener.
     * @param events   - events the subscriber interested in.
     */
    @SuppressWarnings("UnusedReturnValue")
    PKEvent.Listener addEventListener(@NonNull PKEvent.Listener listener, Enum... events);

    /**
     * Remove event listener to the player.
     *
     * @param listener - event listener.
     * @param events   - events the subscriber interested in.
     */
    void removeEventListener(@NonNull PKEvent.Listener listener, Enum... events);

    /**
     * @deprecated Use {@link #addPlayerListener(PlayerListener)} and override {@link PlayerListener#onPlayerStateChanged(PlayerState, PlayerState)}.
     *
     * @param listener - state changed listener
     */
    @Deprecated
    @SuppressWarnings("UnusedReturnValue")
    PKEvent.Listener addStateChangeListener(@NonNull PKEvent.Listener listener);

    /**
     * @deprecated See {@link #addStateChangeListener(PKEvent.Listener)} for details.
     *
     * @param listener - state changed listener
     */
    @Deprecated
    void removeStateChangeListener(@NonNull PKEvent.Listener listener);


    /**
     * Remove an event listener from all event types.
     *
     * @param listener - event listener to remove
     */
    void removeListener(@NonNull PKListener listener);
}

