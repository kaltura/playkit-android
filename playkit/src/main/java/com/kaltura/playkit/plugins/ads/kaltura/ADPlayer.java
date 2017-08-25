package com.kaltura.playkit.plugins.ads.kaltura;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.kaltura.admanager.AdPlayer;
import com.kaltura.admanager.model.MediaFile;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

public class ADPlayer implements AdPlayer {
    private static final String TAG = ADPlayer.class.getSimpleName();

    private Context context;
    private Player player;
    private PKMediaConfig mediaConfig;
    private final List<Listener> adPlayerCallbacks = new ArrayList<Listener>(3);
    private PlaybackState playbackState;

    private enum PlaybackState {
        STOPPED, PAUSED, PLAYING
    }

    public ADPlayer(Context context) {
        this.context = context;
        PKPluginConfigs pluginConfigs = new PKPluginConfigs();
        this.player =  PlayKitManager.loadPlayer(context, pluginConfigs);

        subscribeToPlayerStateChanges();
        subscribeToPlayerEvents();
    }

    @Override
    public void setMedia(MediaFile mediaFile, long adDuration) {
        playbackState = PlaybackState.STOPPED;
        mediaConfig = new PKMediaConfig();

        PKMediaEntry mediaEntry = new PKMediaEntry();
        mediaEntry.setId(mediaFile.getId());
        mediaEntry.setDuration(adDuration);
        mediaEntry.setMediaType(PKMediaEntry.MediaEntryType.Vod);
        List<PKMediaSource> mediaSources = new ArrayList<>();
        PKMediaSource mediaSource = new PKMediaSource();
        mediaSource.setId(mediaFile.getId());
        mediaSource.setUrl(mediaFile.getUrl());
        PKMediaFormat pkMediaFormat = PKMediaFormat.valueOfUrl(mediaFile.getUrl());
        mediaSource.setMediaFormat(pkMediaFormat);
        mediaSources.add(mediaSource);
        mediaEntry.setSources(mediaSources);
        mediaConfig.setMediaEntry(mediaEntry);

    }

    @Override
    public void preload() {
        player.prepare(mediaConfig);
    }

    @Override
    public void play() {
        player.play();
    }

    @Override
    public void pause() {
        player.pause();
    }

    @Override
    public void stop() {
        player.stop();
    }

    @Override
    public void destroy() {
        player.destroy();
    }

    @Override
    public float getDurationSec() {
        return playbackState == PlaybackState.STOPPED ? 0 : player.getDuration();
    }

    @Override
    public float getPositionSec() {
        return player.getCurrentPosition();
    }

    @Override
    public View getView() {
        return player.getView();
    }

    @Override
    public void addListener(Listener listener) {
        adPlayerCallbacks.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        adPlayerCallbacks.remove(listener);
    }

    @Override
    public void onApplicationPaused() {
        if (player != null) {
            player.onApplicationPaused();
        }
    }

    @Override
    public void onApplicationResumed() {
        if (player != null) {
            player.onApplicationResumed();
        }
    }


