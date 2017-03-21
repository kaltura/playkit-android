package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.player.PlayerView;

public class PlayerDecoratorBase implements Player {
    
    @Override
    public void prepare(@NonNull PKMediaConfig mediaConfig) {
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
    public AdController getAdController() {
        return player.getAdController();
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
    public void replay() {
        player.replay();
    }

    @Override
    public void setVolume(float volume) {
        player.setVolume(volume);
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public void prepareNext(@NonNull PKMediaConfig mediaConfig) {
        player.prepareNext(mediaConfig);
    }

    @Override
    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    @Override
    public void destroy() {
        player.destroy();
    }

    @Override
    public void reset() {
        player.reset();
    }

    @Override
    public PlayerView getView() {
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
    public void onApplicationResumed() {
        player.onApplicationResumed();
    }

    @Override
    public void changeTrack(String uniqueId) {
        player.changeTrack(uniqueId);
    }

    @Override
    public void onApplicationPaused() {
        player.onApplicationPaused();
    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
        player.updatePluginConfig(pluginName, pluginConfig);
    }
}
