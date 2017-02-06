

package com.kaltura.playkit.player;

import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
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

import static com.kaltura.playkit.player.MediaPlayerWrapper.PrepareState.PREPARED;

/**
 * Created by gilad.nadav on 30/12/2016.
 */

public class MediaPlayerWrapper implements PlayerEngine,  SurfaceHolder.Callback {

    private static final PKLog log = PKLog.get("MediaPlayerWrapper");

    private static final long PLAYHEAD_UPDATE_INTERVAL = 200;

    Context context;
    private MediaPlayer player;
    private MediaPlayerView mediaPlayerView;
    private PKMediaSource mediaSource;
    private String assetUri;
    private String licenseUri;
    private WidevineClassicDrm drmClient;
    private PlayerEvent.Type currentEvent;
    private PlayerState currentState = PlayerState.IDLE, previousState;
    @Nullable
    private PlayheadTracker mPlayheadTracker;
    private long playerPosition;
    private long prevDuration = Consts.TIME_UNSET;
    private boolean isSeeking = false;
    private boolean shouldResetPlayerPosition;
    private Uri lastPlayedSource;
    private PlayerController.EventListener eventListener;
    private PlayerController.StateChangedListener stateChangedListener;
    private boolean shouldRestorePlayerToPreviousState = false;
    private PrepareState prepareState = PrepareState.NOT_PREPARED;
    private boolean isPlayAfterPrepare = false;
    private Exception currentException = null;


