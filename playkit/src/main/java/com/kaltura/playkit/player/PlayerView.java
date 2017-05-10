package com.kaltura.playkit.player;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by gilad.nadav on 27/12/2016.
 */

public abstract class PlayerView extends FrameLayout {
    public PlayerView(Context context) {
        super(context);
    }

    public PlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public abstract void hideVideoSurface();
    public abstract void showVideoSurface();
    public abstract void hideVideoSubtitles();
    public abstract void showVideoSubtitles();

}
