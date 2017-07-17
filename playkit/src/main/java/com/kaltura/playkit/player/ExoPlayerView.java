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

class ExoPlayerView extends PlayerView implements SimpleExoPlayer.VideoListener, TextRenderer.Output {

    private View videoSurface;
    private final View posterView; // TODO should be changed to poster?
    private final SubtitleView subtitleLayout;
    private final AspectRatioFrameLayout layout;

    private SimpleExoPlayer player;

    ExoPlayerView(Context context) {
        this(context, null);
    }

    ExoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    ExoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        layout = initFrameLayout();
        posterView = initPosterView();
        subtitleLayout = initSubtitleLayout();

        addView(layout);

        //by default create with SurfaceView.
        swapVideoSurface(false);

        layout.addView(posterView);
        layout.addView(subtitleLayout);
    }

    /**
     * Swap the video surface view that player should render.
     * @param useTextureView - if should use {@link TextureView}
     */
    void swapVideoSurface(boolean useTextureView) {

        if (useTextureView) {
            videoSurface = new TextureView(getContext());
        } else {
            videoSurface = new SurfaceView(getContext());
        }

        //Set view size to match the parent.
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        videoSurface.setLayoutParams(params);

        //Check if videoSurface exist in view hierarchy
        View firstChild = layout.getChildAt(0);
        if (firstChild instanceof TextureView || firstChild instanceof SurfaceView) {
            //Remove it if so.
            layout.removeView(firstChild);
        }

        //Add newly created videoSurface.
        layout.addView(videoSurface, 0);

        //Apply videoSurface to the player(if exist).
        if (player != null) {
            applyVideoSurface();
        }
    }

    /**
     * Will set videoSurface to player, and reset all the related listeners.
     */
    private void applyVideoSurface() {
        //Remove existed videoSurface from player.
        player.setVideoSurface(null);

        //Decide which type of videoSurface should be set.
        if (videoSurface instanceof TextureView) {
            player.setVideoTextureView((TextureView) videoSurface);
        } else {
            player.setVideoSurfaceView((SurfaceView) videoSurface);
        }
        //Clear listeners
        player.setVideoListener(null);
        player.setTextOutput(null);
        //Set listeners
        player.setVideoListener(this);
        player.setTextOutput(this);
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
     * Set the {@link SimpleExoPlayer} to use. The {@link SimpleExoPlayer#setTextOutput} and
     * {@link SimpleExoPlayer#setVideoListener} method of the player will be called and previous
     * assignments are overridden.
     *
     * @param player The {@link SimpleExoPlayer} to use.
     */
    void setPlayer(SimpleExoPlayer player) {
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
            applyVideoSurface();
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
        videoSurface.setVisibility(GONE);
        subtitleLayout.setVisibility(GONE);
    }

    @Override
    public void showVideoSurface() {
        videoSurface.setVisibility(VISIBLE);
        subtitleLayout.setVisibility(VISIBLE);
    }

    @Override
    public void hideVideoSubtitles() {
        subtitleLayout.setVisibility(GONE);
    }

    @Override
    public void showVideoSubtitles() {
        subtitleLayout.setVisibility(VISIBLE);
    }
}

