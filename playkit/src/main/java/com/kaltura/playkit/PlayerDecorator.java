package com.kaltura.playkit;

import android.support.annotation.NonNull;
import android.view.View;


public abstract class PlayerDecorator extends PlayerDecoratorBase {
    @Override
    public final void destroy() {
        super.destroy();
    }

    @Override
    final public View getView() {
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

    final void setPlayer(Player player) {
        super.setPlayer(player);
    }
}

