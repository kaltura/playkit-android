package com.kaltura.playkit.player;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by anton.afanasiev on 01/11/2016.
 */

public class PlayerController implements Player {

    private static final String TAG = PlayerController.class.getSimpleName();

    private PlayerEngine player;
    private Context context;

    private List<PlayerEvent.Listener> eventListeners = new ArrayList<>();
    private List<PlayerState.Listener> stateChangeListeners = new ArrayList<>();

    private PlayerConfig.Media mediaConfig;



    public interface EventListener {
        void onEvent(PlayerEvent event);
    }

    public interface StateChangedListener {
        void onStateChanged(PlayerState state);
    }

    private EventListener eventTrigger = new EventListener() {
        @Override
        public void onEvent(PlayerEvent event) {
            for (PlayerEvent.Listener eventListener : eventListeners){
                if(eventListener != null){
                    eventListener.onPlayerEvent(PlayerController.this, event);
                }
            }
        }
    };

    private StateChangedListener stateChangedTrigger = new StateChangedListener() {
        @Override
        public void onStateChanged(PlayerState state) {
            for(PlayerState.Listener listener : stateChangeListeners){
                if (listener != null){
                    listener.onPlayerStateChanged(PlayerController.this, state);
                }
            }
        }
    };

    public PlayerController(Context context, PlayerConfig config){
        this.context = context;
        this.mediaConfig = config.media;
        player = new ExoPlayerWrapper(context);
        player.setEventListener(eventTrigger);
        player.setStateChangedListener(stateChangedTrigger);
        prepare(config.media);
    }

    @Override
    public void prepare(@NonNull PlayerConfig.Media playerConfig) {
        Uri sourceUri = Uri.parse(playerConfig.getMediaEntry().getSources().get(0).getUrl());
        boolean shouldAutoplay = playerConfig.isAutoPlay();
        player.load(sourceUri, shouldAutoplay);
    }

    @Override
    public void release() {
        Log.d(TAG, "release");
        player.release();
    }

    @Override
    public View getView() {
        //return view of the current player.
        return player.getView();
    }

    @Override
    public long getDuration() {
        Log.d(TAG, "getDuration " + player.getCurrentPosition());

        return player.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        Log.d(TAG, "getPosition " + player.getCurrentPosition());
        return player.getCurrentPosition();
    }

    @Override
    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    @Override
    public void seekTo(long position) {
        Log.d(TAG, "seek to " + position);
        player.seekTo(position);
    }

    @Override
    public boolean getAutoPlay() {
        Log.d(TAG, "getAutoPlay " + player.shouldAutoPlay());
        return player.shouldAutoPlay();
    }

    @Override
    public void setAutoPlay(boolean autoPlay) {
        Log.d(TAG, "setAutoPlay => " + autoPlay);
        player.setAutoPlay(autoPlay);
    }

    @Override
    public void play() {
        Log.d(TAG, "play");
        player.play();
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause");
        player.pause();
    }

    @Override
    public void prepareNext(@NonNull PlayerConfig.Media mediaConfig) {
        Log.d(TAG, "prepareNext");
        prepare(mediaConfig);
    }

    @Override
    public void skip() {
        Log.d(TAG, "loadNext");
    }

    @Override
    public void addEventListener(@NonNull PlayerEvent.Listener listener, PlayerEvent... events) {
        Log.d(TAG, "addEventListener. events=> " + Arrays.toString(events));
        eventListeners.add(listener);
    }

    @Override
    public void addStateChangeListener(@NonNull PlayerState.Listener listener) {
        Log.d(TAG, "addStateChangeListener");
        stateChangeListeners.add(listener);
    }

    @Override
    public void restore() {
        Log.d(TAG, "on resume");
        player.setEventListener(eventTrigger);
        player.setStateChangedListener(stateChangedTrigger);
        player.resume();
        prepare(mediaConfig);
    }
}
