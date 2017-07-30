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
import com.kaltura.playkit.PKController;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.PKError;

import java.util.UUID;

import static com.kaltura.playkit.utils.Consts.MILLISECONDS_MULTIPLIER;

/**
 * @hide
 */

public class PlayerController implements Player {

    private static final PKLog log = PKLog.get("PlayerController");

    private Context context;
    private PlayerEngine player;

    private PlayerView rootPlayerView;
    private PlayerView playerEngineView;

    private PKMediaConfig mediaConfig;
    private PKMediaSourceConfig sourceConfig;
    private PKRequestParams.Adapter contentRequestAdapter;
    private PlayerEngineType currentPlayerEngineType = PlayerEngineType.UNKNOWN;

    private String sessionId;
    private UUID playerSessionId = UUID.randomUUID();

    private PKEvent.Listener eventListener;

    private Settings settings = new Settings();
    private boolean isNewEntry = true;
    private boolean useTextureView = false;
    private boolean cea608CaptionsEnabled = false;

    private PlayerEngine.EventListener eventTrigger = initializeEventListener();
    private PlayerEngine.StateChangedListener stateChangedTrigger = initializeStateChangedListener();


    public PlayerController(Context context) {
        this.context = context;
        initializeRootPlayerView();
    }

    private void initializeRootPlayerView() {
        this.rootPlayerView = new PlayerView(context) {
            @Override
            public void setVideoSurfaceVisibility(int visibilityState) {
                if (player == null) {
                    log.w("Failed to change PlayerView state. Player is null");
                    return;
                }

                if(playerEngineView != null) {
                    playerEngineView.setVideoSurfaceVisibility(visibilityState);
                }else {
                    log.w("Failed to change PlayerView state. PlayerView is null");
                }
            }

            @Override
            public void setVideoSubtitlesVisibility(int visibilityState) {
                if (player == null) {
                    log.w("Failed to change SubtitlesView state. Player is null");
                    return;
                }

                if(playerEngineView != null) {
                    playerEngineView.setVideoSubtitlesVisibility(visibilityState);
                } else {
                    log.w("Failed to change SubtitlesView state. PlayerView is null");
                }

            }
        };

        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.rootPlayerView.setLayoutParams(lp);
    }

    public void prepare(@NonNull PKMediaConfig mediaConfig) {

        PKMediaFormat mediaFormat = sourceConfig.getSource().getMediaFormat();
        boolean is360supported = mediaConfig.getMediaEntry().getVrParams() != null;

        PlayerEngineType incomingPlayerEngineType = PlayerEngineFactory.selectPlayerType(mediaFormat, is360supported);
        if (currentPlayerEngineType != incomingPlayerEngineType) {
            switchPlayers(incomingPlayerEngineType);
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

    private void switchPlayers(PlayerEngineType incomingPlayerEngineType) {

        if (currentPlayerEngineType != PlayerEngineType.UNKNOWN) {
            removePlayerView();
            player.destroy();
        }

        try {
            player = PlayerEngineFactory.getPlayerEngine(context, incomingPlayerEngineType);
            currentPlayerEngineType = incomingPlayerEngineType;
            togglePlayerListeners(true);
        } catch (PlayerEngineFactory.PlayerInitializationException exception) {
            PKError error = new PKError(PKPlayerErrorType.FAILED_TO_INITIALIZE_PLAYER,
                    exception.getMessage(), exception);
            eventListener.onEvent(new PlayerEvent.Error(error));
        }
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
    public void stop() {
        if (player != null) {
            player.stop();
        }
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
    public void setVolume(float volume) {
        if (player == null) {
            log.w("Attempt to invoke 'setVolume()' on null instance of the player engine");
            return;
        }
        player.setVolume(volume);
    }

    @Override
    public void changeTrack(String uniqueId) {
        if (player == null) {
            log.w("Attempt to invoke 'changeTrack()' on null instance of the player engine");
            return;
        }

        player.changeTrack(uniqueId);
    }

    @Override
    public boolean isPlaying() {
        return player != null && player.isPlaying();
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

    public PlayerView getView() {
        return rootPlayerView;
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
        currentPlayerEngineType = PlayerEngineType.UNKNOWN;
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
    public PKController getController(Class<? extends PKController> type) {

        if (type == VRController.class && currentPlayerEngineType == PlayerEngineType.VR_PLAYER) {
            return new VRController() {
                @Override
                public void enableVRMode(boolean shouldEnable) {
                    log.e("enableVRMode");
                }
            };
        }

        return null;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public Player.Settings getSettings() {
        return settings;
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void addStateChangeListener(@NonNull PKEvent.Listener listener) {
        Assert.shouldNeverHappen();
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

    private void addPlayerView() {
        if (playerEngineView != null) {
            return;
        }

        playerEngineView = player.getView();
        rootPlayerView.addView(playerEngineView);
    }

    private void removePlayerView() {
        togglePlayerListeners(false);
        rootPlayerView.removeView(playerEngineView);
        playerEngineView = null;
    }

    public void setEventListener(PKEvent.Listener eventListener) {
        this.eventListener = eventListener;
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

    private void sendErrorMessage(Enum errorType, String errorMessage) {
        log.e(errorMessage);
        PlayerEvent errorEvent = new PlayerEvent.Error(new PKError(errorType, errorMessage, null));
        eventListener.onEvent(errorEvent);
    }

    @NonNull
    private PlayerEngine.EventListener initializeEventListener() {
        return new PlayerEngine.EventListener() {

            @Override
            public void onEvent(PlayerEvent.Type eventType) {
                if (eventListener != null) {

                    PKEvent event;

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
                            break;
                        case METADATA_AVAILABLE:
                            if (player.getMetadata() == null || player.getMetadata().isEmpty()) {
                                log.w("METADATA_AVAILABLE event received, but player engine have no metadata.");
                                return;
                            }
                            event = new PlayerEvent.MetadataAvailable(player.getMetadata());
                            break;
                        case SOURCE_SELECTED:
                            event = new PlayerEvent.SourceSelected(sourceConfig.getSource());
                            break;
                        default:
                            event = new PlayerEvent.Generic(eventType);
                    }

                    eventListener.onEvent(event);
                }
            }
        };
    }

    @NonNull
    private PlayerEngine.StateChangedListener initializeStateChangedListener() {
        return new PlayerEngine.StateChangedListener() {
            @Override
            public void onStateChanged(PlayerState oldState, PlayerState newState) {
                if (eventListener != null) {
                    eventListener.onEvent(new PlayerEvent.StateChanged(newState, oldState));
                }
            }
        };
    }

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
}
