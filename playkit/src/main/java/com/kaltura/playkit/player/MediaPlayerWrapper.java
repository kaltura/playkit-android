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
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.SurfaceHolder;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlaybackInfo;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.drm.WidevineClassicDrm;
import com.kaltura.playkit.player.metadata.PKMetadata;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.PKError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.kaltura.playkit.player.MediaPlayerWrapper.PrepareState.NOT_PREPARED;
import static com.kaltura.playkit.player.MediaPlayerWrapper.PrepareState.PREPARED;
import static com.kaltura.playkit.player.MediaPlayerWrapper.PrepareState.PREPARING;

/**
 * Created by gilad.nadav on 30/12/2016.
 * @hide
 */

class MediaPlayerWrapper implements PlayerEngine, SurfaceHolder.Callback, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener {


    private static final PKLog log = PKLog.get("MediaPlayerWrapper");

    private static final int ILLEGAL_STATEׁ_OPERATION = -38;

    private Context context;
    private MediaPlayer player;
    private MediaPlayerView mediaPlayerView;
    private PKMediaSourceConfig mediaSourceConfig;
    private String assetUri;
    private String licenseUri;

    private WidevineClassicDrm drmClient;
    private PlayerEvent.Type currentEvent;
    private PlayerState currentState = PlayerState.IDLE, previousState;
    private long playerDuration = Consts.TIME_UNSET;
    private long playerPosition;
    private PlayerController.EventListener eventListener;
    private PlayerController.StateChangedListener stateChangedListener;
    private boolean shouldRestorePlayerToPreviousState = false;
    private PrepareState prepareState = NOT_PREPARED;
    private boolean isPlayAfterPrepare = false;
    private boolean isPauseAfterPrepare = false;
    private boolean appInBackground;
    private boolean isFirstPlayback = true;

    MediaPlayerWrapper(Context context) {
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
    public void load(PKMediaSourceConfig mediaSourceConfig) {
        log.d("load");

        if (currentState != null && this.mediaSourceConfig != null && !this.mediaSourceConfig.equals(mediaSourceConfig) && prepareState != PREPARING) {
            player.reset();
            currentState = PlayerState.IDLE;
            prepareState = PrepareState.NOT_PREPARED;
        }

        this.mediaSourceConfig = mediaSourceConfig;

        if ((currentState == null || currentState == PlayerState.IDLE) && prepareState != PREPARING) {
            initializePlayer();
        }
    }

    private void initializePlayer() {
        if (player == null) {
            return;
        }
        currentState = PlayerState.IDLE;
        changeState(PlayerState.IDLE);
        //player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        if (assetUri != null) {
            isFirstPlayback = false;
        }
        assetUri = mediaSourceConfig.getUrl().toString();

        String assetAcquireUri = getWidevineAssetAcquireUri(assetUri);
        String playbackUri     = getWidevineAssetPlaybackUri(assetUri);
        log.d("playback uri = " + playbackUri);
        try {
            mediaPlayerView.getSurfaceHolder().addCallback(this);
            player.setDataSource(playbackUri);
            setPlayerListeners();
        } catch (IOException e) {
            log.e(e.toString());
        }
        if (drmClient.needToAcquireRights(assetAcquireUri)) {
            List<PKDrmParams> drmData = mediaSourceConfig.mediaSource.getDrmData();
            if (drmData != null && !drmData.isEmpty()) {
                licenseUri = drmData.get(0).getLicenseUri();
                drmClient.acquireRights(assetAcquireUri, licenseUri);
            } else {
                log.e("Rights acq required but no DRM Params");
                sendDistinctEvent(PlayerEvent.Type.ERROR);
                return;
            }
        }
        if (!isFirstPlayback) {
            if (prepareState == NOT_PREPARED) {
                changeState(PlayerState.BUFFERING);
                prepareState = PREPARING;
                playerDuration = Consts.TIME_UNSET;
                player.prepareAsync();
            }
        }
    }

