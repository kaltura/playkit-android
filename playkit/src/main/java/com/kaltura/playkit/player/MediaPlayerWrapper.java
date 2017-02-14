

package com.kaltura.playkit.player;

import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.SurfaceHolder;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PlaybackParamsInfo;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.drm.WidevineClassicDrm;
import com.kaltura.playkit.utils.Consts;

import java.io.IOException;
import java.util.ArrayList;

import static com.kaltura.playkit.player.MediaPlayerWrapper.PrepareState.NOT_PREPARED;
import static com.kaltura.playkit.player.MediaPlayerWrapper.PrepareState.PREPARED;
import static com.kaltura.playkit.player.MediaPlayerWrapper.PrepareState.PREPARING;

/**
 * Created by gilad.nadav on 30/12/2016.
 */

public class MediaPlayerWrapper implements PlayerEngine, SurfaceHolder.Callback {

    private static final PKLog log = PKLog.get("MediaPlayerWrapper");

    private static int ILLEGAL_STATEׁ_ORERATION = -38;

    private Context context;
    private MediaPlayer player;
    private MediaPlayerView mediaPlayerView;
    private PKMediaSource mediaSource;
    private String assetUri;

    private String licenseUri;
    private WidevineClassicDrm drmClient;
    private PlayerEvent.Type currentEvent;
    private PlayerState currentState = PlayerState.IDLE, previousState;
    private long playerPosition;
    private long prevDuration = Consts.TIME_UNSET;
    private PlayerController.EventListener eventListener;
    private PlayerController.StateChangedListener stateChangedListener;
    private boolean shouldRestorePlayerToPreviousState = false;
    private PrepareState prepareState = NOT_PREPARED;
    private boolean isPlayAfterPrepare = false;
    private boolean isPauseAfterPrepare = false;
    private boolean appInBackground;

    public MediaPlayerWrapper(Context context) {
        this.context = context;
        player = new MediaPlayer();
        mediaPlayerView = new MediaPlayerView(context);
        initDrmClient();
    }

    private void initDrmClient() {
        drmClient = new WidevineClassicDrm(context);
        drmClient.setEventListener(new WidevineClassicDrm.EventListener() {
            @Override
            public void onError(final DrmErrorEvent event) {
                sendDistinctEvent(PlayerEvent.Type.ERROR);
            }

            @Override
            public void onEvent(DrmEvent event) {
                //Do Nothing
            }
        });
    }

    @Override
    public void load(PKMediaSource mediaSource) {
        log.d("load");
        this.mediaSource = mediaSource;

        if (currentState == null || currentState == PlayerState.IDLE) {
            initializePlayer();
        }
    }

    private void initializePlayer() {
        if (player == null) {
            return;
        }
        currentState = PlayerState.IDLE;
        //player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);

        assetUri = mediaSource.getUrl();
        licenseUri = mediaSource.getDrmData().get(0).getLicenseUri();
        String assetAcquireUri = getWidevineAssetAcquireUri(assetUri);
        try {
            mediaPlayerView.getSurfaceHolder().addCallback(this);
            player.setDataSource(assetUri);
            setPlayerListeners();
            prepareState = PREPARING;
        } catch (IOException e) {
            log.e(e.toString());
        }
        if(drmClient.needToAcquireRights(assetAcquireUri)) {
            drmClient.acquireRights(assetAcquireUri, licenseUri);
        }
    }

    private void setPlayerListeners() {
        // Set OnCompletionListener to notify our callbacks when the video is completed.
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                log.d("onCompletion");
                handleContentCompleted();
            }
        });

        // Set OnErrorListener to notify our callbacks if the video errors.
        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                currentState = PlayerState.IDLE;
                changeState(PlayerState.IDLE);
                String errMsg = "onError what = " + what;
                log.e(errMsg);

                if (what == ILLEGAL_STATEׁ_ORERATION) {
                    release();
                    player.reset();
                    try {
                        player.setDataSource(assetUri);
                    } catch (IOException e) {
                        log.e(e.getMessage());
                        sendDistinctEvent(PlayerEvent.Type.ERROR);
                        return true;
                    }
                    restore();
                    return true;
                }
                sendDistinctEvent(PlayerEvent.Type.ERROR);
