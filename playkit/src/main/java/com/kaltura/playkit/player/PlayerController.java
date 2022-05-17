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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.android.exoplayer2.source.dash.manifest.EventStream;
import com.kaltura.playkit.Assert;
import com.kaltura.playkit.PKController;
import com.kaltura.playkit.PKDeviceCapabilities;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKTracksAvailableStatus;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEngineWrapper;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.ads.AdsPlayerEngineWrapper;
import com.kaltura.playkit.ads.AdvertisingConfig;
import com.kaltura.playkit.ads.PKAdvertisingController;
import com.kaltura.playkit.player.metadata.URIConnectionAcquiredInfo;
import com.kaltura.playkit.player.thumbnail.ThumbnailInfo;
import com.kaltura.playkit.utils.Consts;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.kaltura.playkit.utils.Consts.MILLISECONDS_MULTIPLIER;
import static com.kaltura.playkit.utils.Consts.POSITION_UNSET;
import static com.kaltura.playkit.utils.Consts.TIME_UNSET;

/**
 * @hide
 */

public class PlayerController implements Player {

    private static final PKLog log = PKLog.get("PlayerController");
    private Context context;
    private PKMediaConfig mediaConfig;
    private PKMediaSourceConfig sourceConfig;
    private PlayerSettings playerSettings = new PlayerSettings();
    private final Runnable updateProgressAction = initProgressAction();

    private PlayerEngine player;
    private PlayerEngineType currentPlayerType = PlayerEngineType.Unknown;

    private PlayerView rootPlayerView;
    private PlayerView playerEngineView;

    private String sessionId;
    private UUID playerSessionId = UUID.randomUUID();

    private long targetSeekPosition;
    private boolean isVideoTracksUpdated;
    private boolean isVideoTracksReset;
    private boolean isNewEntry = true;
    private boolean isPlayerStopped;

    private Handler handler = new Handler(Looper.getMainLooper());

    @NonNull
    private Profiler profiler = ProfilerFactory.get();

    private PKEvent.RawListener eventListener;
    private PlayerEngine.EventListener eventTrigger = initEventListener();
    private PlayerEngine.StateChangedListener stateChangedTrigger = initStateChangeListener();
    private PlayerEngineWrapper playerEngineWrapper;
    private Cache downloadCache;

    public PlayerController(Context context) {
        this.context = context;
        initializeRootPlayerView();
    }

    private void initializeRootPlayerView() {
        this.rootPlayerView = new PlayerView(context) {
            @Override
            public void hideVideoSurface() {
                setVideoSurfaceVisibility(false);
            }

            @Override
            public void showVideoSurface() {
                setVideoSurfaceVisibility(true);
            }

            @Override
            public void hideVideoSubtitles() {
                setVideoSubtitlesVisibility(false);

            }

            @Override
            public void showVideoSubtitles() {
                setVideoSubtitlesVisibility(true);

            }
        };
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.rootPlayerView.setLayoutParams(lp);
    }

    private void setVideoSurfaceVisibility(boolean isVisible) {
        String visibilityFunction = "showVideoSurface";
        if (!isVisible) {
            visibilityFunction = "hideVideoSurface";
        }

        if (player == null) {
            log.w("Error in " + visibilityFunction + " player is null");
            return;
        }

        PlayerView playerView = player.getView();
        if (playerView != null) {
            if (isVisible) {
                playerView.showVideoSurface();
            } else {
                playerView.hideVideoSurface();
            }
        } else {
            log.w("Error in " + visibilityFunction + " playerView is null");
        }
    }

    private void setVideoSubtitlesVisibility(boolean isVisible) {
        String visibilityFunction = "showVideoSubtitles";
        if (!isVisible) {
            visibilityFunction = "hideVideoSubtitles";
        }

        if (player == null) {
            log.w("Error in " + visibilityFunction + " player is null");
            return;
        }

        PlayerView playerView = player.getView();
        if (playerView != null) {
            if (isVisible) {
                playerView.showVideoSubtitles();
            } else {
                playerView.hideVideoSubtitles();
            }
        } else {
            log.w("Error in " + visibilityFunction + " playerView is null");
        }
    }

