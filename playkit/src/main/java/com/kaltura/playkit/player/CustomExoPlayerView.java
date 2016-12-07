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
import com.kaltura.playkit.PlayerConfig;

import java.util.List;

/**
 * Created by anton.afanasiev on 13/11/2016.
 */

public class CustomExoPlayerView extends FrameLayout implements SimpleExoPlayer.VideoListener, TextRenderer.Output{

    private static final String TAG = CustomExoPlayerView.class.getSimpleName();

    private final PlayerConfig.Media mediaConfig;
    private final View surfaceView;
    private final View posterView; // TODO should be changed to poster?
    private final SubtitleView subtitleLayout;
    private final AspectRatioFrameLayout layout;

    private SimpleExoPlayer player;

    public CustomExoPlayerView(Context context, PlayerConfig.Media mediaConfig) {
        this(context, mediaConfig, null);
    }

    public CustomExoPlayerView(Context context, PlayerConfig.Media mediaConfig, AttributeSet attrs) {
        this(context, mediaConfig, attrs, 0);
    }

    public CustomExoPlayerView(Context context, PlayerConfig.Media mediaConfig, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mediaConfig = mediaConfig;
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
                if (mediaConfig.isAutoPlay()) {
                   hideVideoSurfaceView();
                }
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
    public void onVideoTracksDisabled() {
        posterView.setVisibility(VISIBLE);
    }

    @Override
    public void onCues(List<Cue> cues) {
        subtitleLayout.onCues(cues);
    }

    public void showVideoSurfaceView() {
        surfaceView.setVisibility(VISIBLE);
    }

    public void hideVideoSurfaceView() {
        surfaceView.setVisibility(GONE);
    }

}

