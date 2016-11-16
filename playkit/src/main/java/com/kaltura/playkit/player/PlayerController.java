package com.kaltura.playkit.player;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.google.gson.JsonElement;
import com.kaltura.playkit.Assert;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anton.afanasiev on 01/11/2016.
 */

public class PlayerController implements Player {

    private static final String TAG = PlayerController.class.getSimpleName();

    private PlayerEngine player;
    private Context context;

    private PKEvent.Listener listener;
    private List<PlayerState.Listener> stateChangeListeners = new ArrayList<>();

    private PlayerConfig.Media mediaConfig;



    interface EventListener {
        void onEvent(PlayerEvent event);
    }

    interface StateChangedListener {
        void onStateChanged(PlayerState state);
    }

    private EventListener eventTrigger = new EventListener() {
        @Override
        public void onEvent(PlayerEvent event) {
            if (listener != null) {
                listener.onEvent(event);
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
//        prepare(config.media);
    }

    @Override
    public void prepare(@NonNull PlayerConfig.Media playerConfig) {
        Uri sourceUri = Uri.parse(playerConfig.getMediaEntry().getSources().get(0).getUrl());
        player.load(sourceUri);
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
    public void addEventListener(@NonNull PKEvent.Listener listener, PKEvent... events) {
        this.listener = listener;
    }

    @Override
    public void addStateChangeListener(@NonNull PlayerState.Listener listener) {
        Log.d(TAG, "addStateChangeListener");
        stateChangeListeners.add(listener);
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable JsonElement value) {
        Assert.failState("Something is wrong");
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