    @Override
    public Settings getSettings() {
        return playerSettings;
    }

    public void prepare(@NonNull PKMediaConfig mediaConfig) {

        if (sourceConfig == null) {
            log.e("source config not found. Can not prepare source.");
            return;
        }

        // Checking if AdsPlayerEngineWrapper is not there then make sure use force single if false.
        if (!(playerEngineWrapper instanceof AdsPlayerEngineWrapper)) {
            playerSettings.forceSinglePlayerEngine(false);
        }

        boolean is360Supported = sourceConfig.getVrSettings() != null && playerSettings.isVRPlayerEnabled();
        PlayerEngineType incomingPlayerType = PlayerEngineFactory.selectPlayerType(sourceConfig.mediaSource.getMediaFormat(), is360Supported);

        switchPlayersIfRequired(incomingPlayerType);

        if (assertPlayerIsNotNull("prepare()")) {
            player.setProfiler(profiler);
            player.load(sourceConfig);
        }
    }

    @Override
    public void setAdvertising(@NonNull PKAdvertisingController pkAdvertisingController, @Nullable AdvertisingConfig advertisingConfig) {
        Assert.shouldNeverHappen();
    }

    /**
     * Responsible for preparing source configurations before loading it to actual player.
     *
     * @param mediaConfig - the mediaConfig that holds necessary initial data.
     * @return - true if managed to create valid sourceConfiguration object,
     * otherwise will return false and notify user with the error that happened.
     */
    public boolean setMedia(PKMediaConfig mediaConfig) {
        log.v("setMedia");
        if (!isNewEntry) {
            isNewEntry = true;
            stop();
        }

        sessionId = generateSessionId();

        if (playerSettings.getContentRequestAdapter() != null) {
            playerSettings.getContentRequestAdapter().updateParams(this);
        }
        if (playerSettings.getLicenseRequestAdapter() != null) {
            playerSettings.getLicenseRequestAdapter().updateParams(this);
        }

        profiler.newSession(sessionId, playerSettings);
        profiler.onSetMedia(mediaConfig);

        this.mediaConfig = mediaConfig;
        PKMediaSource source = SourceSelector.selectSource(mediaConfig.getMediaEntry(), playerSettings.getPreferredMediaFormat());

        if (source == null) {
            sendErrorMessage(PKPlayerErrorType.SOURCE_SELECTION_FAILED, "No playable source found for entry", null);
            return false;
        }

        initSourceConfig(mediaConfig.getMediaEntry(), source);
        eventTrigger.onEvent(PlayerEvent.Type.SOURCE_SELECTED);

        return true;
    }

    private void initSourceConfig(PKMediaEntry mediaEntry, PKMediaSource source) {
        this.sourceConfig = new PKMediaSourceConfig(mediaConfig, source, playerSettings);
    }

    private String generateSessionId() {
        UUID mediaSessionId = UUID.randomUUID();
        String newSessionId = playerSessionId.toString();
        newSessionId += ":";
        newSessionId += mediaSessionId.toString();
        return newSessionId;
    }

