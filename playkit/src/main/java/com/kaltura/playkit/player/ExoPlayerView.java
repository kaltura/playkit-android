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
import android.graphics.Matrix;
import android.graphics.RectF;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.ui.spherical.SphericalSurfaceView;
import com.google.android.exoplayer2.video.VideoListener;
import com.kaltura.playkit.PKLog;

import java.util.List;

/**
 * View that is attached to the Exoplayer and responsible for displaying and managing
 * graphic content of the media.
 *
 * @hide
 */

class ExoPlayerView extends BaseExoplayerView {
    private static final PKLog log = PKLog.get("ExoPlayerView");
    private View shutterView;
    private View videoSurface;
    private SubtitleView subtitleView;
    private AspectRatioFrameLayout contentFrame;
    private ImageView artworkView;
    private Drawable artworkDrawable;

    private SimpleExoPlayer player;
    private ComponentListener componentListener;
    private Player.EventListener playerEventListener;
    private int textureViewRotation;
    private @AspectRatioFrameLayout.ResizeMode int resizeMode;


    ExoPlayerView(Context context) {
        this(context, null);
    }

    ExoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    ExoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        componentListener = new ComponentListener();
        playerEventListener = getPlayerEventListener();
        initContentFrame();
        initArtworkView();
        initSubtitleLayout();
        initPosterView();
    }

    @NonNull
    private Player.EventListener getPlayerEventListener() {
        return new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_READY:
                        if (playWhenReady) {
                            log.d("ExoPlayerView READY. playWhenReady => " + true);
                            shutterView.setVisibility(INVISIBLE);
                            if (artworkDrawable != null) {
                                setArtworkViewVisibility(true);
                            } else {
                                setArtworkViewVisibility(false);
                            }
                        }
                        break;

                    default:
                        break;

                }
            }
        };
    }

    /**
     * Set the {@link SimpleExoPlayer} to use. If ExoplayerView instance already has
     * player attached to it, it will remove and clear videoSurface first.
     *
     * @param player           - The {@link SimpleExoPlayer} to use.
     * @param isSurfaceSecured - should allow secure rendering of the surface
     */
    @Override
    public void setPlayer(SimpleExoPlayer player, boolean useTextureView, boolean isSurfaceSecured) {
        if (this.player == player) {
            return;
        }

        if (this.player != null) {
            removeVideoSurface();
        }
        this.player = player;
        addVideoSurface(useTextureView, isSurfaceSecured);
    }

    /**
     * Swap the video surface view that player should render.
     *
     * @param useTextureView   - if should use {@link TextureView} for rendering
     * @param isSurfaceSecured - should allow secure rendering of the surface
     */
    @Override
    public void setVideoSurfaceProperties(boolean useTextureView, boolean isSurfaceSecured) {
        if (player != null) {
            removeVideoSurface();
            addVideoSurface(useTextureView, isSurfaceSecured);
        }
    }

    /**
     * Create and set relevant surface and listeners to player and attach Surface to the view hierarchy.
     *
     * @param isSurfaceSecured - should allow secure rendering of the surface
     */
    @TargetApi(17)
    private void addVideoSurface(boolean useTextureView, boolean isSurfaceSecured) {
        resetViews();
        createVideoSurface(useTextureView);

        Player.VideoComponent newVideoComponent = player.getVideoComponent();
        Player.TextComponent newTextComponent = player.getTextComponent();
        player.addListener(playerEventListener);

        //Decide which type of videoSurface should be set.
        if (newVideoComponent != null) {
            if (videoSurface instanceof TextureView) {
                newVideoComponent.setVideoTextureView((TextureView) videoSurface);
            } else {
                ((SurfaceView) videoSurface).setSecure(isSurfaceSecured);
                newVideoComponent.setVideoSurfaceView((SurfaceView) videoSurface);
            }
            //Set listeners
            newVideoComponent.addVideoListener(componentListener);
        }

        if (newTextComponent != null) {
            newTextComponent.addTextOutput(componentListener);
        }

        contentFrame.addView(videoSurface, 0);
    }

    /**
     * Clear all the listeners and detach Surface from view hierarchy.
     */
    private void removeVideoSurface() {

        Player.VideoComponent oldVideoComponent = player.getVideoComponent();
        Player.TextComponent oldTextComponent = player.getTextComponent();
        if (playerEventListener != null) {
            player.removeListener(playerEventListener);
        }
        //Remove existed videoSurface from player.
        if (oldVideoComponent != null) {

            if (videoSurface instanceof SurfaceView) {
                oldVideoComponent.clearVideoSurfaceView((SurfaceView) videoSurface);
            } else if (videoSurface instanceof TextureView) {
                oldVideoComponent.clearVideoTextureView((TextureView) videoSurface);
            }

            //Clear listeners.
            oldVideoComponent.removeVideoListener(componentListener);
        }

        if (oldTextComponent != null) {
            oldTextComponent.removeTextOutput(componentListener);
        }

        contentFrame.removeView(videoSurface);

    }

    /**
     * Creates the actual surface on which video will be rendered.
     *
     * @param useTextureView - if should use {@link TextureView} for rendering
     */
    private void createVideoSurface(boolean useTextureView) {
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
    }

    @Override
    public void hideVideoSurface() {
        if (videoSurface == null || subtitleView == null) {
            return;
        }
        videoSurface.setVisibility(GONE);
        subtitleView.setVisibility(GONE);
    }

    @Override
    public void showVideoSurface() {
        if (videoSurface == null || subtitleView == null) {
            return;
        }
        videoSurface.setVisibility(VISIBLE);
        subtitleView.setVisibility(VISIBLE);
    }

    @Override
    public void hideVideoSubtitles() {
        if (subtitleView == null) {
            return;
        }
        subtitleView.setVisibility(GONE);
    }

    @Override
    public void showVideoSubtitles() {
        subtitleView.setVisibility(VISIBLE);
    }

    @Override
    public SubtitleView getSubtitleView() {
        return subtitleView;
    }

    @Override
    public void setArtworkViewVisibility(boolean visibility) {
        if (artworkDrawable != null) {
            artworkView.setVisibility(visibility ? VISIBLE : GONE);
        } else {
            artworkView.setVisibility(GONE);
        }
        if (shutterView != null) {
            shutterView.setVisibility(GONE);
        }
    }

    @Override
    public void setArtworkDrawable(Drawable artworkDrawable) {
        this.artworkDrawable = artworkDrawable;
        setArtworkDrawableView(artworkDrawable);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (videoSurface instanceof SurfaceView) {
            // Work around https://github.com/google/ExoPlayer/issues/3160.
            videoSurface.setVisibility(visibility);
        }
    }

    private void initContentFrame() {
        contentFrame = new AspectRatioFrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        contentFrame.setLayoutParams(params);
        addView(contentFrame);
    }

    private void initPosterView() {
        shutterView = new View(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        shutterView.setLayoutParams(params);
        shutterView.setBackgroundColor(Color.BLACK);
        contentFrame.addView(shutterView);
    }

    private void initArtworkView() {
        artworkView = new ImageView(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        artworkView.setLayoutParams(params);
        contentFrame.addView(artworkView);
        setArtworkViewVisibility(false);
    }

    private void initSubtitleLayout() {
        subtitleView = new SubtitleView(getContext());
        subtitleView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        subtitleView.setUserDefaultStyle();
        subtitleView.setUserDefaultTextSize();
        contentFrame.addView(subtitleView);
    }

    private void resetViews() {
        if (shutterView != null) {
            shutterView.setVisibility(VISIBLE);
        }
        if (subtitleView != null) {
            subtitleView.setCues(null);
        }
    }

    /**
     * Local listener implementation.
     */
    private final class ComponentListener implements TextOutput, VideoListener, OnLayoutChangeListener {

        @Override
        public void onCues(List<Cue> cues) {
            if (subtitleView != null) {
                subtitleView.onCues(cues);
            }
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            if (contentFrame == null) {
                return;
            }

            float videoAspectRatio =
                    (height == 0 || width == 0) ? 1 : (width * pixelWidthHeightRatio) / height;

            if (videoSurface instanceof TextureView) {
                // Try to apply rotation transformation when our surface is a TextureView.
                if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                    // We will apply a rotation 90/270 degree to the output texture of the TextureView.
                    // In this case, the output video's width and height will be swapped.
                    videoAspectRatio = 1 / videoAspectRatio;
                }
                if (textureViewRotation != 0) {
                    videoSurface.removeOnLayoutChangeListener(this);
                }
                textureViewRotation = unappliedRotationDegrees;
                if (textureViewRotation != 0) {
                    // The texture view's dimensions might be changed after layout step.
                    // So add an OnLayoutChangeListener to apply rotation after layout step.
                    videoSurface.addOnLayoutChangeListener(this);
                }
                applyTextureViewRotation((TextureView) videoSurface, textureViewRotation);
            }


            contentFrame.setResizeMode(resizeMode);
            contentFrame.setAspectRatio(videoAspectRatio);
        }

        @Override
        public void onRenderedFirstFrame() {
            if (shutterView != null) {
                shutterView.setVisibility(GONE);
            }
        }

        @Override
        public void onLayoutChange(
                View view,
                int left,
                int top,
                int right,
                int bottom,
                int oldLeft,
                int oldTop,
                int oldRight,
                int oldBottom) {
            applyTextureViewRotation((TextureView) view, textureViewRotation);
        }

        /**
         * Applies a texture rotation to a {@link TextureView}.
         */
        private void applyTextureViewRotation(TextureView textureView, int textureViewRotation) {
            float textureViewWidth = textureView.getWidth();
            float textureViewHeight = textureView.getHeight();
            if (textureViewWidth == 0 || textureViewHeight == 0 || textureViewRotation == 0) {
                textureView.setTransform(null);
            } else {
                Matrix transformMatrix = new Matrix();
                float pivotX = textureViewWidth / 2;
                float pivotY = textureViewHeight / 2;
                transformMatrix.postRotate(textureViewRotation, pivotX, pivotY);

                // After rotation, scale the rotated texture to fit the TextureView size.
                RectF originalTextureRect = new RectF(0, 0, textureViewWidth, textureViewHeight);
                RectF rotatedTextureRect = new RectF();
                transformMatrix.mapRect(rotatedTextureRect, originalTextureRect);
                transformMatrix.postScale(
                        textureViewWidth / rotatedTextureRect.width(),
                        textureViewHeight / rotatedTextureRect.height(),
                        pivotX,
                        pivotY);
                textureView.setTransform(transformMatrix);
            }
        }
    }

    @Override
    public void setSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        this.resizeMode = ExoPlayerView.getExoPlayerAspectRatioResizeMode(resizeMode);
        if (contentFrame != null) {
            contentFrame.setResizeMode(this.resizeMode);
        }
    }

    public static @AspectRatioFrameLayout.ResizeMode int getExoPlayerAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {

        @AspectRatioFrameLayout.ResizeMode int exoPlayerAspectRatioResizeMode;

        switch (resizeMode) {
            case fixedWidth:
                exoPlayerAspectRatioResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH;
                break;
            case fixedHeight:
                exoPlayerAspectRatioResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT;
                break;
            case fill:
                exoPlayerAspectRatioResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
                break;
            case zoom:
                exoPlayerAspectRatioResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                break;
            case fit:
            default:
                exoPlayerAspectRatioResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
                break;
        }
        return exoPlayerAspectRatioResizeMode;
    }

    private void setArtworkDrawableView(@Nullable Drawable drawable) {
        if (drawable != null) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            if (width > 0 && height > 0) {
                float artworkAspectRatio = (float) width / height;
                onContentAspectRatioChanged(artworkAspectRatio);
                artworkView.setImageDrawable(drawable);
            } else {
                setArtworkViewVisibility(false);
                log.e("Passed drawable for artwork view is not in the proper format.");
            }
        } else {
            setArtworkViewVisibility(false);
            log.d("Passed drawable for artwork view is null.");
        }
    }

    /**
     * Required to fit the artwork view to the content frame based on aspect ratio.
     * If this function is not there then the artwork view will fit and take the aspect ratio
     * of last content frame.
     * Called when there's a change in the aspect ratio of the content being displayed. The default
     * implementation sets the aspect ratio of the content frame to that of the content, unless the
     * content view is a {@link SphericalSurfaceView} in which case the frame's aspect ratio is
     * cleared [SphericalSurfaceView is used for VR]
     *
     * @param contentAspectRatio The aspect ratio of the content
     */
    private void onContentAspectRatioChanged(float contentAspectRatio) {
        if (contentFrame != null) {
            contentFrame.setAspectRatio(contentAspectRatio);
        } else {
            log.e("Content frame is null");
        }
    }
}