    private void setPlayerListeners() {
        // Set OnCompletionListener to notify our callbacks when the video is completed.
        player.setOnCompletionListener(this);
        // Set OnErrorListener to notify our callbacks if the video errors.
        player.setOnErrorListener(this);
        player.setOnBufferingUpdateListener(this);
        player.setOnPreparedListener(this);
    }

    private void sendOnPreparedEvents() {
        sendDistinctEvent(PlayerEvent.Type.LOADED_METADATA);
        sendDistinctEvent(PlayerEvent.Type.DURATION_CHANGE);
        sendDistinctEvent(PlayerEvent.Type.TRACKS_AVAILABLE);
        sendDistinctEvent(PlayerEvent.Type.PLAYBACK_INFO_UPDATED);
        sendDistinctEvent(PlayerEvent.Type.CAN_PLAY);

    }

    private void handleContentCompleted() {
        pause();
        seekTo(playerDuration);
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

        // FIXME: this should only be sent after playback has started
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
            log.w("Attempt to invoke 'replay()' on null instance of the mediaplayer");
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
        if (player == null || !PREPARED.equals(prepareState)) {
            return 0;
        }
        return playerDuration;
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
        if (position > 0) {
            seekTo((int) position);
        }
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
    public PlaybackInfo getPlaybackInfo() {
        return new PlaybackInfo(getWidevineAssetPlaybackUri(assetUri), -1, -1, -1, player.getVideoWidth(), player.getVideoHeight(), false);
    }

    @Override
    public PKError getCurrentError() {
        return null;
    }

    @Override
    public void stop() {
        if (player != null) {
            player.pause();
            player.seekTo(0);
            player.reset();
            sendDistinctEvent(PlayerEvent.Type.STOPPED);
        }
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
    public void onPrepared(MediaPlayer mp) {
        prepareState = PREPARED;
        log.d("onPrepared " + prepareState + " isPlayAfterPrepare = " + isPlayAfterPrepare + " appInBackground = " + appInBackground);
        mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                log.d("onSeekComplete");
                if (getCurrentPosition() < getDuration()) {
                    sendDistinctEvent(PlayerEvent.Type.CAN_PLAY);
                    changeState(PlayerState.READY);
                    if (mp.isPlaying()) {
                        sendDistinctEvent(PlayerEvent.Type.PLAYING);
                    }
                }
            }
        });
        if (appInBackground) {
            return;
        }
        playerDuration = player.getDuration();
        changeState(PlayerState.READY);
        sendOnPreparedEvents();
        if (isPlayAfterPrepare) {
            sendDistinctEvent(PlayerEvent.Type.PLAY);
            play();
            isPlayAfterPrepare = false;
        } else if (isPauseAfterPrepare){
            pause();
            isPauseAfterPrepare = false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        log.d("surfaceCreated state = " + currentState);
        if (player == null) {
            return;
        }

        player.setDisplay(surfaceHolder);

        if (prepareState == NOT_PREPARED) {
            changeState(PlayerState.BUFFERING);
            prepareState = PREPARING;
            playerDuration = Consts.TIME_UNSET;
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
            log.w("Attempt to invoke 'savePlayerPosition()' on null instance of mediaplayer");
            return;
        }
        playerPosition = player.getCurrentPosition();
        log.d("playerPosition = " + playerPosition);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        log.d("onCompletion");
        handleContentCompleted();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        currentState = PlayerState.IDLE;
        changeState(PlayerState.IDLE);
        String errMsg = "onError what = " + what;
        log.e(errMsg);

        if (what == ILLEGAL_STATEׁ_OPERATION) {
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
        return true;
    }

    enum PrepareState {
        NOT_PREPARED,
        PREPARING,
        PREPARED
    }

    @Override
    public List<PKMetadata> getMetadata() {
        return null;
    }
}