    private void switchPlayersIfRequired(PlayerEngineType incomingPlayerType) {

        //If incomingPlayerType of the same type as current - we are good, so do nothing.
        if (currentPlayerType == incomingPlayerType) {
            return;
        }

        //Clear previous PlayerEngine.
        if (currentPlayerType != PlayerEngineType.Unknown) {
            removePlayerView();
            player.destroy();
        }

        //Initialize new PlayerEngine.
        try {
            player = PlayerEngineFactory.initializePlayerEngine(context, incomingPlayerType, playerSettings, rootPlayerView);
            if (downloadCache != null) {
                player.setDownloadCache(downloadCache);
            }

            if (playerEngineWrapper != null) {
                playerEngineWrapper.setPlayerEngine(player);
                player = playerEngineWrapper;
            }
        } catch (PlayerEngineFactory.PlayerInitializationException e) {
            log.e(e.getMessage());
            sendErrorMessage(PKPlayerErrorType.FAILED_TO_INITIALIZE_PLAYER, e.getMessage(), e);
            if (incomingPlayerType == PlayerEngineType.VRPlayer) {
                incomingPlayerType = PlayerEngineType.Exoplayer;
                player = new ExoPlayerWrapper(context, playerSettings, rootPlayerView);

            } else {
                return;
            }
        }

        //IMA workaround. In order to prevent flickering of the first frame
        //with ExoplayerEngine we should addPlayerView here for all playerEngines except Exoplayer.
        if (incomingPlayerType == PlayerEngineType.MediaPlayer) {
            addPlayerView();
        }
        togglePlayerListeners(true);
        currentPlayerType = incomingPlayerType;
    }

    private void addPlayerView() {
        if (playerEngineView != null) {
            return;
        }

        playerEngineView = player.getView();
        //always place playerView as first layer in view hierarchy.
        rootPlayerView.addView(playerEngineView, 0);
    }

    @Override
    public void destroy() {
        log.v("destroy");
        if (assertPlayerIsNotNull("destroy()")) {
            if (playerEngineView != null) {
                rootPlayerView.removeView(playerEngineView);
            }
            player.destroy();
            togglePlayerListeners(false);
        }
        player = null;
        mediaConfig = null;
        eventListener = null;
        currentPlayerType = PlayerEngineType.Unknown;
    }

    @Override
    public void stop() {
        log.v("stop");
        if (eventListener != null && !isPlayerStopped) {
            PlayerEvent event = new PlayerEvent.Generic(PlayerEvent.Type.STOPPED);
            cancelUpdateProgress();

            log.d("stop() isForceSinglePlayerEngine = " + playerSettings.isForceSinglePlayerEngine());
            if (!playerSettings.isForceSinglePlayerEngine()) {
                isPlayerStopped = true;
            }

            log.d("sending STOPPED event ");
            eventListener.onEvent(event);
            if (assertPlayerIsNotNull("stop()")) {
                player.stop();
            }
        }
    }

    private void startPlaybackFrom(long startPosition) {
        log.v("startPlaybackFrom " + startPosition);
        if (assertPlayerIsNotNull("startPlaybackFrom()")) {
            if (startPosition <= getDuration()) {
                player.startFrom(startPosition);
            } else {
                log.w("The start position is grater then duration of the video! Start position " + startPosition + ", duration " + getDuration());
            }
        }
    }

    public PlayerView getView() {
        return rootPlayerView;
    }

    @Override
    public <T extends PKController> T getController(Class<T> type) {
        log.v("getController");
        if (assertPlayerIsNotNull("getController()")) {
            return player.getController(type);
        }
        return null;
    }

    public long getDuration() {
        log.v("getDuration");
        if (assertPlayerIsNotNull("getDuration()")) {
            return player.getDuration();
        }
        return Consts.TIME_UNSET;
    }

    public long getCurrentPosition() {
        log.v("getCurrentPosition");
        if (assertPlayerIsNotNull("getCurrentPosition()")) {
            return player.getCurrentPosition();
        }
        return Consts.POSITION_UNSET;
    }

    @Override
    public long getPositionInWindowMs() {
        log.v("getPositionInWindowMs");
        if (assertPlayerIsNotNull("getPositionInWindowMs()")) {
            return player.getPositionInWindowMs();
        }
        return 0;
    }

    @Override
    public long getCurrentProgramTime() {
        if (assertPlayerIsNotNull("getCurrentProgramTime()")) {
            final long currentPosition = getCurrentPosition();
            final long programStartTime = player.getProgramStartTime();
            return currentPosition != POSITION_UNSET && programStartTime != TIME_UNSET ?
                    programStartTime + currentPosition : TIME_UNSET;
        }
        return TIME_UNSET;
    }

