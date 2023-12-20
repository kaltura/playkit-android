package com.kaltura.playkit.player;

import android.content.Context;
import android.util.AttributeSet;

import com.kaltura.androidx.media3.exoplayer.ExoPlayer;
import com.kaltura.androidx.media3.ui.SubtitleView;
import com.kaltura.androidx.media3.exoplayer.video.KVideoRendererFirstFrameWhenStartedEventListener;

public abstract class BaseExoplayerView extends PlayerView implements KVideoRendererFirstFrameWhenStartedEventListener {

    public BaseExoplayerView(Context context) {
        super(context);
    }

    public BaseExoplayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BaseExoplayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public abstract void setPlayer(ExoPlayer player, boolean useTextureView, boolean isSurfaceSecured, boolean hideVideoViews);

    public abstract void setVideoSurfaceProperties(boolean useTextureView, boolean isSurfaceSecured, boolean hideVideoViews);

    public abstract SubtitleView getSubtitleView();

    public abstract void applySubtitlesChanges();
}
