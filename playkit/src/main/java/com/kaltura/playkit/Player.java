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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.playkit.ads.AdvertisingConfig;
import com.kaltura.playkit.ads.PKAdvertisingController;
import com.kaltura.playkit.player.ABRSettings;
import com.kaltura.playkit.player.DRMSettings;
import com.kaltura.playkit.player.LoadControlBuffers;
import com.kaltura.playkit.player.MulticastSettings;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.PKLowLatencyConfig;
import com.kaltura.playkit.player.PKMaxVideoSize;
import com.kaltura.playkit.player.PlayerView;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.playkit.player.VideoCodecSettings;
import com.kaltura.playkit.player.AudioCodecSettings;
import com.kaltura.playkit.player.thumbnail.ThumbnailInfo;
import com.kaltura.playkit.player.vr.VRSettings;
import com.kaltura.playkit.utils.Consts;

import java.util.List;

@SuppressWarnings("unused")
public interface Player {

    /**
     * Interface used for setting optional Player settings.
     */
    @SuppressWarnings({"UnusedReturnValue", "unused"})
    interface Settings {
        /**
         * Set the Player's contentRequestAdapter.
         *
         * @param contentRequestAdapter - request adapter.
         * @return - Player Settings.
         */
        Settings setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter);

        /**
         * Set the Player's licenseRequestAdapter.
         *
         * @param licenseRequestAdapter - request adapter.
         * @return - Player Settings.
         */
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
        Settings useTextureView(boolean useTextureView);

        /**
         * Decide if player should do cross protocol redirect or not. By default it will be always set
         * to false.
         *
         * @param crossProtocolRedirectEnabled - true if should do cross protocol redirect.
         * @return - Player Settings.
         * @deprecated Please use {@link com.kaltura.playkit.PKRequestConfig} to set crossProtocolRedirect
         */
        @Deprecated
        Settings setAllowCrossProtocolRedirect(boolean crossProtocolRedirectEnabled);

        /**
         * Decide if player should play clear lead content
         *
         * @param allowClearLead - should enable/disable clear lead playback default true (enabled)
         * @return - Player Settings.
         * @deprecated Please use {@link #setDRMSettings(DRMSettings)} to set allowClearLead.
         */
        @Deprecated
        Settings allowClearLead(boolean allowClearLead);

        /**
         * Decide whether to enable fallback to lower-priority decoders if decoder initialization fails
         *
         * @param enableDecoderFallback - should enable/disable enableDecoderFallback default true (enabled)
         * @return - Player Settings.
         */
        Settings enableDecoderFallback(boolean enableDecoderFallback);

        /**
         * Decide if player should use secure rendering on the surface.
         * Known limitation - when useTextureView set to true and isSurfaceSecured set to true -
         * secure rendering will have no effect.
         *
         * @param isSurfaceSecured - should enable/disable secure rendering
         * @return - Player Settings.
         */
        Settings setSecureSurface(boolean isSurfaceSecured);

        /**
         * Decide the Ad will be auto played when comes to foreground from background
         *
         * @param autoPlayOnResume true if it is autoplayed or else false, default is TRUE
         * @return Player Settings
         */
        Settings setAdAutoPlayOnResume(boolean autoPlayOnResume);

        /**
         * Set the player buffers size
         *
         * @param loadControlBuffers LoadControlBuffers
         * @return Player Settings
         */
        Settings setPlayerBuffers(LoadControlBuffers loadControlBuffers);

        /**
         * Set the Player's VR/360 support
         *
         * @param vrPlayerEnabled - If 360 media should be played on VR player or default player - default == true.
         * @return - Player Settings.
         */
        Settings setVRPlayerEnabled(boolean vrPlayerEnabled);