    /**
     * Will subscribe to the changes in the player states.
     */
    private void subscribeToPlayerStateChanges() {
        //Add event listener to the player.
        player.addStateChangeListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {

                //Cast received event to PlayerEvent.StateChanged.
                PlayerEvent.StateChanged stateChanged = (PlayerEvent.StateChanged) event;

                //Switch on the new state that is received.
                switch (stateChanged.newState) {

                    //Player went to the Idle state.
                    case IDLE:
                        //Print to log.
                        Log.d(TAG, "StateChanged: IDLE.");
                        break;
                    //The player is in Loading state.
                    case LOADING:
                        //Print to log.
                        Log.d(TAG, "StateChanged: LOADING.");
                        break;
                    //The player is ready for playback.
                    case READY:
                        //Print to log.
                        Log.d(TAG, "StateChanged: READY.");
                        for (Listener callback : adPlayerCallbacks) {
                            callback.onBufferEnd();
                        }
                        break;
                    //Player is buffering now.
                    case BUFFERING:
                        //Print to log.
                        Log.d(TAG, "StateChanged: BUFFERING.");
                        for (Listener callback : adPlayerCallbacks) {
                            callback.onBufferStart();
                        }
                        break;
                }
            }
        });
    }

    /**
     * Will subscribe to the player events. The main difference between
     * player state changes and player events, is that events are notify us
     * about playback events like PLAY, PAUSE, TRACKS_AVAILABLE, SEEKING etc.
     * The player state changed events, notify us about more major changes in
     * his states. Like IDLE, LOADING, READY and BUFFERING.
     * For simplicity, in this example we will show subscription to the couple of events.
     * For the full list of events you can check our documentation.
     * !!!Note, we will receive only events, we subscribed to.
     */
    private void subscribeToPlayerEvents() {

        //Add event listener. Note, that it have two parameters.
        // 1. PKEvent.Listener itself.
        // 2. Array of events you want to listen to.
        player.addEventListener(new PKEvent.Listener() {
                                    @Override
                                    public void onEvent(PKEvent event) {
                                        if ( event.eventType() != PlayerEvent.Type.PLAYHEAD_UPDATED) {
                                            Log.d(TAG, "XXX Ad Player Event => " + event.eventType());
                                        }
                                        Enum receivedEventType = event.eventType();
                                        if (event instanceof PlayerEvent) {
                                            switch (((PlayerEvent) event).type) {
                                                case PLAYHEAD_UPDATED:
                                                    PlayerEvent.PlayheadUpdated playheadUpdated = (PlayerEvent.PlayheadUpdated) event;
                                                    for (Listener callback : adPlayerCallbacks) {
                                                        callback.onAdPlayHeadUpdate(playheadUpdated.position, playheadUpdated.duration);
                                                    }
                                                    break;
                                                case CAN_PLAY:
                                                    for (Listener callback : adPlayerCallbacks) {
                                                        callback.onPlayerReady();
                                                    }
                                                    break;
                                                case PLAY:
                                                    switch (playbackState) {
                                                        case STOPPED:
                                                            for (Listener callback : adPlayerCallbacks) {
                                                                callback.onPlay();
                                                            }
                                                            break;
                                                        case PAUSED:
                                                            for (Listener callback : adPlayerCallbacks) {
                                                                callback.onResume();
                                                            }
                                                            break;
                                                        default:
                                                            break;
                                                    }
                                                    playbackState = PlaybackState.PLAYING;
                                                    break;
                                                case PLAYING:
                                                    for (Listener callback : adPlayerCallbacks) {
                                                        callback.onPlaying();
                                                    }
                                                    playbackState = PlaybackState.PLAYING;
                                                    break;
                                                case PAUSE:
                                                    for (Listener callback : adPlayerCallbacks) {
                                                        callback.onPause();
                                                    }
                                                    playbackState = PlaybackState.PAUSED;
                                                    //stopTimer();
                                                    break;
                                                case ENDED:
                                                    for (Listener callback : adPlayerCallbacks) {
                                                        callback.onEnded();
                                                    }

                                                    playbackState = PlaybackState.STOPPED;
                                                    break;
                                                case TRACKS_AVAILABLE:
                                                    break;
                                                case STOPPED:
                                                    playbackState = PlaybackState.STOPPED;
                                                    break;
                                                case ERROR:
                                                    for (Listener callback : adPlayerCallbacks) {
                                                        callback.onError();
                                                    }
                                                    playbackState = PlaybackState.STOPPED;
//                                                    // TODO check for valid ad in pod and play / play content.
//                                                    // TODO Do the same for skip

                                                     //stopTimer();
                                                    break;
                                            }
                                        }

                                    }

                                }, PlayerEvent.Type.PLAY,  PlayerEvent.Type.PAUSE, PlayerEvent.Type.CAN_PLAY, PlayerEvent.Type.PLAYING,
                                   PlayerEvent.Type.ENDED, PlayerEvent.Type.TRACKS_AVAILABLE, PlayerEvent.Type.ERROR, PlayerEvent.Type.STOPPED,
                                   PlayerEvent.Type.PLAYHEAD_UPDATED);
    }
}
