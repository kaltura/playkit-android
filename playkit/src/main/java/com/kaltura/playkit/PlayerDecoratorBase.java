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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.PlayerView;
import com.kaltura.playkit.player.SubtitleStyleSettings;

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
    public long getPositionInWindowMs() {
        return player.getPositionInWindowMs();
    }

    @Override
    public long getCurrentProgramTime() {
        return player.getCurrentProgramTime();
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

    @SuppressWarnings("deprecation")
    @Override
    public PKEvent.Listener addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        return player.addEventListener(listener, events);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        player.removeEventListener(listener);
    }

    @SuppressWarnings("deprecation")
    @Override
    public PKEvent.Listener addStateChangeListener(@NonNull PKEvent.Listener listener) {
        return player.addStateChangeListener(listener);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeStateChangeListener(@NonNull PKEvent.Listener listener) {
        player.removeStateChangeListener(listener);
    }

    @Override
    public void removeListener(@NonNull PKEvent.Listener listener) {
        player.removeListeners(listener);
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

    @Override
    public void updateSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings) {
        player.updateSubtitleStyle(subtitleStyleSettings);
    }

    @Override
    public void updateSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        player.updateSurfaceAspectRatioResizeMode(resizeMode);
    }

    @Override
    public <E extends PKEvent> void addListener(Object groupId, Class<E> type, PKEvent.Listener<E> listener) {
        player.addListener(groupId, type, listener);
    }

    @Override
    public void addListener(Object groupId, Enum type, PKEvent.Listener listener) {
        player.addListener(groupId, type, listener);
    }

    @Override
    public void removeListeners(@NonNull Object groupId) {
        player.removeListeners(groupId);
    }
}
