package com.kaltura.playkit.player;

import android.content.Context;
import android.util.AttributeSet;

import com.kaltura.android.exoplayer2.SimpleExoPlayer;
import com.kaltura.android.exoplayer2.text.Cue;
import com.kaltura.android.exoplayer2.ui.SubtitleView;
import com.kaltura.playkit.PKLifecycleState;

import java.util.List;

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

    public abstract void setPlayer(SimpleExoPlayer player, boolean useTextureView, boolean isSurfaceSecured, boolean hideVideoViews);

    public abstract void setVideoSurfaceProperties(boolean useTextureView, boolean isSurfaceSecured, boolean hideVideoViews);

    public abstract SubtitleView getSubtitleView();

    public abstract void applySubtitlesChanges();

    public abstract List<Cue> getLastReportedCue();

    public abstract void setLastReportedCue(List<Cue> cue);

    public abstract void setApplicationState(PKLifecycleState applicationState);
}
