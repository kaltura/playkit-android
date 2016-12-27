package com.kaltura.playkit.player;

import android.view.View;

/**
 * Created by gilad.nadav on 27/12/2016.
 */

public interface PKView {
    void hideVideoSurface();
    void showVideoSurface();
    View getContainerView();
}
