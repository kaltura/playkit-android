package com.kaltura.playkit.player;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.kaltura.playkit.Assert;
import com.kaltura.playkit.PKAdInfo;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.TrackData;

/**
 * Created by anton.afanasiev on 01/11/2016.
 */

public class PlayerController implements Player {

    private static final String TAG = PlayerController.class.getSimpleName();

    private PlayerEngine player;
    private Context context;

    private PlayerConfig.Media mediaConfig;
    private boolean wasReleased = false;


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
        togglePlayerListeners(true);
    }

    public void prepare(@NonNull PlayerConfig.Media mediaConfig) {
//       Uri sourceUri = Uri.parse(mediaConfig.getMediaEntry().getSources().get(0).getUrl());
//        Uri sourceUri = Uri.parse("https://tungsten.aaplimg.com/VOD/bipbop_adv_example_v2/master.m3u8");
        Uri sourceUri = Uri.parse("https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8");
//        Uri sourceUri = Uri.parse("http://cdnapi.kaltura.com/p/243342/sp/24334200/playManifest/entryId/0_uka1msg4/flavorIds/1_vqhfu6uy,1_80sohj7p/format/applehttp/protocol/http/a.m3u8");// Uri.parse(playerConfig.getMediaEntry().getSources().get(0).getUrl());
        player.load(sourceUri, mediaConfig.getStartPosition());
    }

    public void release() {
        Log.d(TAG, "release");
        player.release();
        togglePlayerListeners(false);
        wasReleased = true;
    }

    public View getView() {
        //return view of the current player.
        return player.getView();
    }

    public long getDuration() {
//        Log.d(TAG, "getDuration " + player.getDuration());

        return player.getDuration();
    }

    public long getCurrentPosition() {
//        Log.d(TAG, "getPosition " + player.getCurrentPosition());
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
        if(wasReleased){
            player.restore();
            prepare(mediaConfig);
            togglePlayerListeners(true);
            wasReleased = false;
        }
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
    public void addEventListener(@NonNull PKEvent.Listener listener, PKEvent... events) {
        Assert.shouldNeverHappen();
    }

    @Override
    public void addStateChangeListener(@NonNull PlayerState.Listener listener) {
        Assert.shouldNeverHappen();
    }

    public PKAdInfo getAdInfo() {
        Assert.shouldNeverHappen();
        return null;
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable Object value) {
        Assert.shouldNeverHappen();
    }

    @Override
    public TrackData getTrackData() {
        return player.getTrackData();
    }

    @Override
    public void changeTrack(int trackType, int position) {
        player.changeTrack(trackType, position);
    }
}
