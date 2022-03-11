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

package com.kaltura.playkit.player;

import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.playkit.PKAbrFilter;
import com.kaltura.playkit.PKController;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PlaybackInfo;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.player.metadata.PKMetadata;
import com.kaltura.playkit.player.metadata.URIConnectionAcquiredInfo;
import com.kaltura.playkit.player.thumbnail.ThumbnailInfo;
import com.kaltura.playkit.utils.Consts;

import java.io.IOException;
import java.util.List;

/**
 * Interface that connect between {@link PlayerController} and actual player engine
 * {@link ExoPlayerWrapper} or MediaPlayerWrapper. Depends on the type of media that
 * should play.
 * Created by anton.afanasiev on 01/11/2016.
 */

public interface PlayerEngine {

    /**
     * Initialize player (if needed), and load the mediaSourceUri
     * that should be played.
     *
     * @param mediaSourceConfig - the source to be played.
     */
    void load(PKMediaSourceConfig mediaSourceConfig);

    /**
     * Getter for the View to which current
     * surface is attached to.
     *
     * @return - {@link android.view.SurfaceView}
     */
    PlayerView getView();

    /**
     * Start players playback.
     * The player will start to play as soon as enough
     * data is buffered for playback.
     */
    void play();

    /**
     * Pause the players playback.
     */
    void pause();

    /**
     * Replay the currently playing/ended media.
     * This method will invoke {@link #seekTo(long)} but without triggering
     * events that related to this method (like SEEK and SEEKED).
     */
    void replay();

    /**
     * Getter for the current playback position.
     *
     * @return - position of the player or {@link Consts#POSITION_UNSET} if position is unknown or player is null
     */
    long getCurrentPosition();


    /**
     * Getter for the current playback position in window.
     *
     * @return - position of the player in window or 0 o/w.
     */
    default long getPositionInWindowMs() { return 0; }

    /**
     * The program start time, as set by availabilityStartTime in DASH or the EXT-X-PROGRAM-DATE-TIME in HLS.
     * @return The program start time in milliseconds since the epoch.
     */
    long getProgramStartTime();

    /**
     * @return - The total duration of current media
     * or {@link Consts#TIME_UNSET} if the duration is unknown or player is null.
     */
    long getDuration();

    /**
     * @return - The buffered position of the current media,
     * or {@link Consts#POSITION_UNSET} if the position is unknown or player is null.
     */
    long getBufferedPosition();

    /**
     * @return - The Current Live Offset of the media,
     * or {@link Consts#TIME_UNSET} if the offset is unknown or player is null.
     */
    long getCurrentLiveOffset();

    /**
     * @return - the volume of the current audio,
     * with 0 as total silence and 1 as maximum volume up.
     */
    float getVolume();

    /**
     * @return - the {@link PKTracks} object with all the available tracks info.
     */
    PKTracks getPKTracks();

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
     * Override media for video tracks with ABR
     *
     * @param minAbr - min ABR Value.
     * @param maxAbr - max ABR Value.
     */
    void overrideMediaDefaultABR(long minAbr, long maxAbr, PKAbrFilter pkAbrFilter);

    /**
     * Override codec for video tracks when more than 1 codec is available.
     *
     */
    void overrideMediaVideoCodec();

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
    default void seekToDefaultPosition() {}
    
    /**
     * Start players playback from the specified position.
     * Note! The position is passed in seconds.
     *
     * @param position - desired position.
     */
    void startFrom(long position);

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
     * Set the EventListener to the player, which will notify the
     * {@link PlayerController} about events that happened.
     *
     * @param eventTrigger - the event trigger.
     */
    void setEventListener(EventListener eventTrigger);

    /**
     * Set the StateChangeListener to the player, which will notify the
     * {@link PlayerController} about the players states changes.
     * Note! Same change that happens twice in a row will not be reported.
     *
     * @param stateChangedTrigger - the state change listener.
     */
    void setStateChangedListener(StateChangedListener stateChangedTrigger);