    public MediaPlayerWrapper(Context context) {
        this.context = context;
        player = new MediaPlayer();
        mediaPlayerView = new MediaPlayerView(context);

        drmClient = new WidevineClassicDrm(context);

        drmClient.setEventListener(new WidevineClassicDrm.EventListener() {
            @Override
            public void onError(final DrmErrorEvent event) {

            }

            @Override
            public void onEvent(DrmEvent event) {

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
        currentState = PlayerState.IDLE;
        //player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);

        // Set OnCompletionListener to notify our callbacks when the video is completed.
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                log.d("XXX onCompletion");

                // Reset the MediaPlayer.
                // This prevents a race condition which occasionally results in the media
                // player crashing when switching between videos.

                //disablePlaybackControls();
                //mediaPlayer.reset();
                //mediaPlayer.setDisplay(getHolder());
                //enablePlaybackControls();
                handleContentCompleted();

                //for (PlayerCallback callback : mVideoPlayerCallbacks) {
                //  callback.onCompleted();
                //}
            }
        });

        // Set OnErrorListener to notify our callbacks if the video errors.
        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                currentState = PlayerState.IDLE;
                changeState(PlayerState.IDLE);


                //for (PlayerCallback callback : mVideoPlayerCallbacks) {
                //    callback.onError();
                //}

                // Returning true signals to MediaPlayer that we handled the error. This will
                // prevent the completion handler from being called.


                String errMsg = "onError what = " + what;
                log.e(errMsg);
                if (what == -38) {
                    return true;
                }
                if(what == MediaPlayer.MEDIA_ERROR_SERVER_DIED || what == MediaPlayer.MEDIA_ERROR_UNKNOWN || what == MediaPlayer.MEDIA_ERROR_IO) {

                }
                return true;
            }
        });

        player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

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
                log.d("XXX onPrepared " + prepareState + " isPlayAfterPrepare = " + isPlayAfterPrepare);

                prepareState = PREPARED;
                sendDistinctEvent(PlayerEvent.Type.CAN_PLAY);
                changeState(PlayerState.READY);
                if (isPlayAfterPrepare) {
                    play();
                    isPlayAfterPrepare = false;
                }

            }
        });


        assetUri = mediaSource.getUrl();
        licenseUri = mediaSource.getDrmData().get(0).getLicenseUri();
        String assetAcquireUri = getWidevineAssetAcquireUri(assetUri);
        try {
            // player.setDataSource(context,Uri.parse("https://cdnapisec.kaltura.com/p/1982551/sp/198255100/playManifest/entryId/0_lcinsq2i/format/applehttp/tags/iphonenew/protocol/https/f/a.m3u8"));
            player.setDataSource(assetUri);
            prepareState = PrepareState.PREPARING;
            mediaPlayerView.getSurfaceHolder().addCallback(this);
        } catch (IOException e) {
            log.e(e.toString());
        }
        if(drmClient.needToAcquireRights(assetAcquireUri)) {
            drmClient.acquireRights(assetAcquireUri, licenseUri);
        }
    }

    private void handleContentCompleted() {
        pause();
        seekTo(player.getDuration());
        stopPlayheadTracker();
        currentState = PlayerState.IDLE;
        changeState(PlayerState.IDLE);
        sendDistinctEvent(PlayerEvent.Type.ENDED);
    }

    @Override
    public PlayerView getView() {
        log.d("XXX getView ");
        return mediaPlayerView;
    }

    @Override
    public void play() {
        log.d("XXX play prepareState = " + prepareState.name());
        if (!PREPARED.equals(prepareState)) {
            isPlayAfterPrepare = true;
            return;
        }
        player.start();
        if  (currentState == PlayerState.IDLE ) {
//                for (PlayerCallback callback : mVideoPlayerCallbacks) {
//                    callback.onPlay();
//                }
        }
        if (currentEvent == PlayerEvent.Type.PAUSE) {
//                for (PlayerCallback callback : mVideoPlayerCallbacks) {
//                    callback.onResume();
//                }
        }

        // Already playing; do nothing.
        currentEvent = PlayerEvent.Type.PLAY;
        sendDistinctEvent(currentEvent);

        if (mPlayheadTracker == null) {
            mPlayheadTracker = new PlayheadTracker();
        }
        mPlayheadTracker.start();
        if (previousState.equals(PlayerState.READY)) {
            sendDistinctEvent(PlayerEvent.Type.PLAYING);
        }
        sendDistinctEvent(PlayerEvent.Type.PLAY);



    }



    @Override
    public void pause() {
        log.d("XXX pause ");
        if (!PREPARED.equals(prepareState)) {
            return;
        }
        if(player.isPlaying()) {
            player.pause();
        }
        sendDistinctEvent(PlayerEvent.Type.PAUSE);

//        for (PlayerCallback callback : mVideoPlayerCallbacks) {
//            callback.onPause();
//        }
    }

    @Override
    public void replay() {
        if (!PREPARED.equals(prepareState)) {
            return;
        }
        log.d("XXX replay ");

        if (player == null) {
            log.e("Attempt to invoke 'replay()' on null instance of the exoplayer");
            return;
        }
        isSeeking = false;
        seekTo(0);
        player.start();
        sendDistinctEvent(PlayerEvent.Type.REPLAY);
    }

    @Override
    public long getCurrentPosition() {
        log.d("XXX getCurrentPosition ");
        if (player == null || !PREPARED.equals(prepareState)) {
            return 0;
        }

        return player.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        log.d("XXX getDuration ");
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

    }

    @Override
    public void seekTo(long position) {
        log.d("XXX seekTo " + position);
        if (!PREPARED.equals(prepareState)) {
            return;
        }
        if (player == null) {
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
            log.i("Restoring player from previous known state. So skip this block.");
            shouldRestorePlayerToPreviousState = false;
            return;
        }

        log.d("XXX startFrom " + position);
        seekTo((int)position);
    }

    @Override
    public void setVolume(float volume) {

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
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void suspend() {
        log.d("suspend");
        if (player != null) {
            savePlayerPosition();
            stopPlayheadTracker();
            pause();
            shouldRestorePlayerToPreviousState = true;
        }
    }

    @Override
    public void restore() {
        log.d("restore");
        if (player != null) {
            play();
            seekTo(playerPosition);
        }
//        if (player != null) {
//            initializePlayer();
//            if (shouldResetPlayerPosition) {
//                seekTo((int) playerPosition);
//            }
//        }
    }

    @Override
    public void destroy() {

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
        if (assetUri.startsWith("file:")) {
            assetUri = Uri.parse(assetUri).getPath();
        } else if (assetUri.startsWith("http:")) {
            assetUri = assetUri.replaceFirst("^http:", "widevine:");
        }
        return assetUri;
    }

    // Convert file:///local/path/a.wvm to /local/path/a.wvm
    // Convert widevine://example.com/path/a.wvm to http://example.com/path/a.wvm
    // Everything else remains the same.
    public static String getWidevineAssetAcquireUri(String assetUri) {
        if (assetUri.startsWith("file:")) {
            assetUri = Uri.parse(assetUri).getPath();
        } else if (assetUri.startsWith("widevine:")) {
            assetUri = assetUri.replaceFirst("widevine", "http");
        }
        return assetUri;
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
            eventListener.onEvent(currentEvent);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        log.d("XXXXXY surfaceCreated state = " + currentState);

        //SurfaceHolder playerurfaceHolder = mediaPlayerView.getSurfaceHolder();
        player.setDisplay(surfaceHolder);
        //try {
        //player.prepare();
        if (!PREPARED.equals(prepareState)) {
            player.prepareAsync();
        }
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    enum PrepareState {
        NOT_PREPARED,
        PREPARING,
        PREPARED
    }

    ////////////////////////

    private void stopPlayheadTracker() {
        if (mPlayheadTracker != null) {
            playerPosition = (int) mPlayheadTracker.getPlaybackTime() * 1000;
            mPlayheadTracker.stop();
            mPlayheadTracker = null;
        }
    }

    class PlayheadTracker {
        Handler mHandler;
        float playbackTime;
        Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (player != null && player.getCurrentPosition() == player.getDuration()){
                        log.d("--- Video onCompletion ---");
                        handleContentCompleted();
                        return;
                    }

                    if (player != null && player.isPlaying()) {
                        int currPos = player.getCurrentPosition();
                        log.d("progress status = " + currPos + "/" + player.getDuration());
                        if (currPos > player.getDuration()) {
                            playbackTime = player.getDuration() / 1000f;
                        } else {
                            playbackTime = currPos / 1000f;
                        }
                    }

                } catch (IllegalStateException e) {
                    String errMsg = "Player Error ";
                    log.e(errMsg + e.getMessage());

                }
                if (mHandler != null) {
                    mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
                }
            }
        };

        float getPlaybackTime() {
            return playbackTime;
        }

        void start() {
            if (mHandler == null) {
                mHandler = new Handler(Looper.getMainLooper());
                mHandler.postDelayed(mRunnable, PLAYHEAD_UPDATE_INTERVAL);
            } else {
                log.d("Tracker is already started");
            }
        }

        void stop() {
            if (mHandler != null) {
                mHandler.removeCallbacks(mRunnable);
                mHandler = null;
            } else {
                log.d("Tracker is not started, nothing to stop");
            }
        }
    }

    void savePlayerPosition() {
        if (player == null) {
            log.e("Attempt to invoke 'savePlayerPosition()' on null instance");
            return;
        }
        currentException = null;
        playerPosition = player.getCurrentPosition();
        log.e("XXX playerPosition = " + playerPosition);

    }
}