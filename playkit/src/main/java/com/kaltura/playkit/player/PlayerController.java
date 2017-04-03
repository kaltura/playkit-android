package com.kaltura.playkit.player;

import android.content.Context;
import android.media.MediaCodec;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.kaltura.playkit.Assert;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKRequestInfo;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.utils.Consts;

import java.util.UUID;

import static com.kaltura.playkit.utils.Consts.MILLISECONDS_MULTIPLIER;

/**
 * @hide
 */

public class PlayerController implements Player {

    private static final PKLog log = PKLog.get("PlayerController");
    private static final int ALLOWED_ERROR_RETRIES = 3;



    private PlayerEngine player = null;
    private Context context;
    private PlayerView rootPlayerView;

    private PKMediaConfig mediaConfig;
    private PKMediaSourceConfig sourceConfig;

    private PKEvent.Listener eventListener;
    private PlayerView playerEngineView;

    private UUID sessionId = UUID.randomUUID();
    private PKRequestInfo.Decorator contentRequestDecorator;

    private boolean isNewEntry = true;
    private Settings settings = new Settings();

    private class Settings implements Player.Settings {

        @Override
        public Player.Settings setContentRequestDecorator(PKRequestInfo.Decorator contentRequestDecorator) {
            PlayerController.this.contentRequestDecorator = contentRequestDecorator;
            return this;
        }
    }



    public void setEventListener(PKEvent.Listener eventListener) {
        this.eventListener = eventListener;
    }

    @Override
    public UUID getSessionId() {
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

                PlayerEvent event;

                // TODO: use specific event class
                switch (eventType) {
                    case DURATION_CHANGE:
                        event = new PlayerEvent.DurationChanged(getDuration());
                        if (getDuration() != Consts.TIME_UNSET && isNewEntry) {
                            startPlaybackFrom(mediaConfig.getStartPosition() * MILLISECONDS_MULTIPLIER);
                        }
                        break;
                    case TRACKS_AVAILABLE:
                        event = new PlayerEvent.TracksAvailable(player.getPKTracks());
                        break;
                    case VOLUME_CHANGED:
                        event = new PlayerEvent.VolumeChanged(player.getVolume());
                        break;
                    case PLAYBACK_PARAMS_UPDATED:
                        event = new PlayerEvent.PlaybackParamsUpdated(player.getPlaybackParamsInfo());
                        break;
                    case ERROR:
                        event = player.getCurrentException();
                        PlayerEvent.ExceptionInfo exceptionInfo = (PlayerEvent.ExceptionInfo) event;
                        if (exceptionInfo == null || exceptionInfo.getException() == null) {
                            return;
                        }

                        //if exception should be handled locally, do not send it to message bus.
                        if (maybeHandleExceptionLocally(exceptionInfo)) {
                            return;
                        }
                        break;
                    case ID3_METADATA_AVAILABLE:
                        event = new PlayerEvent.Id3MetadataAvailable(player.getId3Metadata());
                        break;
                    case EMSG_METADATA_AVAILABLE:
                        event = new PlayerEvent.EmsgMetadataAvailable(player.getEmsgMetadata());
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
            log.e("Error in " + visibilityFunction + " player is null");
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
            log.e("Error in " + visibilityFunction + " playerView is null");
        }
    }

    @Override
    public Player.Settings getSettings() {
        return settings;
    }

    public void prepare(@NonNull PKMediaConfig mediaConfig) {

        isNewEntry = isNewEntry(mediaConfig);
        this.mediaConfig = mediaConfig;

        PKMediaSource source = SourceSelector.selectSource(mediaConfig.getMediaEntry());

        if (source == null) {
            log.e("No playable source found for entry");
            return;
        }

        boolean shouldSwitchBetweenPlayers = shouldSwitchBetweenPlayers(source);

        if (shouldSwitchBetweenPlayers || player == null) { //if player is null consider it as switch player flow.
            switchPlayers(source.getMediaFormat());
        }


        this.sourceConfig = new PKMediaSourceConfig(source, contentRequestDecorator);

        player.load(sourceConfig);
    }

    private void switchPlayers(PKMediaFormat mediaFormat) {
        removeAllViews();

        if (player != null) {
            player.destroy();
        }
        initializePlayer(mediaFormat);

        addPlayerView();
    }


