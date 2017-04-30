package com.kaltura.playkit.player;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SubtitleView;

import java.util.List;

/**
 * @hide
 */

public class ExoPlayerView extends PlayerView implements SimpleExoPlayer.VideoListener, TextRenderer.Output{

    private static final String TAG = ExoPlayerView.class.getSimpleName();

    private final View surfaceView;
    private final View posterView; // TODO should be changed to poster?
    private final SubtitleView subtitleLayout;
    private final AspectRatioFrameLayout layout;

    private SimpleExoPlayer player;

    public ExoPlayerView(Context context) {
        this(context, null);
    }

    public ExoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        layout = initFrameLayout();
        posterView = initPosterView();
        subtitleLayout = initSubtitleLayout();

        surfaceView = initSurfaceView();

        addView(layout);
        layout.addView(surfaceView, 0);
        layout.addView(posterView);
        layout.addView(subtitleLayout);
    }

    private View initSurfaceView() {
        View surfaceView = new SurfaceView(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        surfaceView.setLayoutParams(params);
        return surfaceView;
    }

    private SubtitleView initSubtitleLayout() {
        SubtitleView subtitleLayout = new SubtitleView(getContext());
        subtitleLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        subtitleLayout.setUserDefaultStyle();
        subtitleLayout.setUserDefaultTextSize();
        return subtitleLayout;
    }

    private View initPosterView() {
        View posterView = new View(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        posterView.setLayoutParams(params);
        posterView.setBackgroundColor(Color.BLACK);

        return posterView;
    }

    private AspectRatioFrameLayout initFrameLayout() {
        AspectRatioFrameLayout frameLayout = new AspectRatioFrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        frameLayout.setLayoutParams(params);
        return frameLayout;
    }

    /**
     * Returns the player currently set on this view, or null if no player is set.
     */
    public SimpleExoPlayer getPlayer() {
        return player;
    }

    /**
     * Set the {@link SimpleExoPlayer} to use. The {@link SimpleExoPlayer#setTextOutput} and
     * {@link SimpleExoPlayer#setVideoListener} method of the player will be called and previous
     * assignments are overridden.
     *
     * @param player The {@link SimpleExoPlayer} to use.
     */
    public void setPlayer(SimpleExoPlayer player) {
        if (this.player == player) {
            return;
        }
        if (this.player != null) {
            this.player.setTextOutput(null);
            this.player.setVideoListener(null);
            this.player.setVideoSurface(null);
        }
        this.player = player;

        if (player != null) {
            if (surfaceView instanceof TextureView) {
                player.setVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                player.setVideoSurfaceView((SurfaceView) surfaceView);
            }
            player.setVideoListener(this);
            player.setTextOutput(this);
        } else {
            posterView.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        layout.setAspectRatio(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
    }

    @Override
    public void onRenderedFirstFrame() {
        posterView.setVisibility(GONE);
    }

    @Override
    public void onCues(List<Cue> cues) {
        subtitleLayout.onCues(cues);
    }

    @Override
    public void hideVideoSurface() {
        surfaceView.setVisibility(GONE);
        subtitleLayout.setVisibility(GONE);
    }

    @Override
    public void showVideoSurface() {
        surfaceView.setVisibility(VISIBLE);
        subtitleLayout.setVisibility(VISIBLE);
    }
}

