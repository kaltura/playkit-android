package com.kaltura.playkit.player;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.kaltura.playkit.Assert;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
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


    private PlayerEngine player;
    private Context context;

    private PlayerConfig.Media mediaConfig;
    private boolean wasReleased = false;

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
                    case PLAYBACK_PARAMS:
                        event = new PlayerEvent.PlaybackParams(player.getPlaybackParamsInfo());
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

    public PlayerController(Context context, PlayerConfig.Media mediaConfig){
        this.context = context;
        this.mediaConfig = mediaConfig;
        player = new ExoPlayerWrapper(context);
        togglePlayerListeners(true);
    }

    public void prepare(@NonNull PlayerConfig.Media mediaConfig) {

        PKMediaSource source = SourceSelector.selectSource(mediaConfig.getMediaEntry());

        player.load(source);
        long startPosition = mediaConfig.getStartPosition() * MILLISECONDS_MULTIPLIER;
        if(startPosition <= player.getDuration()){
            startPlaybackFrom(startPosition);
        }else{
            log.w("The start position is grater then duration of the video!");
        }
    }

    @Override
    public void destroy() {
        log.e("destroy");
        if(player != null){
            player.destroy();
            togglePlayerListeners(false);
        }
        player = null;
        mediaConfig = null;
        eventListener = null;
    }

    private void startPlaybackFrom(long startPosition) {
        if(player == null){
            log.e("Attempt to invoke 'startPlaybackFrom()' on null instance of the player engine");
            return;
        }
        if(!wasReleased){
            togglePlayerListeners(false);
            player.startFrom(startPosition);
            togglePlayerListeners(true);
        }
    }

    public View getView() {
        if(player == null){
            return null;
        }
        return player.getView();
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
        if(player == null){
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
        if(player == null){
            log.e("Attempt to invoke 'play()' on null instance of the player engine");
            return;
        }
        player.play();
    }

    public void pause() {
        log.d("pause");
        if(player == null){
            log.e("Attempt to invoke 'pause()' on null instance of the player engine");
            return;
        }
        player.pause();
    }

    @Override
    public void replay() {
        log.d("replay");
        if(player == null){
            log.e("Attempt to invoke 'replay()' on null instance of the player engine");
            return;
        }
        player.replay();
    }

    @Override
    public void setVolume(float volume) {
        if(player == null){
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
        if(enable){
            player.setEventListener(eventTrigger);
            player.setStateChangedListener(stateChangedTrigger);
        }else {
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
        if(player == null){
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
        if(wasReleased){
            player.restore();
            prepare(mediaConfig);
            togglePlayerListeners(true);
            wasReleased = false;
        }
    }

    @Override
    public void changeTrack(String uniqueId) {
        if(player == null){
            log.e("Attempt to invoke 'changeTrack()' on null instance of the player engine");
            return;
        }

        player.changeTrack(uniqueId);
    }

    @Override
    public boolean isAutoPlay() {
        return mediaConfig.isAutoPlay();
    }
}