    void setAnalyticsListener(AnalyticsListener analyticsListener);

    /**
     * Release the current player.
     * Note, that {@link ExoPlayerWrapper} and {@link TrackSelectionHelper} objects, will be destroyed.
     * But first the last known playback position will be cached in order to restore it
     * when {@link #restore()} is called.
     */
    void release();

    /**
     * Restore the player to its previous state.
     * It also restore the last playback position of the player, if it have and can do so.
     * Otherwise it will reset player to the default position.
     */
    void restore();

    /**
     * Actually destroy the {@link PlayerEngine} implementation.
     * All the object related data (e.g lastKnownPosition) will be lost.
     */
    void destroy();

    /**
     * Holds current media url(as String), current playing video and audio bitrates.
     *
     * @return the playback params data object of the current media.
     */
    PlaybackInfo getPlaybackInfo();

    /**
     * Return the ExceptionInfo object, which holds the last error that happened,
     * and counter, which holds amount of the same exception that happened in a row.
     * This counter will help us to avoid the infinite loop, in case when we retry the playback, when handle the exception.
     *
     * @return - the last {@link PKError} that happened.
     */
    PKError getCurrentError();


    /**
     * Stop player executing the {@link PlayerEngine} implementation.
     * stop the player and seek to start position.
     */
    void stop();

    /**
     * Will return list of metadata objects, for the loaded entry.
     *
     * @return - list of {@link PKMetadata}
     */
    List<PKMetadata> getMetadata();

    BaseTrack getLastSelectedTrack(int renderType);

    boolean isLive();

    void setPlaybackRate(float rate);

    float getPlaybackRate();

    /**
     * Set the inputFormatChanged listener from AnalyticsListener
     * This listener is being used to get Video and Audio format
     * which is currently being played by the player.
     *
     * @param enableListener true to add / null to remove listener
     */
    default void setInputFormatChangedListener(Boolean enableListener) {}

    default void setProfiler(Profiler profiler) {}

    /**
     * Update Subtitle Styles
     * @param subtitleStyleSettings
     */
    void updateSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings);

    /**
      * Update View Size
      * @param resizeMode
      */
    default void updateSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {}
    
    default void updatePKLowLatencyConfig(PKLowLatencyConfig pkLowLatencyConfig) {}

    /**
     * Update the ABR Settings
     * @param abrSettings
     */
    default void updateABRSettings(ABRSettings abrSettings) {}

    /**
     * Reset the ABR Settings
     */
    default void resetABRSettings() {}

    /**
     * Generic getters for playkit controllers.
     *
     * @param type - type of the controller you want to obtain.
     * @return - the {@link PKController} instance if specified controller type exist,
     * otherwise return null.
     */
    default <T extends PKController> T getController(Class<T> type) { return null; }

    /**
     * Must be called by application when Android onConfigurationChanged triggered by system.
     */
    void onOrientationChanged();

    default void setDownloadCache(Cache downloadCache) {}

    default ThumbnailInfo getThumbnailInfo(long positionMS) { return null; }

    interface EventListener {
        void onEvent(PlayerEvent.Type event);
    }

    interface StateChangedListener {
        void onStateChanged(PlayerState oldState, PlayerState newState);
    }

    interface AnalyticsListener {
        void onDroppedFrames(long droppedVideoFrames, long droppedVideoFramesPeriod, long totalDroppedVideoFrames);
        void onBytesLoaded(int trackType, int dataType, long bytesLoaded, long loadDuration, long totalBytesLoaded);
        void onConnectionAcquired(URIConnectionAcquiredInfo uriConnectionAcquiredInfo);
        void onLoadError(IOException error, boolean wasCanceled);
        void onDecoderDisabled(int skippedOutputBufferCount, int renderedOutputBufferCount);
    }
}