//                if(what == MediaPlayer.MEDIA_ERROR_SERVER_DIED || what == MediaPlayer.MEDIA_ERROR_UNKNOWN || what == MediaPlayer.MEDIA_ERROR_IO) {
//
//                }
                return true;
            }
        });

        player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                //Do Nothing;
            }
        });

        player.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {
                if (getCurrentPosition() < getDuration()) {
                    sendDistinctEvent(PlayerEvent.Type.CAN_PLAY);
                    changeState(PlayerState.READY);
                    if (mediaPlayer.isPlaying()) {
                        sendDistinctEvent(PlayerEvent.Type.PLAYING);
                    }
                }
            }
        });

        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                log.d("onPrepared " + prepareState + " isPlayAfterPrepare = " + isPlayAfterPrepare + " appInBackground = " + appInBackground);
                if (appInBackground) {
                    return;
                }
                prepareState = PREPARED;
                changeState(PlayerState.READY);
                if (isPlayAfterPrepare) {
                    sendDistinctEvent(PlayerEvent.Type.PLAY);
                    sendOnPreparedEvents();
                    play();
                    isPlayAfterPrepare = false;
                } else if (isPauseAfterPrepare){
                    sendOnPreparedEvents();
                    pause();
                    isPauseAfterPrepare = false;
                }

            }
        });
    }

    private void sendOnPreparedEvents() {
        sendDistinctEvent(PlayerEvent.Type.LOADED_METADATA);
        sendDistinctEvent(PlayerEvent.Type.CAN_PLAY);
        sendDistinctEvent(PlayerEvent.Type.TRACKS_AVAILABLE);
    }

    private void handleContentCompleted() {
        pause();
        seekTo(player.getDuration());       // TODO: why?
        currentState = PlayerState.IDLE;
        changeState(PlayerState.IDLE);
        sendDistinctEvent(PlayerEvent.Type.ENDED);
    }

    @Override
    public PlayerView getView() {
        log.d("getView ");
        return mediaPlayerView;
    }

    @Override
    public void play() {
        log.d("play prepareState = " + prepareState.name());
        if (!PREPARED.equals(prepareState)) {
            isPlayAfterPrepare = true;
            if (isPauseAfterPrepare) {
                isPauseAfterPrepare = false;
            }
            return;
        }
        player.start();
        sendDistinctEvent(PlayerEvent.Type.PLAY);

        // FIXME: this should only be sent after playback as started
        sendDistinctEvent(PlayerEvent.Type.PLAYING);
    }



    @Override
    public void pause() {
        log.d("pause ");
        if (!PREPARED.equals(prepareState)) {
            isPauseAfterPrepare = true;
            if (isPlayAfterPrepare) {
                isPlayAfterPrepare = false;
            }
            return;
        }
        if(player.isPlaying()) {
            player.pause();
        }
        sendDistinctEvent(PlayerEvent.Type.PAUSE);
    }

    @Override
    public void replay() {
        if (!PREPARED.equals(prepareState)) {
            return;
        }
        log.d("replay ");

        if (player == null) {
            log.e("Attempt to invoke 'replay()' on null instance of the exoplayer");
            return;
        }
        seekTo(0);
        player.start();
        sendDistinctEvent(PlayerEvent.Type.REPLAY);
    }

    @Override
    public long getCurrentPosition() {
        log.d("getCurrentPosition");
        if (player == null || !PREPARED.equals(prepareState)) {
            return 0;
        }

        return player.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        log.d("getDuration ");
        if (player == null || !PREPARED.equals(prepareState)) {
            return 0;
        }
        long currentDuration;
        currentDuration = player == null ? Consts.TIME_UNSET : player.getDuration();
        if (prevDuration != currentDuration) {
            sendDistinctEvent(PlayerEvent.Type.DURATION_CHANGE);
        }
        prevDuration = currentDuration;
        return prevDuration;

    }

    @Override
    public long getBufferedPosition() {
        return 0;
    }

    @Override
    public float getVolume() {
        return 0;
    }

    @Override
    public PKTracks getPKTracks() {
        return new PKTracks(new ArrayList<VideoTrack>(), new ArrayList<AudioTrack>(), new ArrayList<TextTrack>(),
                0, 0, 0);
    }

    @Override
    public void changeTrack(String uniqueId) {
        // Do Nothing
    }

    @Override
    public void seekTo(long position) {
        log.d("seekTo " + position);
        if (player == null || !PREPARED.equals(prepareState)) {
            return;
        }

        player.seekTo((int)position);
        changeState(PlayerState.BUFFERING);
        sendDistinctEvent(PlayerEvent.Type.SEEKING);
        sendDistinctEvent(PlayerEvent.Type.SEEKED);
    }

    @Override
    public void startFrom(long position) {
        if (shouldRestorePlayerToPreviousState) {
            log.i("Restoring player from previous known position. So skip this block.");
            shouldRestorePlayerToPreviousState = false;
            return;
        }

        log.d("startFrom " + position);
        seekTo((int)position);
    }

    @Override
    public void setVolume(float volume) {
        // Do Nothing
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public void setEventListener(PlayerController.EventListener eventTrigger) {
        this.eventListener = eventTrigger;
    }

    @Override
    public void setStateChangedListener(PlayerController.StateChangedListener stateChangedTrigger) {
        this.stateChangedListener = stateChangedTrigger;
    }

    @Override
    public void release() {
        log.d("release");
        appInBackground = true;
        if (player != null && prepareState == PREPARED) {
            savePlayerPosition();
            pause();
            shouldRestorePlayerToPreviousState = true;
        }
    }

    @Override
    public void restore() {
        log.d("restore prepareState = " + prepareState.name());
        appInBackground = false;
        if (player != null && prepareState == PREPARED) {
            play();
            if (playerPosition != 0) {
                seekTo(playerPosition);
                shouldRestorePlayerToPreviousState = false;

            }
            pause();
        } else {
            destroy();
            log.e("Error restore while player is not prepared");
            sendDistinctEvent(PlayerEvent.Type.ERROR);
        }
    }

    @Override
    public void destroy() {
        log.d("destroy");
        if (player != null) {
            player.release();
            player = null;
        }
        mediaPlayerView = null;
        eventListener = null;
        stateChangedListener = null;
        currentState = PlayerState.IDLE;
        previousState = null;
        playerPosition = 0;
    }

    @Override
    public PlaybackParamsInfo getPlaybackParamsInfo() {
        return null;
    }

    @Override
    public PlayerEvent.ExceptionInfo getCurrentException() {
        return null;
    }

    public static String getWidevineAssetPlaybackUri(String assetUri) {
        String assetUriForPlayback = assetUri;
        if (assetUri.startsWith("file:")) {
            assetUriForPlayback = Uri.parse(assetUri).getPath();
        } else if (assetUri.startsWith("http:")) {
            assetUriForPlayback = assetUri.replaceFirst("^http:", "widevine:");
        }
        return assetUriForPlayback;
    }

    // Convert file:///local/path/a.wvm to /local/path/a.wvm
    // Convert widevine://example.com/path/a.wvm to http://example.com/path/a.wvm
    // Everything else remains the same.
    public static String getWidevineAssetAcquireUri(String assetUri) {
        String assetAcquireUriForPlayback = assetUri;
        if (assetUri.startsWith("file:")) {
            assetAcquireUriForPlayback = Uri.parse(assetUri).getPath();
        } else if (assetUri.startsWith("widevine:")) {
            assetAcquireUriForPlayback = assetUri.replaceFirst("widevine", "http");
        }
        return assetAcquireUriForPlayback;
    }

    private void changeState(PlayerState newState) {
        previousState = currentState;
        if (newState.equals(currentState)) {
            return;
        }
        this.currentState = newState;
        if (stateChangedListener != null) {
            stateChangedListener.onStateChanged(previousState, currentState);
        }
    }

    private void sendDistinctEvent(PlayerEvent.Type newEvent) {
        if (newEvent.equals(currentEvent)) {
            return;
        }
        sendEvent(newEvent);
    }


    private void sendEvent(PlayerEvent.Type event) {
        if (shouldRestorePlayerToPreviousState) {
            log.i("Trying to send event " + event.name() + ". Should be blocked from sending now, because the player is restoring to the previous state.");
            return;
        }
        currentEvent = event;
        if (eventListener != null) {
            log.i("Event sent: " + event.name());
            eventListener.onEvent(currentEvent);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        log.d("surfaceCreated state = " + currentState);
        player.setDisplay(surfaceHolder);

        if (!PREPARED.equals(prepareState)) {
            changeState(PlayerState.BUFFERING);
            player.prepareAsync();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        //Do Nothing;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        //Do Nothing;
    }

    private void savePlayerPosition() {
        if (player == null) {
            log.e("Attempt to invoke 'savePlayerPosition()' on null instance");
            return;
        }
        playerPosition = player.getCurrentPosition();
        log.d("playerPosition = " + playerPosition);
    }

    enum PrepareState {
        NOT_PREPARED,
        PREPARING,
        PREPARED
    }
}
