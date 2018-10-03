/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playkit.player.PlayerView;

public class PlayerDecoratorBase implements Player {

    @Override
    public Settings getSettings() {
        return player.getSettings();
    }

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
    public <T extends PKController> T getController(Class<T> type) {
        return player.getController(type);
    }

    @Override
    public final String getSessionId() {
        return player.getSessionId();
    }

    @Override
    public boolean isLive() {
        return player.isLive();
    }

    @Override
    public PKMediaFormat getMediaFormat() {
        return player.getMediaFormat();
    }

    @Override
    public void setPlaybackRate(float rate) {
        player.setPlaybackRate(rate);
    }

    @Override
    public float getPlaybackRate() {
        return player.getPlaybackRate();
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
    public void stop() {
        player.stop();
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
    public void onOrientationChanged() {
        player.onOrientationChanged();
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