    public long getBufferedPosition() {
        log.v("getBufferedPosition");
        if (assertPlayerIsNotNull("getBufferedPosition()")) {
            return player.getBufferedPosition();
        }
        return Consts.POSITION_UNSET;
    }

    public long getCurrentLiveOffset() {
        log.v("getCurrentLiveOffset");
        if (assertPlayerIsNotNull("getCurrentLiveOffset()")) {
            return player.getCurrentLiveOffset();
        }
        return Consts.POSITION_UNSET;
    }

    public void seekTo(long position) {
        log.v("seek to " + position);
        if (assertPlayerIsNotNull("seekTo()")) {
            targetSeekPosition = position;
            player.seekTo(position);
        }
    }

    @Override
    public void seekToLiveDefaultPosition() {
        log.v("seekToLiveDefaultPosition");
        if (assertPlayerIsNotNull("seekToLiveDefaultPosition()") && player.isLive()) {
            player.seekToDefaultPosition();
        }
    }

    public void play() {
        log.v("play");
        if (assertPlayerIsNotNull("play()")) {
            player.play();
        }
    }

    public void pause() {
        log.v("pause");
        if (assertPlayerIsNotNull("pause()")) {
            player.pause();
        }
    }

    @Override
    public void replay() {
        log.v("replay");
        if (assertPlayerIsNotNull("replay()")) {
            player.replay();
        }
    }

    @Override
    public void setVolume(float volume) {
        log.v("setVolume");
        if (assertPlayerIsNotNull("setVolume()")) {
            player.setVolume(volume);
        }
    }

    @Override
    public boolean isPlaying() {
        log.v("isPlaying");
        if (assertPlayerIsNotNull("isPlaying()")) {
            return player.isPlaying();
        }
        return false;
    }

    private void togglePlayerListeners(boolean enable) {
        log.v("togglePlayerListeners");
        if (assertPlayerIsNotNull("togglePlayerListeners()")) {
            if (enable) {
                player.setEventListener(eventTrigger);
                player.setStateChangedListener(stateChangedTrigger);
                player.setAnalyticsListener(new PlayerEngine.AnalyticsListener() {
                    @Override
                    public void onDroppedFrames(long droppedVideoFrames, long droppedVideoFramesPeriod, long totalDroppedVideoFrames) {
                        if (eventListener != null) {
                            eventListener.onEvent(new PlayerEvent.VideoFramesDropped(droppedVideoFrames, droppedVideoFramesPeriod, totalDroppedVideoFrames));
                        }
                    }

                    @Override
                    public void onBytesLoaded(int trackType, int dataType, long bytesLoaded, long loadDuration, long totalBytesLoaded) {
                        if (eventListener != null) {
                            eventListener.onEvent(new PlayerEvent.BytesLoaded(trackType, dataType, bytesLoaded, loadDuration, totalBytesLoaded));
                        }
                    }

                    @Override
                    public void onConnectionAcquired(URIConnectionAcquiredInfo uriConnectionAcquiredInfo) {
                        if (eventListener != null) {
                            eventListener.onEvent(new PlayerEvent.ConnectionAcquired(uriConnectionAcquiredInfo));
                        }
                    }

                    @Override
                    public void onLoadError(IOException error, boolean wasCanceled) {
                        String errorStr = "onLoadError Player Load error: " + PKPlayerErrorType.LOAD_ERROR;
                        log.e(errorStr);
                        PKError loadError = new PKError(PKPlayerErrorType.LOAD_ERROR, PKError.Severity.Recoverable, errorStr, error);

                        if (eventListener != null) {
                            eventListener.onEvent(new PlayerEvent.Error(loadError));
                        }
                    }

                    @Override
                    public void onDecoderDisabled(int skippedOutputBufferCount, int renderedOutputBufferCount) {
                        if (eventListener != null) {
                            eventListener.onEvent(new PlayerEvent.OutputBufferCountUpdate(skippedOutputBufferCount, renderedOutputBufferCount));
                        }
                    }
                });
                player.setInputFormatChangedListener(true);
            } else {
                player.setEventListener(null);
                player.setStateChangedListener(null);
                player.setInputFormatChangedListener(null);
                player.setAnalyticsListener(null);
            }
        }
    }

