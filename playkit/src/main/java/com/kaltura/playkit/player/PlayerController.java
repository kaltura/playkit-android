package com.kaltura.playkit.player;

import android.content.Context;
import android.media.MediaCodec;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.kaltura.playkit.Assert;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.utils.Consts;

/**
 * Created by anton.afanasiev on 01/11/2016.
 */

public class PlayerController implements Player {

    private static final PKLog log = PKLog.get("PlayerController");
    private static final long MILLISECONDS_MULTIPLIER = 1000L;
    private static final int ALLOWED_ERROR_RETRIES = 3;


    private PlayerEngine player;
    private Context context;
    private PlayerView wrapperView;

    private PlayerConfig.Media mediaConfig;
    private boolean wasReleased = false;

    //private ViewGroup playerRootView;
    private PKEvent.Listener eventListener;

    public void setEventListener(PKEvent.Listener eventListener) {
        this.eventListener = eventListener;
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
                        if (exceptionInfo.getException() == null) {
                            return;
                        }

                        //if exception should be handled locally, do not send it to message bus.
                        if (maybeHandleExceptionLocally(exceptionInfo)) {
                            return;
                        }
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

    public PlayerController(Context context, PlayerConfig.Media mediaConfig) {
        this.context = context;
        this.wrapperView = new PlayerView(context) {
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
        this.wrapperView.setLayoutParams(lp);
        this.mediaConfig = mediaConfig;
    }

    private void setVideoSurfaceVisibility(boolean isVisible) {
        View videoSurface = wrapperView.getChildAt(0);
        if (videoSurface != null) {
            if (videoSurface instanceof PlayerView) {
                PlayerView playerView = (PlayerView) wrapperView.getChildAt(0);
                if (playerView != null) {
                    if (isVisible) {
                        playerView.showVideoSurface();
                    } else {
                        playerView.hideVideoSurface();
                    }
                }
            } else {
                String visibilityFunction = "showVideoSurface";
                if (!isVisible) {
                    visibilityFunction = "hideVideoSurface";
                }
                log.e("Error in " + visibilityFunction + " cannot cast to PlayerView,  class = " + videoSurface.getClass().getName());
            }
        }
    }

    public void prepare(@NonNull PlayerConfig.Media mediaConfig) {
        this.mediaConfig = mediaConfig;
        PKMediaSource source = SourceSelector.selectSource(mediaConfig.getMediaEntry());

        if (source == null) {
            log.e("No playable source found for entry");
            return;
        }

        if (source.getMediaFormat() != PKMediaFormat.wvm_widevine) {
            if (player == null) {
                player = new ExoPlayerWrapper(context);
                wrapperView.addView(player.getView());
                togglePlayerListeners(true);
            }
        } else {
            //WVM Player
            return;
        }

        player.load(source);
        startPlaybackFrom(mediaConfig.getStartPosition() * MILLISECONDS_MULTIPLIER);
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

    private void startPlaybackFrom(long startPosition) {
        if (player == null) {
            log.e("Attempt to invoke 'startPlaybackFrom()' on null instance of the player engine");
            return;
        }

        if (startPosition <= mediaConfig.getMediaEntry().getDuration()) {
            if (!wasReleased) {
                togglePlayerListeners(false);
                player.startFrom(startPosition);
                togglePlayerListeners(true);
            }
        } else {
            log.w("The start position is grater then duration of the video! Start position " + startPosition + ", duration " + mediaConfig.getMediaEntry().getDuration());
        }
    }

    public PlayerView getView() {
        return wrapperView;
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
        if (enable) {
            player.setEventListener(eventTrigger);
            player.setStateChangedListener(stateChangedTrigger);
        } else {
            player.setEventListener(null);
            player.setStateChangedListener(null);
        }
    }

    @Override
    public void prepareNext(@NonNull PlayerConfig.Media mediaConfig) {
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
    public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable Object value) {
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
        wasReleased = true;
    }

    @Override
    public void onApplicationResumed() {
        log.d("onApplicationResumed");
        if (wasReleased) {
            player.restore();
            prepare(mediaConfig);
            togglePlayerListeners(true);
            wasReleased = false;
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

    private boolean maybeHandleExceptionLocally(PlayerEvent.ExceptionInfo exceptionInfo) {
        if (exceptionInfo.getErrorCounter() > ALLOWED_ERROR_RETRIES) {
            log.w("Amount of the retries that happened on the same error are exceed the allowed amount of retries. Allowed amount of retries " + ALLOWED_ERROR_RETRIES + " actual amount " +exceptionInfo.getErrorCounter());
            return false;
        }

        if (exceptionInfo.getException() instanceof ExoPlaybackException) {
            ExoPlaybackException exoPlaybackException = (ExoPlaybackException) exceptionInfo.getException();
            if (exoPlaybackException.type == ExoPlaybackException.TYPE_RENDERER) {

                if (exoPlaybackException.getRendererException() instanceof MediaCodec.CryptoException) {
                    ExoPlayerWrapper exoPlayerWrapper = (ExoPlayerWrapper) player;
                    long currentPosition = player.getCurrentPosition();
                    exoPlayerWrapper.savePlayerPosition();
                    PKMediaSource source = SourceSelector.selectSource(mediaConfig.getMediaEntry());

                    if (source == null) {
                        log.e("No playable source found for entry");
                        return false;
                    }
                    exoPlayerWrapper.load(source);
                    exoPlayerWrapper.startFrom(currentPosition);
                    return true;
                }
            }
        }
        return false;
    }
}
