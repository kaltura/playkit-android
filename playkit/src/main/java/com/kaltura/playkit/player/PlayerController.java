package com.kaltura.playkit.player;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.kaltura.playkit.Assert;
import com.kaltura.playkit.PKAdInfo;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;

/**
 * Created by anton.afanasiev on 01/11/2016.
 */

public class PlayerController implements Player {

    private static final String TAG = PlayerController.class.getSimpleName();

    private PlayerEngine player;
    private Context context;
    
    private PlayerConfig.Media mediaConfig;

    private PKEvent.Listener eventListener; 
    
    public void setEventListener(PKEvent.Listener eventListener) {
        this.eventListener = eventListener;
    }

    public interface EventListener {
        void onEvent(PlayerEvent event);
    }

    public interface StateChangedListener {
        void onStateChanged(PlayerState oldState, PlayerState newState);
    }

    private EventListener eventTrigger = new EventListener() {
        @Override
        public void onEvent(PlayerEvent event) {
            if (eventListener != null) {
                eventListener.onEvent(event);
            }
        }
    };

    private StateChangedListener stateChangedTrigger = new StateChangedListener() {
        @Override
        public void onStateChanged(PlayerState oldState, PlayerState newState) {
            if (eventListener != null) {
                eventListener.onEvent(new PlayerState.Event(oldState, newState));
            }
        }
    };

    public PlayerController(Context context, PlayerConfig.Media mediaConfig){
        this.context = context;
        this.mediaConfig = mediaConfig;
        player = new ExoPlayerWrapper(context);
        player.setEventListener(eventTrigger);
        player.setStateChangedListener(stateChangedTrigger);
    }
    
    public void prepare(@NonNull PlayerConfig.Media mediaConfig) {
        Uri sourceUri = Uri.parse(mediaConfig.getMediaEntry().getSources().get(0).getUrl());
        player.load(sourceUri);
    }

    public void release() {
        Log.d(TAG, "release");
        player.release();
    }

    public View getView() {
        //return view of the current player.
        return player.getView();
    }

    public long getDuration() {
        Log.d(TAG, "getDuration " + player.getDuration());

        return player.getDuration();
    }

    public long getCurrentPosition() {
        Log.d(TAG, "getPosition " + player.getCurrentPosition());
        return player.getCurrentPosition();
    }

    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    public void seekTo(long position) {
        Log.d(TAG, "seek to " + position);
        player.seekTo(position);
    }

    public void play() {
        Log.d(TAG, "play");
        player.play();
    }

    public void pause() {
        Log.d(TAG, "pause");
        player.pause();
    }

    public void restore() {
        Log.d(TAG, "on resume");
        player.setEventListener(eventTrigger);
        player.setStateChangedListener(stateChangedTrigger);
        player.resume();
        prepare(mediaConfig);
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
    public void addEventListener(@NonNull PKEvent.Listener listener, PKEvent... events) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void addStateChangeListener(@NonNull PlayerState.Listener listener) {
        Assert.shouldNeverHappen();
    }

    @Override
    public PKAdInfo getAdInfo() {
        Assert.shouldNeverHappen();
        return null;
    }}
