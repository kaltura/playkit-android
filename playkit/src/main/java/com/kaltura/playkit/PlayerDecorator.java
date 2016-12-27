package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playkit.player.PKView;


public abstract class PlayerDecorator extends PlayerDecoratorBase {
    @Override
    public final void destroy() {
        super.destroy();
    }

    @Override
    final public PKView getView() {
        return super.getView();
    }

    @Override
    final public void skip() {
        super.skip();
    }

    @Override
    final public void addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        super.addEventListener(listener, events);
    }

    @Override
    final public void addStateChangeListener(@NonNull PKEvent.Listener listener) {
        super.addStateChangeListener(listener);
    }

    final Player getPlayer() {
        return super.getPlayer();
    }

    @Override
    final public void onApplicationResumed() {
        super.onApplicationResumed();
    }

    @Override
    final public void onApplicationPaused() {
        super.onApplicationPaused();
    }

    @Override
    public boolean isAutoPlay() {
        return super.isAutoPlay();
    }

    @Override
    final public void updatePluginConfig(@NonNull String pluginName, @NonNull String key, @Nullable Object value) {
        super.updatePluginConfig(pluginName, key, value);
    }

    final void setPlayer(Player player) {
        super.setPlayer(player);
    }
}

