package com.kaltura.playkit.player;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.SubtitleView;

public abstract class BaseExoplayerView extends PlayerView {

    public BaseExoplayerView(Context context) {
        super(context);
    }

    public BaseExoplayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BaseExoplayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public abstract void setPlayer(SimpleExoPlayer player, boolean useTextureView, boolean isSurfaceSecured);

    public abstract void setVideoSurfaceProperties(boolean useTextureView, boolean isSurfaceSecured);

    public abstract SubtitleView getSubtitleView();

}