    private void initializePlayer(PKMediaFormat mediaFormat) {
        //Decide which player wrapper should be initialized.
        if (mediaFormat != PKMediaFormat.wvm) {
            player = new ExoPlayerWrapper(context);
            togglePlayerListeners(true);
        } else {
            player = new MediaPlayerWrapper(context);
            togglePlayerListeners(true);
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
        log.e("destroy");
        if (player != null) {
            player.destroy();
            togglePlayerListeners(false);
        }
        player = null;
        mediaConfig = null;
        eventListener = null;
    }

    @Override
    public void stop() {
        player.stop();
    }

    private void startPlaybackFrom(long startPosition) {
        if (player == null) {
            log.e("Attempt to invoke 'startPlaybackFrom()' on null instance of the player engine");
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
            log.e("Attempt to invoke 'seekTo()' on null instance of the player engine");
            return;
        }
        player.seekTo(position);
    }

    @Override
    public AdController getAdController() {
        return null;
    }

    public void play() {
        log.d("play");
        if (player == null) {
            log.e("Attempt to invoke 'play()' on null instance of the player engine");
            return;
        }


        player.play();
    }

    public void pause() {
        log.d("pause");
        if (player == null) {
            log.e("Attempt to invoke 'pause()' on null instance of the player engine");
            return;
        }
        player.pause();
    }

    @Override
    public void replay() {
        log.d("replay");
        if (player == null) {
            log.e("Attempt to invoke 'replay()' on null instance of the player engine");
            return;
        }
        player.replay();
    }

    @Override
    public void setVolume(float volume) {
        if (player == null) {
            log.e("Attempt to invoke 'setVolume()' on null instance of the player engine");
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
            log.e("Attempt to invoke 'release()' on null instance of the player engine");
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
            prepare(mediaConfig);
            togglePlayerListeners(true);
        }
    }

    @Override
    public void changeTrack(String uniqueId) {
        if (player == null) {
            log.e("Attempt to invoke 'changeTrack()' on null instance of the player engine");
            return;
        }

        player.changeTrack(uniqueId);
    }

    private boolean isNewEntry(PKMediaConfig mediaConfig) {
        if (this.mediaConfig == null) {
            return true;
        }

        String oldEntryId = this.mediaConfig.getMediaEntry().getId();
        if (oldEntryId == null) {
            return true;
        }
        String newEntryId = mediaConfig.getMediaEntry().getId();
        return !oldEntryId.equals(newEntryId);
    }

    private boolean shouldSwitchBetweenPlayers(PKMediaSource newSource) {

        PKMediaFormat currentMediaFormat = newSource.getMediaFormat();
        if (currentMediaFormat != PKMediaFormat.wvm && player instanceof MediaPlayerWrapper) {
            return true;
        }

        if (currentMediaFormat == PKMediaFormat.wvm && player instanceof ExoPlayerWrapper) {
            return true;
        }

        return false;
    }

    private void removeAllViews() {
        togglePlayerListeners(false);
        rootPlayerView.removeAllViews();
        playerEngineView = null;
    }

    private boolean maybeHandleExceptionLocally(PlayerEvent.ExceptionInfo exceptionInfo) {
        if (exceptionInfo.getErrorCounter() > ALLOWED_ERROR_RETRIES) {
            log.w("Amount of the retries that happened on the same error are exceed the allowed amount of retries. Allowed amount of retries " + ALLOWED_ERROR_RETRIES + " actual amount " + exceptionInfo.getErrorCounter());
            return false;
        }

        if (exceptionInfo.getException() instanceof ExoPlaybackException) {
            ExoPlaybackException exoPlaybackException = (ExoPlaybackException) exceptionInfo.getException();
            if (exoPlaybackException.type == ExoPlaybackException.TYPE_RENDERER) {

                if (exoPlaybackException.getRendererException() instanceof MediaCodec.CryptoException) {
                    ExoPlayerWrapper exoPlayerWrapper = (ExoPlayerWrapper) player;
                    long currentPosition = player.getCurrentPosition();
                    exoPlayerWrapper.savePlayerPosition();

                    exoPlayerWrapper.load(sourceConfig);
                    exoPlayerWrapper.startFrom(currentPosition);
                    return true;
                }
            }
        }
        return false;
    }
}
