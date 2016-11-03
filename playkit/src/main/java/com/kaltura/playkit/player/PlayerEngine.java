package com.kaltura.playkit.player;

import android.net.Uri;
import android.view.View;

/**
 * Created by anton.afanasiev on 01/11/2016.
 */

public interface PlayerEngine {

    void load(Uri mediaSourceUri, boolean shouldAutoplay);

    View getView();

    void play();

    void pause();

    long getCurrentPosition();

    void seekTo(long position);

    boolean shouldAutoPlay();

    void setAutoPlay(boolean shouldAutoplay);

    void setEventTrigger(PlayerController.EventTrigger eventTrigger);

    void setStateChangedTrigger(PlayerController.StateChangedTrigger stateChangedTrigger);

    long getDuration();
}
