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
    private PlayerConfig playerConfig;
    private Context context;

    private List<PlayerEvent.Listener> eventListeners = new ArrayList<>();
    private List<PlayerState.Listener> stateChangeListeners = new ArrayList<>();

    public interface EventTrigger {
        void triggerEvent(PlayerEvent event);
    }

    public interface StateChangedTrigger {
        void triggerStateChanged(PlayerState state);
    }

    private EventTrigger eventTrigger = new EventTrigger() {
        @Override
        public void triggerEvent(PlayerEvent event) {
            for (PlayerEvent.Listener eventListener : eventListeners){
                if(eventListener != null){
                    eventListener.onPlayerEvent(PlayerController.this, event);
                }
            }
        }
    };

    private StateChangedTrigger stateChangedTrigger = new StateChangedTrigger() {
        @Override
        public void triggerStateChanged(PlayerState state) {
            for(PlayerState.Listener listener : stateChangeListeners){
                if (listener != null){
                    listener.onPlayerStateChanged(PlayerController.this, state);
                }
            }
        }
    };

    public PlayerController(Context context){
        this.context = context;
        //create default player(ExoPlayer).
        player = new ExoPlayerWrapper(context);
        player.setEventTrigger(eventTrigger);
        player.setStateChangedTrigger(stateChangedTrigger);
        //set player listener that notify controller about events that happened.
        // the PlayerController will pass them to the app.
    }

    @Override
    public void load(@NonNull PlayerConfig playerConfig) {
        this.playerConfig = playerConfig;
        //create player based on player config.
        //wv classic -> MediaPlayerWrapper.
        // everything else -> ExoPlayerWrapper.
        //set eventListener.
        Uri sourceUri = Uri.parse(playerConfig.getEntry().getSources().get(0).getUrl());
        boolean shouldAutoplay = playerConfig.shouldAutoPlay();
        player.load(sourceUri, shouldAutoplay);

    }

    @Override
    public void apply(@NonNull PlayerConfig playerConfig) {
        Log.e(TAG, "apply");
    }

    @Override
    public View getView() {
        //return view of the current player.
        return player.getView();
    }

    @Override
    public long getCurrentPosition() {
        Log.e(TAG, "getPosition " + player.getCurrentPosition());
        return player.getCurrentPosition();
    }

    @Override
    public void seekTo(long position) {
        Log.e(TAG, "seek to " + position);
        player.seekTo(position);
    }

    @Override
    public boolean getAutoPlay() {
        Log.e(TAG, "getAutoPlay " + player.shouldAutoPlay());
        return false;
    }

    @Override
    public void setAutoPlay(boolean autoPlay) {
        Log.e(TAG, "setAutoPlay => " + autoPlay);
        player.setAutoPlay(autoPlay);

    }

    @Override
    public void play() {
        Log.e(TAG, "play");
        player.play();
    }

    @Override
    public void pause() {
        Log.e(TAG, "pause");
        player.pause();
    }

    @Override
    public void prepareNext(@NonNull PlayerConfig playerConfig) {
        Log.e(TAG, "prepareNext");

    }

    @Override
    public void loadNext() {
        Log.e(TAG, "loadNext");
    }

    @Override
    public void addEventListener(@NonNull PlayerEvent.Listener listener, PlayerEvent... events) {
        Log.e(TAG, "addEventListener. events=> " + Arrays.toString(events));
        eventListeners.add(listener);
    }

    @Override
    public void addStateChangeListener(@NonNull PlayerState.Listener listener) {
        Log.e(TAG, "addStateChangeListener");
        stateChangeListeners.add(listener);
    }

    @Override
    public void addBoundaryTimeListener(@NonNull TimeListener listener, boolean wait, @NonNull RelativeTime... times) {
        Log.e(TAG, "addBoundaryTimeListener");
    }

    @Override
    public void addPeriodicTimeListener(@NonNull TimeListener listener, long interval) {
        Log.e(TAG, "addPeriodicTimeListener");
    }

    public PlayerConfig getPlayerConfig() {
        return playerConfig;
    }
}
