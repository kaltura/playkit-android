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
import android.media.MediaCodec;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.kaltura.playkit.Assert;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.PKError;

import java.util.UUID;

import static com.kaltura.playkit.PKMediaFormat.wvm;
import static com.kaltura.playkit.utils.Consts.MILLISECONDS_MULTIPLIER;

/**
 * @hide
 */

public class PlayerController implements Player {

    private static final PKLog log = PKLog.get("PlayerController");

    private PlayerEngine player;
    private Context context;
    private PlayerView rootPlayerView;
    private PKMediaConfig mediaConfig;
    private PKMediaSourceConfig sourceConfig;
    private PKEvent.Listener eventListener;
    private PlayerView playerEngineView;

    private String sessionId;
    private UUID playerSessionId = UUID.randomUUID();

    private PKRequestParams.Adapter contentRequestAdapter;

    private boolean isNewEntry = true;
    private boolean useTextureView = false;
    private boolean cea608CaptionsEnabled = false;

    private Settings settings = new Settings();

    private class Settings implements Player.Settings {

        @Override
        public Player.Settings setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter) {
            PlayerController.this.contentRequestAdapter = contentRequestAdapter;
            return this;
        }

        @Override
        public Player.Settings setCea608CaptionsEnabled(boolean cea608CaptionsEnabled) {
            PlayerController.this.cea608CaptionsEnabled = cea608CaptionsEnabled;
            return this;
        }

