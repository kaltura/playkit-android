package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

public class PlayerDecoratorBase implements Player {
    
    @Override
    public void prepare(@NonNull PlayerConfig.Media mediaConfig) {
        player.prepare(mediaConfig);
    }

    @Override
    public long getDuration() {
        return player.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    @Override
    public void seekTo(long position) {
        player.seekTo(position);
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
    public void prepareNext(@NonNull PlayerConfig.Media mediaConfig) {
        player.prepareNext(mediaConfig);
    }

    @Override
    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    @Override
    public void release() {
        player.release();
    }

    @Override
    public View getView() {
        return player.getView();
    }

    @Override
    public void skip() {
        player.skip();
    }

    @Override
    public void addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        player.addEventListener(listener, events);
    }

    @Override
    public void addStateChangeListener(@NonNull PKEvent.Listener listener) {
        player.addStateChangeListener(listener);
    }

    void setPlayer(Player player) {
        this.player = player;
    }
    
    Player getPlayer() {
        return player;
    }

    private Player player;

    @Override
    public void restore() {
        player.restore();
    }

    @Override
    public PKAdInfo getAdInfo() {
        return player.getAdInfo();
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable Object value) {
        player.updatePluginConfig(pluginName, key, value);
    }
}
