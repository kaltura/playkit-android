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

/**
 * Created by anton.afanasiev on 01/11/2016.
 */

public class PlayerController {

    private static final String TAG = PlayerController.class.getSimpleName();

    private PlayerEngine playerEngine;
    private Context context;

    private PKEvent.Listener eventListener;

    private PlayerConfig.Media mediaConfig;

    public void setEventListener(PKEvent.Listener eventListener) {
        this.eventListener = eventListener;
    }


    interface EventListener {
        void onEvent(PlayerEvent event);
    }

    interface StateChangedListener {
        void onStateChanged(PlayerState state, PlayerState previousState);
    }

    private EventListener eventProxy = new EventListener() {
        @Override
        public void onEvent(PlayerEvent event) {
            if (eventListener != null) {
                eventListener.onEvent(event);
            }
        }
    };

    private StateChangedListener stateChangedProxy = new StateChangedListener() {
        @Override
        public void onStateChanged(PlayerState state, PlayerState oldState) {
            if (eventListener != null) {
                eventListener.onEvent(state.stateChangedEvent(oldState));
            }
        }
    };

    public PlayerController(Context context, PlayerConfig.Media mediaConfig){
        this.context = context;
        this.mediaConfig = mediaConfig;
        playerEngine = new ExoPlayerWrapper(context);
        playerEngine.setEventListener(eventProxy);
        playerEngine.setStateChangedListener(stateChangedProxy);
//        prepare(config.media);
    }

    public void prepare(@NonNull PlayerConfig.Media playerConfig) {
        Uri sourceUri = Uri.parse(playerConfig.getMediaEntry().getSources().get(0).getUrl());
        playerEngine.prepare(sourceUri);
    }

    public void release() {
        Log.d(TAG, "release");
        playerEngine.release();
    }

    public View getView() {
        //return view of the current playerEngine.
        return playerEngine.getView();
    }

    public long getDuration() {
        Log.d(TAG, "getDuration " + playerEngine.getCurrentPosition());

        return playerEngine.getDuration();
    }

    public long getCurrentPosition() {
        Log.d(TAG, "getPosition " + playerEngine.getCurrentPosition());
        return playerEngine.getCurrentPosition();
    }

    public long getBufferedPosition() {
        return playerEngine.getBufferedPosition();
    }

    public void seekTo(long position) {
        Log.d(TAG, "seek to " + position);
        playerEngine.seekTo(position);
    }

    public void play() {
        Log.d(TAG, "play");
        playerEngine.play();
    }

    public void pause() {
        Log.d(TAG, "pause");
        playerEngine.pause();
    }
    
    public void restore() {
        Log.d(TAG, "on resume");
        playerEngine.setEventListener(eventProxy);
        playerEngine.setStateChangedListener(stateChangedProxy);
        playerEngine.resume();
        prepare(mediaConfig);
    }
    
    public Player player() {
        return new Player() {

            PlayerController playerController = PlayerController.this;

            @Override
            public void prepare(@NonNull PlayerConfig.Media playerConfig) {
                playerController.prepare(playerConfig);
            }

            @Override
            public void release() {
                playerController.release();
            }

            @Override
            public View getView() {
                return playerController.getView();
            }

            @Override
            public long getDuration() {
                return playerController.getDuration();
            }

            @Override
            public long getCurrentPosition() {
                return playerController.getCurrentPosition();
            }

            @Override
            public long getBufferedPosition() {
                return playerController.getBufferedPosition();
            }

            @Override
            public void seekTo(long position) {
                playerController.seekTo(position);
            }

            @Override
            public void play() {
                playerController.play();
            }

            @Override
            public void pause() {
                playerController.pause();
            }

            @Override
            public void restore() {
                playerController.restore();
            }

            @Override
            public void prepareNext(@NonNull PlayerConfig.Media mediaConfig) {
                Assert.shouldNeverHappen();
            }

            @Override
            public void skip() {
                Assert.shouldNeverHappen();
            }

            @Override
            public void addEventListener(@NonNull PKEvent.Listener listener, PKEvent... events) {
                Assert.shouldNeverHappen();
            }

            @Override
            public void addStateChangeListener(@NonNull PKEvent.Listener<PlayerState.StateChangedEvent> listener) {
                Assert.shouldNeverHappen();
            }

            @Override
            public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable JsonElement value) {
                Assert.shouldNeverHappen();
            }
        };
    }
}