        /**
         * Set the Player's preferredAudioTrackConfig.
         *
         * @param preferredAudioTrackConfig - AudioTrackConfig.
         * @return - Player Settings.
         */
        Settings setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig);

        /**
         * Set the Player's preferredTextTrackConfig.
         *
         * @param preferredTextTrackConfig - TextTrackConfig.
         * @return - Player Settings.
         */
        Settings setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig);

        /**
         * Set the Player's PreferredMediaFormat.
         *
         * @param preferredMediaFormat - PKMediaFormat.
         * @return - Player Settings.
         */
        Settings setPreferredMediaFormat(PKMediaFormat preferredMediaFormat);

        /**
         * Set the Player's Subtitles
         *
         * @param subtitleStyleSettings - SubtitleStyleSettings
         * @return - Player Settings
         */
        Settings setSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings);

        /**
         * Set the Player's ABR settings
         *
         * @param abrSettings ABR settings
         * @return - Player Settings
         */
        Settings setABRSettings(ABRSettings abrSettings);

        /**
         * Set the Player's AspectRatio resize Mode
         *
         * @param resizeMode Resize mode
         * @return - Player Settings
         */
        Settings setSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode);

        /**
         * Do not prepare the content player when the Ad starts(if exists); instead content player will be prepared
         * when content_resume_requested is called.
         * <p>
         * Default value is set to 'false'.
         *
         * @param forceSinglePlayerEngine Do not prepare the content player while Ad is playing
         * @return - Player Settings
         */
        Settings forceSinglePlayerEngine(boolean forceSinglePlayerEngine);

        /**
         * Set the flag which handles the video view
         *
         * @param hide video surface visibility
         * @return - Player Settings
         */
        Settings setHideVideoViews(boolean hide);

        /**
         * Set VR Settings on the player
         *
         * @param vrSettings vr configuration
         * @return - Player Settings
         */
        Settings setVRSettings(VRSettings vrSettings);

        /**
         * Set Preferred codec for video track
         *
         * @param videoCodecSettings Use {@link VideoCodecSettings}
         */
        Settings setPreferredVideoCodecSettings(VideoCodecSettings videoCodecSettings);

        /**
         * Set Preferred codec for audio track
         *
         * @param audioCodecSettings Use {@link AudioCodecSettings}
         */
        Settings setPreferredAudioCodecSettings(AudioCodecSettings audioCodecSettings);

        /**
         * Set custom load control strategy
         *
         * @param loadControlStrategy object implementing LoadControlStrategy interface
         * @return - Player Settings
         */
        Settings setCustomLoadControlStrategy(Object loadControlStrategy);

        /**
         * Set Tunneled Audio Playback
         *
         * @param isTunneledAudioPlayback audio tunnelling enabled
         * @return - Player Settings
         */
        Settings setTunneledAudioPlayback(boolean isTunneledAudioPlayback);

        /**
         * Set HandleAudioBecomingNoisy - Sets whether the player should pause automatically
         * when audio is rerouted from a headset to device speakers
         *
         * @param handleAudioBecomingNoisyEnabled
         * @return - Player Settings
         */
        Settings setHandleAudioBecomingNoisy(boolean handleAudioBecomingNoisyEnabled);

        /**
         * Set WakeLock Mode  - Sets whether the player should not handle wakeLock or should handle a wake lock only or both wakeLock & wifiLock when the screen is off
         *
         * <p>It should be used together with a foreground {@link android.app.Service} for use cases where
         * playback occurs and the screen is off (e.g. background audio playback). It is not useful when
         * the screen will be kept on during playback (e.g. foreground video playback).
         *
         * <p>When enabled, the locks ({@link android.os.PowerManager.WakeLock} / {@link
         * android.net.wifi.WifiManager.WifiLock}) will be held whenever the player is in the
         * STATE_READY STATE_BUFFERINGstates with {@code playWhenReady = true}. The locks
         * held depends on the specified {@link PKWakeMode}.
         * default - NONE - not handling wake lock
         *
         * @param wakeMode
         * @return - Player Settings
         */
        Settings setWakeMode(PKWakeMode wakeMode);

        /**
         * Set HandleAudioFocus - Support for automatic audio focus handling
         *
         * @param handleAudioFocus
         * @return - Player Settings
         */
        Settings setHandleAudioFocus(boolean handleAudioFocus);

        /**
         * Set preference to choose internal subtitles over external subtitles (Only in the case if the same language is present
         * in both Internal and External subtitles) - Default is true (Internal is preferred)
         *
         * @param subtitlePreference PKSubtitlePreference.INTERNAL, Internal will be present and External subtitle will be discarded
         *                           PKSubtitlePreference.EXTERNAL, External will be present and Internal subtitle will be discarded
         *                           PKSubtitlePreference.OFF, Both internal and external subtitles will be there
         * @return - Player Settings
         */
        Settings setSubtitlePreference(PKSubtitlePreference subtitlePreference);

        /**
         * Sets the maximum allowed video width and height.
         * to set the maximum allowed video bitrate to sd resolution call:
         * setMaxVideoSize(new PKMaxVideoSize().setMaxVideoWidth(1279).setMaxVideoHeight(719)
         * to reset call:
         * setMaxVideoSize(new PKMaxVideoSize().setMaxVideoWidth(Integer.MAX_VALUE).setMaxVideoHeight(Integer.MAX_VALUE)
         *
         * @param maxVideoSize - Max allowed video width and height
         * @return - Player Settings
         * @deprecated Please use {@link #setABRSettings(ABRSettings)} to set max video size.
         */
        @Deprecated
        Settings setMaxVideoSize(@NonNull PKMaxVideoSize maxVideoSize);

        /**
         *
         * Sets the maximum allowed video bitrate.
         *
         * @param maxVideoBitrate Maximum allowed video bitrate in bits per second.
         * @return - Player Settings
          @deprecated Please use {@link #setABRSettings(ABRSettings)} to set max video bitrate.
         */
        @Deprecated
        Settings setMaxVideoBitrate(@NonNull Integer maxVideoBitrate);

        /**
         * Sets the maximum allowed audio bitrate
         *
         * @param maxAudioBitrate - maximum allowed audio bitrate in bits per second.
         * @return - Player Settings
         */
        Settings setMaxAudioBitrate(@NonNull Integer maxAudioBitrate);

        /**
         * Sets the maximum allowed audio channel count.
         *
         * @param maxAudioChannelCount - maximum allowed audio channel count. default max = Integer.MAX_VALUE
         * @return - Player Settings
         */
        Settings setMaxAudioChannelCount(int maxAudioChannelCount);

        /**
         * Sets the multicastSettings for udp streams.
         *
         * @param multicastSettings - maxPacketSize default = 3000 & socketTimeoutMillis default = 10000
         * @return - Player Settings
         */
        Settings setMulticastSettings(MulticastSettings multicastSettings);

        /**
         * If the device codec is known to fail if security level L1 is used
         * then set flag to true, it will force the player to use Widevine L3
         * Will work only SDK level 18 or above
         *
         * @param forceWidevineL3Playback - force the L3 Playback. Default is false
         * @return - Player Settings
         * @deprecated Please use {@link #setDRMSettings(DRMSettings)} to forceWidevineL3Playback.
         */
        @Deprecated
        Settings forceWidevineL3Playback(boolean forceWidevineL3Playback);

        /**
         * Creates a DRM playback configuration.
         *
         * @param drmSettings - Configuration for DRM playback Widevine/Playready default is Widevine
         * @return - Player Settings
         */
        Settings setDRMSettings(DRMSettings drmSettings);

        /**
         * Creates a Low Latency Live playback configuration.
         *
         * @param pkLowLatencyConfig - Configuration for Low Latency
         * @return - Player Settings
         */
        Settings setPKLowLatencyConfig(PKLowLatencyConfig pkLowLatencyConfig);

        /**
         * Creates a request configuration for HttpDataSourceFactory {@link com.kaltura.playkit.player.ExoPlayerWrapper}.
         *
         * @param pkRequestConfig - Configuration for PKRequestConfig
         * @return - Player Settings
         */
        Settings setPKRequestConfig(PKRequestConfig pkRequestConfig);
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

    /**
     * Used by Kaltura-Player SDK internally for AdvertisingConfiguration.
     * @param pkAdvertisingController Controller, it resides in Kaltura-Player
     * @param advertisingConfig AdvertisingConfig
     */
    void setAdvertising(@NonNull PKAdvertisingController pkAdvertisingController, @Nullable AdvertisingConfig advertisingConfig);

    void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig);
    
    /**
     * Used by Kaltura-Player SDK internally for ExoOffline provider.
     * This feature is blocked for being used directly by Playkit SDK.
     *
     * @param downloadCache internally build the CacheDataSource
     */
    void setDownloadCache(Cache downloadCache);

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
     * Getter for the current playback position in window.
     *
     * @return - position of the player in window or 0 o/w.
     */
    long getPositionInWindowMs();

    /**
     * The current program time in milliseconds since the epoch, or {@link Consts#TIME_UNSET} if not set.
     * This value is derived from the attribute availabilityStartTime in DASH or the tag EXT-X-PROGRAM-DATE-TIME in HLS.
     *
     * @return The current program time in milliseconds since the epoch, or {@link Consts#TIME_UNSET} if not set.
     */
    long getCurrentProgramTime();

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
     * @return - The Current Live Offset of the media,
     * or {@link Consts#TIME_UNSET} if the offset is unknown or player engine is null.
     */
    long getCurrentLiveOffset();

    /**
     * Change the volume of the current audio track.
     * Accept values between 0.0 and 1.0. Where 0.0 is mute and 1.0 is maximum volume.
     * If the volume parameter is higher then 1.0, it will be converted to 1.0.
     * If the volume parameter is lower then 0.0, it be converted to 0.0.
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
     * Seek player to Live Default Position.
     *
     */
    void seekToLiveDefaultPosition();

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
     * get the Information for a thumbnailImage by position
     * if positionMS is not passed current position will be used
     *
     * @param positionMS - relevant image for given player position. (optional)
     */
    ThumbnailInfo getThumbnailInfo(long ... positionMS);
    
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
     * Update video size
     */
    void updateSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode);

    /**
     * Update Low Latency configuration
     */
    void updatePKLowLatencyConfig(PKLowLatencyConfig pkLowLatencyConfig);

    /** Update ABRSettings
     * <br>
     * Updating {@link ABRSettings#setInitialBitrateEstimate(Long)} is unaffected because
     * initial bitrate is only meant at the start of the playback
     * <br>
     * @param abrSettings new ABR Settings
     */
    void updateABRSettings(ABRSettings abrSettings);

    /**
     * Reset existing ABRSettings
     */
    void resetABRSettings();

    /**
     * Add listener by event type as Class object. This generics-based method allows the caller to
     * avoid the otherwise required cast.
     * <p>
     * Sample usage:
     * <pre>
     *   player.addListener(this, PlayerEvent.stateChanged,
     *      event -> Log.d(TAG, "Player state change: " + event.oldState + " => " + event.newState));
     * </pre>
     *
     * @param groupId  listener group id for calling {@link #removeListeners(Object)}
     * @param type     A typed {@link Class} object. The class type must extend PKEvent.
     * @param listener a typed {@link PKEvent.Listener}. Must match the type given as the first parameter.
     * @param <E>      Event type.
     */
    <E extends PKEvent> void addListener(Object groupId, Class<E> type, PKEvent.Listener<E> listener);

    /**
     * Add listener by event type as enum, for use with events that don't have payloads.
     * <p>
     * Sample usage:
     * <pre>
     *   player.addListener(this, PlayerEvent.canPlay, event -> {
     *       Log.d(TAG, "Player can play");
     *   });
     * </pre>
     *
     * @param groupId  listener group id for calling {@link #removeListeners(Object)}
     * @param type     event type
     * @param listener listener
     */
    void addListener(Object groupId, Enum type, PKEvent.Listener listener);

    /**
     * Remove all listeners that belong to the group.
     *
     * @param groupId listener group id as passed to {@link #addListener(Object, Enum, PKEvent.Listener)} or {@link #addListener(Object, Class, PKEvent.Listener)}.
     */
    void removeListeners(@NonNull Object groupId);

    /**
     * Remove event listener, regardless of event type.
     *
     * @param listener - event listener
     */
    void removeListener(@NonNull PKEvent.Listener listener);

    /**
     * Get loaded plugins of type.
     *
     * @param pluginClass - PluginType class.
     */
    @NonNull <PluginType> List<PluginType> getLoadedPluginsByType(Class<PluginType> pluginClass);

    /**
     * Add event listener to the player.
     *
     * @param listener - event listener.
     * @param events   - events the subscriber interested in.
     * @deprecated It's better to use one listener per event type with {@link #addListener(Object, Enum, PKEvent.Listener)} or {@link #addListener(Object, Class, PKEvent.Listener)}.
     */
    @Deprecated
    PKEvent.Listener addEventListener(@NonNull PKEvent.Listener listener, Enum... events);

    /**
     * Remove event listener from the player.
     *
     * @param listener - event listener.
     * @param events   - events the subscriber interested in.
     * @deprecated See {@link #addEventListener(PKEvent.Listener, Enum[])} for deprecation note.
     */
    @Deprecated
    void removeEventListener(@NonNull PKEvent.Listener listener, Enum... events);

    /**
     * Add state changed listener to the player.
     *
     * @param listener - state changed listener
     * @deprecated Please use {@link #addListener(Object, Class, PKEvent.Listener)} with {@link PlayerEvent#stateChanged}
     * and remove with {@link #removeListeners(Object)}.
     */
    @Deprecated
    PKEvent.Listener addStateChangeListener(@NonNull PKEvent.Listener listener);

    /**
     * Remove state changed listener from the player.
     *
     * @param listener - state changed listener
     * @deprecated See {@link #addStateChangeListener(PKEvent.Listener)} for deprecation note.
     */
    @Deprecated
    void removeStateChangeListener(@NonNull PKEvent.Listener listener);
}
