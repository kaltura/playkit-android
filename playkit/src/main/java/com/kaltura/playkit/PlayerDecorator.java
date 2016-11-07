package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.view.View;


public abstract class PlayerDecorator implements Player {

    @Override
    public long getDuration() {
        return mPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(long position) {
        mPlayer.seekTo(position);
    }

    @Override
    public boolean getAutoPlay() {
        return mPlayer.getAutoPlay();
    }

    @Override
    public void setAutoPlay(boolean autoPlay) {
        mPlayer.setAutoPlay(autoPlay);
    }

    @Override
    public void play() {
        mPlayer.play();
    }

    @Override
    public void pause() {
        mPlayer.pause();
    }

    @Override
    public void prepareNext(@NonNull PlayerConfig playerConfig) {
        mPlayer.prepareNext(playerConfig);
    }

    
    
    
    @Override
    final public void update(@NonNull PlayerConfig playerConfig) {
        mPlayer.update(playerConfig);
    }

    @Override
    final public View getView() {
        return mPlayer.getView();
    }

    @Override
    final public void loadNext() {
        mPlayer.loadNext();
    }

    @Override
    final public void addEventListener(@NonNull PlayerEvent.Listener listener, PlayerEvent... events) {
        mPlayer.addEventListener(listener, events);
    }

    @Override
    final public void addStateChangeListener(@NonNull PlayerState.Listener listener) {
        mPlayer.addStateChangeListener(listener);
    }

    final void setPlayer(Player player) {
        mPlayer = player;
    }

    private Player mPlayer;

}
