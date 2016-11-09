package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.view.View;

public abstract class PlayerDecoratorBase implements Player {
    
    @Override
    public void prepare(@NonNull PlayerConfig.Media mediaConfig) {
        mPlayer.prepare(mediaConfig);
    }

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
    public void prepareNext(@NonNull PlayerConfig.Media mediaConfig) {
        mPlayer.prepareNext(mediaConfig);
    }

    @Override
    public long getBufferedPosition() {
        return mPlayer.getBufferedPosition();
    }

    @Override
    public void release() {
        mPlayer.release();
    }

    @Override
    public View getView() {
        return mPlayer.getView();
    }

    @Override
    public void skip() {
        mPlayer.skip();
    }

    @Override
    public void addEventListener(@NonNull PlayerEvent.Listener listener, PlayerEvent... events) {
        mPlayer.addEventListener(listener, events);
    }

    @Override
    public void addStateChangeListener(@NonNull PlayerState.Listener listener) {
        mPlayer.addStateChangeListener(listener);
    }

    void setPlayer(Player player) {
        mPlayer = player;
    }
    
    Player getPlayer() {
        return mPlayer;
    }

    private Player mPlayer;

}
