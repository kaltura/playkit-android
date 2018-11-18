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


public abstract class PlayerDecorator extends PlayerDecoratorBase {
    @Override
    public final void destroy() {
        super.destroy();
    }

    @Override
    final public PlayerView getView() {
        return super.getView();
    }

    @Override
    final public PKEvent.Listener addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        return super.addEventListener(listener, events);
    }

    @Override
    final public void removeEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        super.removeEventListener(listener, events);
    }

    @Override
    final public PKEvent.Listener addStateChangeListener(@NonNull PKEvent.Listener listener) {
        return super.addStateChangeListener(listener);
    }

    @Override
    final public void removeStateChangeListener(@NonNull PKEvent.Listener listener) {
        super.removeStateChangeListener(listener);
    }

    @Override
    final public void removeListener(@NonNull PKEvent.Listener listener) {
        super.removeListener(listener);
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
    final public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
        super.updatePluginConfig(pluginName, pluginConfig);
    }

    final void setPlayer(Player player) {
        super.setPlayer(player);
    }
}

