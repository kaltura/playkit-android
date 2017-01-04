package com.kaltura.playkit.player;

import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.SurfaceHolder;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKTracks;
import com.kaltura.playkit.PlaybackParamsInfo;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.drm.WidevineClassicDrm;

import java.io.IOException;

import static com.kaltura.playkit.player.MediaPlayerWrapper.PrepareState.Prepared;


/**
 * Created by gilad.nadav on 30/12/2016.
 */

public class MediaPlayerWrapper implements PlayerEngine,  SurfaceHolder.Callback {

    private static final PKLog log = PKLog.get("MediaPlayerWrapper");

    Context context;
    private MediaPlayer player;
    private MediaPlayerView mediaPlayerView;
    private PKMediaSource mediaSource;
    private String assetUri;
    private String licenseUri;
    private WidevineClassicDrm drmClient;
    private PlayerEvent.Type currentEvent;
    private PlayerState currentState = PlayerState.IDLE, previousState;
    private long playerPosition;
    private boolean isSeeking = false;
    private boolean shouldResetPlayerPosition;
    private Uri lastPlayedSource;
    private PlayerController.EventListener eventListener;
    private PlayerController.StateChangedListener stateChangedListener;
    private PrepareState prepareState = PrepareState.NotPrepared;
    private boolean isPlayAfterPrepare = false;


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
        //if (player == null) {
        initializePlayer();
        //}
    }

    private void initializePlayer() {
        currentState = PlayerState.IDLE;
        //player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);

        // Set OnCompletionListener to notify our callbacks when the video is completed.
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                // Reset the MediaPlayer.
                // This prevents a race condition which occasionally results in the media
                // player crashing when switching between videos.

                //disablePlaybackControls();
                mediaPlayer.reset();
                //mediaPlayer.setDisplay(getHolder());
                //enablePlaybackControls();
                currentState = PlayerState.IDLE;

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

            }
        });

        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                log.d("XXX onPrepared " + prepareState + " isPlayAfterPrepare = " + isPlayAfterPrepare);

                prepareState = Prepared;
                if (isPlayAfterPrepare) {
                    isPlayAfterPrepare = false;
                    play();
                }

            }
        });


        assetUri = mediaSource.getUrl();
        licenseUri = mediaSource.getDrmData().get(0).getLicenseUri();
        String assetAcquireUri = getWidevineAssetAcquireUri(assetUri);
        try {
           // player.setDataSource(context,Uri.parse("https://cdnapisec.kaltura.com/p/1982551/sp/198255100/playManifest/entryId/0_lcinsq2i/format/applehttp/tags/iphonenew/protocol/https/f/a.m3u8"));
            player.setDataSource(assetUri);
            prepareState = PrepareState.Preparing;
            mediaPlayerView.getSurfaceHolder().addCallback(this);
        } catch (IOException e) {
            log.e(e.toString());
        }
        if(drmClient.needToAcquireRights(assetAcquireUri)) {
           drmClient.acquireRights(assetAcquireUri, licenseUri);
        }
    }

    @Override
    public PlayerView getView() {
        log.d("XXX getView ");
        return mediaPlayerView;
    }

    @Override
    public void play() {
        log.d("XXX play prepareState = " + prepareState.name());
        if (!Prepared.equals(prepareState)) {
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
    }



    @Override
    public void pause() {
        log.d("XXX pause ");
        if (!Prepared.equals(prepareState)) {
            return;
        }
        if(player.isPlaying()) {
            player.pause();
        }
//        for (PlayerCallback callback : mVideoPlayerCallbacks) {
//            callback.onPause();
//        }
    }

    @Override
    public void replay() {
        if (!Prepared.equals(prepareState)) {
            return;
        }
        log.d("XXX replay ");

        if (player == null) {
            log.e("Attempt to invoke 'replay()' on null instance of the exoplayer");
            return;
        }
        isSeeking = false;
        player.seekTo(0);
        player.start();
        sendDistinctEvent(PlayerEvent.Type.REPLAY);
    }

    @Override
    public long getCurrentPosition() {
        log.d("XXX getCurrentPosition ");
        return currentState == PlayerState.IDLE ? 0 : player.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        log.d("XXX getDuration ");
        return currentState == PlayerState.IDLE ? 0 : player.getDuration();

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
        return null;
    }

    @Override
    public void changeTrack(String uniqueId) {

    }

    @Override
    public void seekTo(long position) {
        log.d("XXX seekTo " + position);
        if (!Prepared.equals(prepareState)) {
            return;
        }
        player.seekTo((int)position);
        sendDistinctEvent(PlayerEvent.Type.SEEKING);
        sendDistinctEvent(PlayerEvent.Type.SEEKED);
    }

    @Override
    public void startFrom(long position) {
        log.d("XXX startFrom " + position);
        if (!Prepared.equals(prepareState)) {
            return;
        }
        player.seekTo((int)position);
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
        player.release();
        player = null;
    }

    @Override
    public void restore() {
        log.d("resume");
        initializePlayer();
        if (shouldResetPlayerPosition) {
            player.seekTo((int) playerPosition);
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public PlaybackParamsInfo getPlaybackParamsInfo() {
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
        currentEvent = event;
        if (eventListener != null) {
            eventListener.onEvent(currentEvent);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //SurfaceHolder playerurfaceHolder = mediaPlayerView.getSurfaceHolder();
        player.setDisplay(surfaceHolder);
        try {
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.d("XXXXXY surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    enum PrepareState {
        NotPrepared,
        Preparing,
        Prepared
    }
}