        @Override
        public Player.Settings useTextureView(boolean useTextureView) {
            PlayerController.this.useTextureView = useTextureView;
            return this;
        }
    }

    public void setEventListener(PKEvent.Listener eventListener) {
        this.eventListener = eventListener;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    interface EventListener {
        void onEvent(PlayerEvent.Type event);
    }

    interface StateChangedListener {
        void onStateChanged(PlayerState oldState, PlayerState newState);
    }

    private EventListener eventTrigger = new EventListener() {

        @Override
        public void onEvent(PlayerEvent.Type eventType) {
            if (eventListener != null) {

                PKEvent event;

                // TODO: use specific event class
                switch (eventType) {
                    case DURATION_CHANGE:
                        event = new PlayerEvent.DurationChanged(getDuration());
                        if (getDuration() != Consts.TIME_UNSET && isNewEntry) {
                            startPlaybackFrom(mediaConfig.getStartPosition() * MILLISECONDS_MULTIPLIER);
                            isNewEntry = false;
                        }
                        break;
                    case TRACKS_AVAILABLE:
                        event = new PlayerEvent.TracksAvailable(player.getPKTracks());
                        break;
                    case VOLUME_CHANGED:
                        event = new PlayerEvent.VolumeChanged(player.getVolume());
                        break;
                    case PLAYBACK_INFO_UPDATED:
                        event = new PlayerEvent.PlaybackInfoUpdated(player.getPlaybackInfo());
                        break;
                    case ERROR:
                        if (player.getCurrentError() == null) {
                            log.e("can not send error event");
                            return;
                        }

                        event = new PlayerEvent.Error(player.getCurrentError());
                        //If error should be handled locally, do not send it to messageBus.
                        if (maybeHandleExceptionLocally(player.getCurrentError())) {
                            return;
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
                    default:
                        event = new PlayerEvent.Generic(eventType);
                }

                eventListener.onEvent(event);
            }
        }
    };

    private StateChangedListener stateChangedTrigger = new StateChangedListener() {
        @Override
        public void onStateChanged(PlayerState oldState, PlayerState newState) {
            if (eventListener != null) {
                eventListener.onEvent(new PlayerEvent.StateChanged(newState, oldState));
            }
        }
    };

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
    public Player.Settings getSettings() {
        return settings;
    }


    public void prepare(@NonNull PKMediaConfig mediaConfig) {

        PKMediaSource source = sourceConfig.mediaSource;
        boolean shouldSwitchBetweenPlayers = shouldSwitchBetweenPlayers(source);
        if (player == null) {
            switchPlayers(source.getMediaFormat(), false);
        } else if (shouldSwitchBetweenPlayers) {
            switchPlayers(source.getMediaFormat(), true);
        }

        player.load(sourceConfig);

    }

    /**
     * Responsible for preparing source configurations before loading it to actual player.
     *
     * @param mediaConfig - the mediaConfig that holds necessary initial data.
     * @return - true if managed to create valid sourceConfiguration object,
     * otherwise will return false and notify user with the error that happened.
     */
    public boolean setMedia(PKMediaConfig mediaConfig) {
        log.d("setMedia");

        isNewEntry = true;

        sessionId = generateSessionId();
        if (contentRequestAdapter != null) {
            contentRequestAdapter.updateParams(this);
        }

        this.mediaConfig = mediaConfig;
        PKMediaSource source = SourceSelector.selectSource(mediaConfig.getMediaEntry());

        if (source == null) {
            sendErrorMessage(PKPlayerErrorType.SOURCE_SELECTION_FAILED, "No playable source found for entry");
            return false;
        }

        this.sourceConfig = new PKMediaSourceConfig(source, contentRequestAdapter, cea608CaptionsEnabled, useTextureView);
        eventTrigger.onEvent(PlayerEvent.Type.SOURCE_SELECTED);
        return true;
    }

    private String generateSessionId() {
        UUID mediaSessionId = UUID.randomUUID();
        String newSessionId = playerSessionId.toString();
        newSessionId += ":";
        newSessionId += mediaSessionId.toString();
        return newSessionId;
    }

    private void switchPlayers(PKMediaFormat mediaFormat, boolean removePlayerView) {
        if (removePlayerView) {
            removePlayerView();
        }

        if (player != null) {
            player.destroy();
        }
        initializePlayer(mediaFormat);
    }

    private void initializePlayer(PKMediaFormat mediaFormat) {
        //Decide which player wrapper should be initialized.
        if (mediaFormat != wvm) {
            player = new ExoPlayerWrapper(context);
            togglePlayerListeners(true);
        } else {
            player = new MediaPlayerWrapper(context);
            togglePlayerListeners(true);
            addPlayerView();
        }
    }

    private void addPlayerView() {
        if (playerEngineView != null) {
            return;
        }

        playerEngineView = player.getView();
        rootPlayerView.addView(playerEngineView);
    }

    @Override
    public void destroy() {
        log.d("destroy");
        if (player != null) {
            if (playerEngineView != null) {
                rootPlayerView.removeView(playerEngineView);
            }
            player.destroy();
            togglePlayerListeners(false);
        }
        player = null;
        mediaConfig = null;
        eventListener = null;
    }

    @Override
    public void stop() {
        if (player != null) {
            player.stop();
        }
    }

    private void startPlaybackFrom(long startPosition) {
        if (player == null) {
            log.w("Attempt to invoke 'startPlaybackFrom()' on null instance of the player engine");
            return;
        }

        if (startPosition <= getDuration()) {
            player.startFrom(startPosition);
        } else {
            log.w("The start position is grater then duration of the video! Start position " + startPosition + ", duration " + mediaConfig.getMediaEntry().getDuration());
        }
    }

    public PlayerView getView() {
        return rootPlayerView;
    }

    public long getDuration() {
        if (player == null) {
            return Consts.TIME_UNSET;
        }
        return player.getDuration();
    }

    public long getCurrentPosition() {
        if (player == null) {
            return Consts.POSITION_UNSET;
        }
        return player.getCurrentPosition();
    }

    public long getBufferedPosition() {
        if (player == null) {
            return Consts.POSITION_UNSET;
        }
        return player.getBufferedPosition();
    }

    public void seekTo(long position) {
        log.d("seek to " + position);
        if (player == null) {
            log.w("Attempt to invoke 'seekTo()' on null instance of the player engine");
            return;
        }
        player.seekTo(position);
    }

    @Override
    public AdController getAdController() {
        log.d("PlayerController getAdController");
        return null;
    }

    public void play() {
        log.d("play");

        if (player == null) {
            log.w("Attempt to invoke 'play()' on null instance of the player engine");
            return;
        }
        addPlayerView();
        player.play();
    }

    public void pause() {
        log.d("pause");
        if (player == null) {
            log.w("Attempt to invoke 'pause()' on null instance of the player engine");
            return;
        }
        player.pause();
    }

    @Override
    public void replay() {
        log.d("replay");
        if (player == null) {
            log.w("Attempt to invoke 'replay()' on null instance of the player engine");
            return;
        }
        player.replay();
    }

    @Override
    public void setVolume(float volume) {
        if (player == null) {
            log.w("Attempt to invoke 'setVolume()' on null instance of the player engine");
            return;
        }
        player.setVolume(volume);
    }

    @Override
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    private void togglePlayerListeners(boolean enable) {
        if (player == null) {
            return;
        }
        if (enable) {
            player.setEventListener(eventTrigger);
            player.setStateChangedListener(stateChangedTrigger);
        } else {
            player.setEventListener(null);
            player.setStateChangedListener(null);
        }
    }

    @Override
    public void prepareNext(@NonNull PKMediaConfig mediaConfig) {
        Assert.failState("Not implemented");
    }

    @Override
    public void skip() {
        Assert.failState("Not implemented");
    }

    @Override
    public void addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void addStateChangeListener(@NonNull PKEvent.Listener listener) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void onApplicationPaused() {
        log.d("onApplicationPaused");
        if (player == null) {
            log.w("Attempt to invoke 'release()' on null instance of the player engine");
            return;
        }

        player.release();
        togglePlayerListeners(false);
    }

    @Override
    public void onApplicationResumed() {
        log.d("onApplicationResumed");
        if (player != null) {
            player.restore();
        }
        togglePlayerListeners(true);
        prepare(mediaConfig);

    }

    @Override
    public void changeTrack(String uniqueId) {
        if (player == null) {
            log.w("Attempt to invoke 'changeTrack()' on null instance of the player engine");
            return;
        }

        player.changeTrack(uniqueId);
    }

    private boolean shouldSwitchBetweenPlayers(PKMediaSource newSource) {

        PKMediaFormat currentMediaFormat = newSource.getMediaFormat();
        return currentMediaFormat != wvm && player instanceof MediaPlayerWrapper ||
                currentMediaFormat == wvm && player instanceof ExoPlayerWrapper;

    }

    private void removePlayerView() {
        togglePlayerListeners(false);
        rootPlayerView.removeView(playerEngineView);
        playerEngineView = null;
    }

    private boolean maybeHandleExceptionLocally(PKError error) {
        if (error.errorType == PKPlayerErrorType.RENDERER_ERROR) {
            if (error.cause instanceof MediaCodec.CryptoException) {
                ExoPlayerWrapper exoPlayerWrapper = (ExoPlayerWrapper) player;
                long currentPosition = player.getCurrentPosition();
                exoPlayerWrapper.savePlayerPosition();
                exoPlayerWrapper.load(sourceConfig);
                exoPlayerWrapper.startFrom(currentPosition);
                return true;
            }
        }
        return false;
    }

    private void sendErrorMessage(Enum errorType, String errorMessage) {
        log.e(errorMessage);
        PlayerEvent errorEvent = new PlayerEvent.Error(new PKError(errorType, errorMessage, null));
        eventListener.onEvent(errorEvent);
    }
}
