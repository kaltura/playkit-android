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

import android.annotation.TargetApi;
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
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SubtitleView;

import java.util.List;

/**
 * @hide
 */

class ExoPlayerView extends PlayerView implements SimpleExoPlayer.VideoListener, TextOutput {

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

        //by default create with unsecured SurfaceView.
        swapVideoSurface(false, false);

        layout.addView(posterView);
        layout.addView(subtitleLayout);
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
     * Swap the video surface view that player should render.
     *
     * @param useTextureView - if should use {@link TextureView}
     * @param isSurfaceSecured       - should allow secure rendering of the surface
     */
    void swapVideoSurface(boolean useTextureView, boolean isSurfaceSecured) {

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
            applyVideoSurface(isSurfaceSecured);
        }
    }

    /**
     * Set the {@link SimpleExoPlayer} to use. The {@link SimpleExoPlayer#setTextOutput} and
     * {@link SimpleExoPlayer#setVideoListener} method of the player will be called and previous
     * assignments are overridden.
     *
     * @param player The {@link SimpleExoPlayer} to use.
     */
    void setPlayer(SimpleExoPlayer player, boolean isSurfaceSecured) {
        if (this.player == player) {
            return;
        }

        if (this.player != null) {
            this.player.removeTextOutput(this);
            this.player.removeVideoListener(this);
            removeVideoSurface();
        }

        this.player = player;

        if (player != null) {
            applyVideoSurface(isSurfaceSecured);
        } else {
            posterView.setVisibility(VISIBLE);
        }
    }

    /**
     * Will set videoSurface to player, and reset all the related listeners.
     */
    @TargetApi(17)
    private void applyVideoSurface(boolean isSurfaceSecured) {

        removeVideoSurface();

        //Decide which type of videoSurface should be set.
        if (videoSurface instanceof TextureView) {
            player.setVideoTextureView((TextureView) videoSurface);
        } else {
            ((SurfaceView) videoSurface).setSecure(isSurfaceSecured);
            player.setVideoSurfaceView((SurfaceView) videoSurface);
        }

        //Set listeners
        player.addVideoListener(this);
        player.addTextOutput(this);
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

    private void removeVideoSurface() {
        //Remove existed videoSurface from player.
        if (videoSurface instanceof SurfaceView) {
            this.player.clearVideoSurfaceView((SurfaceView) videoSurface);
        } else if (videoSurface instanceof TextureView) {
            this.player.clearVideoTextureView((TextureView) videoSurface);
        }
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

