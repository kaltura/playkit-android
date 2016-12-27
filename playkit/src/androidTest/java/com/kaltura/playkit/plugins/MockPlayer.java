package com.kaltura.playkit.plugins;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.player.PlayerView;

/**
 * Created by zivilan on 11/12/2016.
 */

public class MockPlayer implements Player {
    private boolean isPlaying = false;
    private int duration = 100;
    private long currentPosition = 0;

    @Override
    public void prepare(@NonNull PlayerConfig.Media playerConfig) {

    }

    @Override
    public void prepareNext(@NonNull PlayerConfig.Media mediaConfig) {

    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable Object value) {

    }

    @Override
    public void skip() {

    }

    @Override
    public void onApplicationPaused() {

    }

    @Override
    public void onApplicationResumed() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void play() {
        isPlaying = true;
    }

    @Override
    public void pause() {
        isPlaying = true;
    }

    @Override
    public void replay() {

    }

    @Override
    public PlayerView getView() {
        return null;
    }

    @Override
    public long getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public long getBufferedPosition() {
        return 0;
    }

    @Override
    public void setVolume(float volume) {

    }

    @Override
    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    public boolean isAutoPlay() {
        return false;
    }

    @Override
    public void addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {

    }

    @Override
    public void addStateChangeListener(@NonNull PKEvent.Listener listener) {

    }

    @Override
    public void changeTrack(String uniqueId) {

    }

    @Override
    public void seekTo(long position) {
        currentPosition = position;
    }

    @Override
    public AdController getAdController() {
        return null;
    }

    public void setDuration(int duration){
        this.duration = duration;
    }
}
