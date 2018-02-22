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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.kaltura.playkit.Assert;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.utils.Consts;

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
    private PlayerSettings playerSettings = new PlayerSettings();
    private boolean isNewEntry = true;

    private long targetSeekPosition;


    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };


    public void setEventListener(PKEvent.Listener eventListener) {
        this.eventListener = eventListener;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public boolean isLive() {
        return player != null && player.isLive();
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
        if (player != null) {
            player.setPlaybackRate(rate);
        }
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
                switch (eventType) {
                    case PLAYING:
                        updateProgress();
                        event = new PlayerEvent.Generic(eventType);
                        break;
                    case PAUSE:
                    case ENDED:
                    case STOPPED:
                        event = new PlayerEvent.Generic(eventType);
                        cancelUpdateProgress();
                        break;
                    case DURATION_CHANGE:
                        event = new PlayerEvent.DurationChanged(getDuration());
                        if (getDuration() != Consts.TIME_UNSET && isNewEntry) {
                            if (!PKMediaEntry.MediaEntryType.Live.equals(sourceConfig.mediaEntryType)) {
                                startPlaybackFrom(mediaConfig.getStartPosition() * MILLISECONDS_MULTIPLIER);
                            } else {
                                if (mediaConfig.getStartPosition() < 0) {
                                    startPlaybackFrom(getDuration() + (mediaConfig.getStartPosition() * MILLISECONDS_MULTIPLIER));
                                }
                            }
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
                        cancelUpdateProgress();
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
                        event = new PlayerEvent.Seeking(targetSeekPosition);
                        break;
                    case VIDEO_TRACK_CHANGED:
                        event = new PlayerEvent.VideoTrackChanged((VideoTrack) player.getLastSelectedTrack(Consts.TRACK_TYPE_VIDEO));
                        break;
                    case AUDIO_TRACK_CHANGED:
                        event = new PlayerEvent.AudioTrackChanged((AudioTrack) player.getLastSelectedTrack(Consts.TRACK_TYPE_AUDIO));
                        break;
                    case TEXT_TRACK_CHANGED:
                        event = new PlayerEvent.TextTrackChanged((TextTrack) player.getLastSelectedTrack(Consts.TRACK_TYPE_TEXT));
                        break;
                    case PLAYBACK_RATE_CHANGED:
                        event = new PlayerEvent.PlaybackRateChanged(player.getPlaybackRate());
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
        this.rootPlayerView = new PlayerView(context);
    }

    @Override
    public Player.Settings getSettings() {
        return playerSettings;
    }


    public void prepare(@NonNull PKMediaConfig mediaConfig) {
        if (sourceConfig == null) {
            log.e("source config not found. Can not prepare source.");
            return;
        }
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
        if (playerSettings.getContentRequestAdapter() != null) {
            playerSettings.getContentRequestAdapter().updateParams(this);
        }

        this.mediaConfig = mediaConfig;
        PKMediaSource source = SourceSelector.selectSource(mediaConfig);

        if (source == null) {
            sendErrorMessage(PKPlayerErrorType.SOURCE_SELECTION_FAILED, "No playable source found for entry");
            return false;
        }

        this.sourceConfig = new PKMediaSourceConfig(mediaConfig, source, playerSettings);
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
        } else {
            player = new MediaPlayerWrapper(context);
        }

        togglePlayerListeners(true);
        addPlayerView();
    }

    private void addPlayerView() {
        if (playerEngineView != null) {
            return;
        }

        playerEngineView = player.getView();
        //Always place playerView as first layer in view hierarchy.
        rootPlayerView.addView(playerEngineView, 0);
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
        targetSeekPosition = position;
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
        if (player.isPlaying()) {
            player.pause();
        }
        cancelUpdateProgress();
        player.release();
        togglePlayerListeners(false);
    }

    @Override
    public void onApplicationResumed() {
        log.d("onApplicationResumed");
        if (player != null) {
            player.restore();
            updateProgress();
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

    private void sendErrorMessage(Enum errorType, String errorMessage) {
        log.e(errorMessage);
        PlayerEvent errorEvent = new PlayerEvent.Error(new PKError(errorType, errorMessage, null));
        eventListener.onEvent(errorEvent);
    }

    private void updateProgress() {

        long position;
        long duration;

        if (player == null || player.getView() == null) {
            return;
        }

        position = player.getCurrentPosition();
        duration = player.getDuration();
        if (position > 0 && duration > 0) {
            eventListener.onEvent(new PlayerEvent.PlayheadUpdated(position, duration));
        }

        // Cancel any pending updates and schedule a new one if necessary.
        player.getView().removeCallbacks(updateProgressAction);
        player.getView().postDelayed(updateProgressAction, Consts.DEFAULT_PLAYHEAD_UPDATE_MILI);

    }

    private void cancelUpdateProgress() {
        if (player != null && player.getView() != null) {
            player.getView().removeCallbacks(updateProgressAction);
        }
    }
}