    @Override
    public PKEvent.Listener addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        Assert.shouldNeverHappen();
        return null;
    }

    @Override
    public void removeEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        Assert.shouldNeverHappen();
    }

    @Override
    public PKEvent.Listener addStateChangeListener(@NonNull PKEvent.Listener listener) {
        Assert.shouldNeverHappen();
        return null;
    }

    @Override
    public void removeStateChangeListener(@NonNull PKEvent.Listener listener) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void removeListener(@NonNull PKEvent.Listener listener) {
        Assert.shouldNeverHappen();
    }

    @NonNull
    @Override
    public <PluginType> List<PluginType> getLoadedPluginsByType(Class<PluginType> pluginClass) {
        Assert.shouldNeverHappen();
        return null;
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void onApplicationPaused() {
        log.d("onApplicationPaused");

        profiler.onApplicationPaused();

        if (isPlayerStopped) {
            log.e("onApplicationPaused called during player state = STOPPED - return");
            return;
        }
        if (assertPlayerIsNotNull("onApplicationPaused()")) {
            if (player.isPlaying()) {
                player.pause();
            }
            cancelUpdateProgress();
            player.release();
            togglePlayerListeners(false);
        }
    }

    @Override
    public void onApplicationResumed() {
        log.d("onApplicationResumed");

        profiler.onApplicationResumed();

        if (isPlayerStopped) {
            log.e("onApplicationResumed called during player state = STOPPED");

            if (!playerSettings.isForceSinglePlayerEngine()) {
                log.d("onApplicationResumed called during player state = STOPPED - return, isForceSinglePlayerEngine = " + playerSettings.isForceSinglePlayerEngine());
                return;
            }
        }

        if (player != null && playerSettings.isForceSinglePlayerEngine()) {
            if (!isAdDisplayed()) {
                resumePlayer();
            }
        } else {
            resumePlayer();
        }
    }

    private void resumePlayer() {
        if (assertPlayerIsNotNull("onApplicationResumed()")) {
            player.restore();
            updateProgress();
        }
        togglePlayerListeners(true);
        prepare(mediaConfig);
    }

    @Override
    public void onOrientationChanged() {
        log.v("onOrientationChanged");
        if (assertPlayerIsNotNull("onOrientationChanged()")) {
            player.onOrientationChanged();
        }
    }

    @Override
    public void changeTrack(String uniqueId) {
        log.v("changeTrack");
        if (assertPlayerIsNotNull("changeTrack()")) {
            player.changeTrack(uniqueId);
        }
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public boolean isLive() {
        log.v("isLive");
        if (assertPlayerIsNotNull("isLive()")) {
            return player.isLive();
        }
        return false;
    }

    @Override
    public PKMediaFormat getMediaFormat() {
        if (sourceConfig != null) {
            return sourceConfig.mediaSource.getMediaFormat();
        }
        return null;
    }

    @Override
    public void setPlaybackRate(float rate) {
        log.v("setPlaybackRate");
        if (rate > 0) {
            if (assertPlayerIsNotNull("setPlaybackRate()")) {
                player.setPlaybackRate(rate);
            }
        } else {
            log.w("Playback rate should be greater than 0");
        }
    }

    @Override
    public float getPlaybackRate() {
        log.v("getPlaybackRate");
        if (assertPlayerIsNotNull("getPlaybackRate()")) {
            return player.getPlaybackRate();
        }
        return Consts.PLAYBACK_SPEED_RATE_UNKNOWN;
    }

    @Override
    public void setDownloadCache(Cache downloadCache) {
        log.v("setDownloadCache");

        if (!PKDeviceCapabilities.isKalturaPlayerAvailable()) {
            log.e("CacheDataSource is being used for Prefetch feature. This feature is not available in Playkit SDK. " +
                    "It is only being used by Kaltura Player SDK.");
            return;
        }

        if (assertPlayerIsNotNull("setDownloadCache()")) {
            player.setDownloadCache(downloadCache);
        }
        this.downloadCache = downloadCache;
    }

    @Override
    public ThumbnailInfo getThumbnailInfo(long ... positionMS) {
        log.v("getThumbnailInfo");
        if (assertPlayerIsNotNull("getThumbnailInfo()")) {
            if (positionMS.length > 0) {
                return player.getThumbnailInfo(positionMS[0]);
            } else {
                return player.getThumbnailInfo(player.getCurrentPosition());
            }
        }
        return null;
    }
    
    @Override
    public void updateSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings) {
        log.v("updateSubtitleStyle");
        if (assertPlayerIsNotNull("updateSubtitleStyle")) {
            player.updateSubtitleStyle(subtitleStyleSettings);
        }
    }

    @Override
    public void updateSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        log.v("updateSurfaceAspectRatioResizeMode");
        if (assertPlayerIsNotNull("updateSurfaceAspectRatioResizeMode")) {
            player.updateSurfaceAspectRatioResizeMode(resizeMode);
        }
    }

    @Override
    public void updatePKLowLatencyConfig(PKLowLatencyConfig pkLowLatencyConfig) {
        log.v("updatePKLowLatencyConfig");
        if (assertPlayerIsNotNull("updatePKLowLatencyConfig")) {
            player.updatePKLowLatencyConfig(pkLowLatencyConfig);
        }
    }

    @Override
    public void updateABRSettings(ABRSettings abrSettings) {
        log.v("updateABRSettings");

        if (!isVideoTrackPresent()) {
            return;
        }

        if (abrSettings == null || abrSettings.equals(ABRSettings.RESET)) {
            resetABRSettings();
            return;
        }

        if (abrSettings.equals(playerSettings.getAbrSettings())) {
            log.w("Existing and Incoming ABR Settings are same");
            return;
        }

        if (assertPlayerIsNotNull("updateABRSettings")) {
            isVideoTracksUpdated = true;
            player.updateABRSettings(abrSettings);
        }
    }

    @Override
    public void resetABRSettings() {
        log.v("resetABRSettings");

        if (!isVideoTrackPresent()) {
            return;
        }

        if (assertPlayerIsNotNull("resetABRSettings")) {
            isVideoTracksReset = true;
            player.resetABRSettings();
        }
    }

    @Override
    public <E extends PKEvent> void addListener(Object groupId, Class<E> type, PKEvent.Listener<E> listener) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void addListener(Object groupId, Enum type, PKEvent.Listener listener) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void removeListeners(@NonNull Object groupId) {
        Assert.shouldNeverHappen();
    }

    private boolean isVideoTrackPresent() {
        if (player != null &&
                player.getPKTracks() != null &&
                player.getPKTracks().getVideoTracks() != null &&
                player.getPKTracks().getVideoTracks().size() == 0) {
            log.w("No video track found for this media");
            return false;
        }
        return true;
    }

    private boolean assertPlayerIsNotNull(String methodName) {
        if (player != null) {
            return true;
        }
        String nullPlayerMsgFormat = "Attempt to invoke '%s' on null instance of the player engine";
        log.w(String.format(nullPlayerMsgFormat, methodName));
        return false;
    }

    private void removePlayerView() {
        togglePlayerListeners(false);
        rootPlayerView.removeView(playerEngineView);
        playerEngineView = null;
    }

    private void sendErrorMessage(Enum errorType, String errorMessage, @Nullable Exception exception) {
        log.e(errorMessage);
        PlayerEvent errorEvent = new PlayerEvent.Error(new PKError(errorType, errorMessage, exception));
        if (eventListener != null) {
            eventListener.onEvent(errorEvent);
        }
    }

    private void updateProgress() {
        //log.d("Start updateProgress");
        long position;
        long bufferPosition;
        long duration;

        if (player == null || player.getView() == null) {
            return;
        }

        position = player.getCurrentPosition();
        bufferPosition = player.getBufferedPosition();
        duration = player.getDuration();

        if (!isAdDisplayed()) {
            log.v("updateProgress new position/duration = " + position + "/" + duration);
            if (eventListener != null && position > 0 && duration > 0) {
                eventListener.onEvent(new PlayerEvent.PlayheadUpdated(position, bufferPosition, duration));
            }
        }
        // Cancel any pending updates and schedule a new one if necessary.
        handler.removeCallbacks(updateProgressAction);
        handler.postDelayed(updateProgressAction, Consts.DEFAULT_PLAYHEAD_UPDATE_MILI);

    }

    private boolean isAdDisplayed() {
        AdController adController = player.getController(AdController.class);
        return adController != null && adController.isAdDisplayed();
    }

    private Runnable initProgressAction() {
        return this::updateProgress;
    }

    private void cancelUpdateProgress() {
        if (player != null && player.getView() != null) {
            handler.removeCallbacks(updateProgressAction);
        }
    }

    public void setEventListener(PKEvent.RawListener eventListener) {
        this.eventListener = eventListener;
    }

    private PlayerEngine.EventListener initEventListener() {
        return eventType -> {
            if (eventListener != null) {

                PKEvent event;
                switch (eventType) {
                    case PLAYING:
                        updateProgress();
                        event = new PlayerEvent.Generic(eventType);
                        break;
                    case PAUSE:
                    case ENDED:
                        event = new PlayerEvent.Generic(eventType);
                        cancelUpdateProgress();
                        break;
                    case DURATION_CHANGE:
                        event = new PlayerEvent.DurationChanged(getDuration());
                        if (getDuration() != Consts.TIME_UNSET && isNewEntry) {
                            if (mediaConfig.getStartPosition() != null) {
                                if (mediaConfig.getStartPosition() * MILLISECONDS_MULTIPLIER > getDuration()) {
                                    mediaConfig.setStartPosition(getDuration() / MILLISECONDS_MULTIPLIER);
                                }
                                if (mediaConfig.getStartPosition() * MILLISECONDS_MULTIPLIER < 0) {
                                    mediaConfig.setStartPosition(0L);
                                }

                                if ((isLiveMediaWithDvr() && mediaConfig.getStartPosition() == 0) ||
                                        mediaConfig.getStartPosition() > 0) {
                                    startPlaybackFrom(mediaConfig.getStartPosition() * MILLISECONDS_MULTIPLIER);
                                }
                            } else if (isLiveMediaWithDvr() && mediaConfig.getStartPosition() == null) {
                                player.seekToDefaultPosition();
                            }
                            isNewEntry = false;
                            isPlayerStopped = false;
                        }
                        break;
                    case TRACKS_AVAILABLE:
                        PKTracksAvailableStatus pkTracksAvailableStatus = isVideoTracksUpdated ? PKTracksAvailableStatus.UPDATED: PKTracksAvailableStatus.NEW;
                        if (isVideoTracksReset) {
                            pkTracksAvailableStatus = PKTracksAvailableStatus.RESET;
                        }
                        event = new PlayerEvent.TracksAvailable(player.getPKTracks(), pkTracksAvailableStatus);
                        isVideoTracksUpdated = false;
                        isVideoTracksReset = false;
                        break;
                    case VOLUME_CHANGED:
                        event = new PlayerEvent.VolumeChanged(player.getVolume());
                        break;
                    case PLAYBACK_INFO_UPDATED:
                        if (player.getPlaybackInfo() == null) {
                            log.e("PLAYBACK_INFO_UPDATED event payload == null, ignoring event");
                            return;
                        }
                        event = new PlayerEvent.PlaybackInfoUpdated(player.getPlaybackInfo());
                        break;
                    case ERROR:
                        if (player.getCurrentError() == null) {
                            log.e("can not send error event");
                            return;
                        }
                        event = new PlayerEvent.Error(player.getCurrentError());
                        if (player.getCurrentError().isFatal()) {
                            cancelUpdateProgress();
                        }
                        break;
                    case METADATA_AVAILABLE:
                        if (player.getMetadata() == null || player.getMetadata().isEmpty()) {
                            log.w("METADATA_AVAILABLE event received, but player engine have no metadata.");
                            return;
                        }
                        event = new PlayerEvent.MetadataAvailable(player.getMetadata());
                        break;
                    case SOURCE_SELECTED:
                        event = new PlayerEvent.SourceSelected(sourceConfig.mediaSource);
                        break;
                    case SEEKING:
                        event = new PlayerEvent.Seeking(player.getCurrentPosition(), targetSeekPosition);
                        break;
                    case VIDEO_TRACK_CHANGED:
                        VideoTrack videoTrack = (VideoTrack) player.getLastSelectedTrack(Consts.TRACK_TYPE_VIDEO);
                        if (videoTrack == null) {
                            return;
                        }
                        event = new PlayerEvent.VideoTrackChanged(videoTrack);
                        break;
                    case AUDIO_TRACK_CHANGED:
                        AudioTrack audioTrack = (AudioTrack) player.getLastSelectedTrack(Consts.TRACK_TYPE_AUDIO);
                        if (audioTrack == null) {
                            return;
                        }
                        event = new PlayerEvent.AudioTrackChanged(audioTrack);
                        break;
                    case TEXT_TRACK_CHANGED:
                        TextTrack textTrack = (TextTrack) player.getLastSelectedTrack(Consts.TRACK_TYPE_TEXT);
                        if (textTrack == null) {
                            return;
                        }
                        event = new PlayerEvent.TextTrackChanged(textTrack);
                        break;
                    case EVENT_STREAMS_CHANGED:
                        List<EventStream> eventStreamList = player.getEventStreams();
                        if (eventStreamList == null || eventStreamList.isEmpty()) {
                            return;
                        }
                        event = new PlayerEvent.EventStreamChanged(eventStreamList);
                        break;
                    case IMAGE_TRACK_CHANGED:
                        ImageTrack imageTrack = (ImageTrack) player.getLastSelectedTrack(Consts.TRACK_TYPE_IMAGE);
                        if (imageTrack == null) {
                            return;
                        }
                        event = new PlayerEvent.ImageTrackChanged(imageTrack);
                        break;    
                    case PLAYBACK_RATE_CHANGED:
                        event = new PlayerEvent.PlaybackRateChanged(player.getPlaybackRate());
                        break;
                    case SUBTITLE_STYLE_CHANGED:
                        event = new PlayerEvent.SubtitlesStyleChanged(playerSettings.getSubtitleStyleSettings().getStyleName());
                        break;
                    case ASPECT_RATIO_RESIZE_MODE_CHANGED:
                        event = new PlayerEvent.SurfaceAspectRationResizeModeChanged(playerSettings.getAspectRatioResizeMode());
                        break;
                    default:
                        event = new PlayerEvent.Generic(eventType);
                }

                eventListener.onEvent(event);
            }
        };
    }

    private boolean isLiveMediaWithDvr() {
        return (PKMediaEntry.MediaEntryType.DvrLive == sourceConfig.mediaEntryType);
    }

    private PlayerEngine.StateChangedListener initStateChangeListener() {
        return (oldState, newState) -> {
            if (eventListener != null) {
                eventListener.onEvent(new PlayerEvent.StateChanged(newState, oldState));
            }
        };
    }

    public void setPlayerEngineWrapper(PlayerEngineWrapper playerEngineWrapper) {
        this.playerEngineWrapper = playerEngineWrapper;
    }
}
