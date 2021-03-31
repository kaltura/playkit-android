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

package com.kaltura.playkit.player;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.kaltura.android.exoplayer2.text.Cue;

import java.util.List;

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

    public void setSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {}

    public void setSubtitleViewPosition(PKSubtitlePosition subtitleViewPosition) {}
    
    public void setLastReportedCue(List<Cue> lastReportedCue) {}

    /**
     * Method call is being handled from update the video view(ExoPlayerWrapper) from client app
     * @param isVisible videoSurface visibility
     */
    public void toggleVideoViewVisibility(boolean isVisible) {}
}
