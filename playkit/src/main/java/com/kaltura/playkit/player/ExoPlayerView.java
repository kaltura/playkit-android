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
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.kaltura.androidx.media3.exoplayer.ExoPlayer;
import com.kaltura.androidx.media3.common.Player;
import com.kaltura.androidx.media3.common.text.Cue;
import com.kaltura.androidx.media3.common.text.CueGroup;
import com.kaltura.androidx.media3.ui.AspectRatioFrameLayout;
import com.kaltura.androidx.media3.ui.SubtitleView;
import com.kaltura.androidx.media3.common.VideoSize;
import com.kaltura.playkit.PKLog;

import java.util.ArrayList;
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

    private ExoPlayer player;
    private ComponentListener componentListener;
    private Player.Listener playerEventListener;
    private int textureViewRotation;
    private @AspectRatioFrameLayout.ResizeMode int resizeMode;
    private PKSubtitlePosition subtitleViewPosition;
    private boolean isVideoViewVisible;
    private List<Cue> lastReportedCues;
    private boolean shutterStaysOnRenderedFirstFrame;

    private boolean usingSpeedAdjustedRenderer;

    ExoPlayerView(Context context, boolean shutterStaysOnRenderedFirstFrame) {
        this(context, null, shutterStaysOnRenderedFirstFrame);
    }

    ExoPlayerView(Context context, AttributeSet attrs, boolean shutterStaysOnRenderedFirstFrame) {
        this(context, attrs, 0, shutterStaysOnRenderedFirstFrame);
    }

    ExoPlayerView(Context context, AttributeSet attrs, int defStyleAttr, boolean shutterStaysOnRenderedFirstFrame) {
        super(context, attrs, defStyleAttr);
        componentListener = new ComponentListener();
        playerEventListener = getPlayerEventListener();
        initContentFrame();
        initSubtitleLayout();
        initPosterView();
        setShutterStaysOnRenderedFirstFrame(shutterStaysOnRenderedFirstFrame);
    }

    public void setShutterStaysOnRenderedFirstFrame(boolean shutterStaysOnRenderedFirstFrame) {
        this.shutterStaysOnRenderedFirstFrame = shutterStaysOnRenderedFirstFrame;
    }

    public void setUsingSpeedAdjustedRenderer(boolean usingSpeedAdjustedRenderer) {
        this.usingSpeedAdjustedRenderer = usingSpeedAdjustedRenderer;
    }

    @Override
    public void onRenderedFirstFrameWhenStarted() {
        log.d("onRenderedFirstFrameWhenStarted");
        if (shutterView != null) {
            shutterView.setVisibility(INVISIBLE);
        }
    }

    private boolean shouldHideShutterView() {
        return shutterView != null && !(shutterStaysOnRenderedFirstFrame && usingSpeedAdjustedRenderer);
    }

    @NonNull
    private Player.Listener getPlayerEventListener() {
        return new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_IDLE:
                        if (shutterStaysOnRenderedFirstFrame && usingSpeedAdjustedRenderer) {
                            if (shutterView != null) {
                                shutterView.setVisibility(VISIBLE);
                            }
                        }
                        break;

                    case Player.STATE_READY:
                        if (player != null && player.getPlayWhenReady()) {
                            log.d("ExoPlayerView READY. playWhenReady => true");
                            if (shouldHideShutterView()) {
                                shutterView.setVisibility(INVISIBLE);
                            }
                        }
                        break;

                    case Player.STATE_BUFFERING:
                    case Player.STATE_ENDED:
                    default:
                        break;
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                log.d("ExoPlayerView onIsPlayingChanged isPlaying = " + isPlaying);
                if (isPlaying && shouldHideShutterView()) {
                    shutterView.setVisibility(INVISIBLE);
                }
            }
        };
    }

    /**
     * Set the {@link ExoPlayer} to use. If ExoplayerView instance already has
     * player attached to it, it will remove and clear videoSurface first.
     *
     * @param player           - The {@link ExoPlayer} to use.
     * @param isSurfaceSecured - should allow secure rendering of the surface
     */
    @Override
    public void setPlayer(ExoPlayer player, boolean useTextureView, boolean isSurfaceSecured, boolean hideVideoViews) {
        if (this.player == player) {
            return;
        }

        if (this.player != null) {
            removeVideoSurface();
        }
        this.player = player;
        addVideoSurface(useTextureView, isSurfaceSecured, hideVideoViews);
    }

    /**
     * Swap the video surface view that player should render.
     *
     * @param useTextureView   - if should use {@link TextureView} for rendering
     * @param isSurfaceSecured - should allow secure rendering of the surface
     */
    @Override
    public void setVideoSurfaceProperties(boolean useTextureView, boolean isSurfaceSecured, boolean hideVideoViews) {
        if (player != null) {
            removeVideoSurface();
            addVideoSurface(useTextureView, isSurfaceSecured, hideVideoViews);
        }
    }

    /**
     * Create and set relevant surface and listeners to player and attach Surface to the view hierarchy.
     *
     * @param isSurfaceSecured - should allow secure rendering of the surface
     */

    private void addVideoSurface(boolean useTextureView, boolean isSurfaceSecured, boolean hideVideoViews) {
        resetViews();
        createVideoSurface(useTextureView);

        player.addListener(playerEventListener);

        //Decide which type of videoSurface should be set.
        if (videoSurface instanceof TextureView) {
            player.setVideoTextureView((TextureView) videoSurface);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ((SurfaceView) videoSurface).setSecure(isSurfaceSecured);
            }
            player.setVideoSurfaceView((SurfaceView) videoSurface);
        }
        //Set listeners
        player.addListener(componentListener);

        contentFrame.addView(videoSurface, 0);

        isVideoViewVisible = hideVideoViews;

        if (hideVideoViews) {
            videoSurface.setVisibility(GONE);
            shutterView.setVisibility(GONE);
        }
    }

    /**
     * Clear all the listeners and detach Surface from view hierarchy.
     */
    private void removeVideoSurface() {

        if (playerEventListener != null) {
            player.removeListener(playerEventListener);
        }
        //Remove existed videoSurface from player.
        if (videoSurface instanceof SurfaceView) {
            player.clearVideoSurfaceView((SurfaceView) videoSurface);
        } else if (videoSurface instanceof TextureView) {
            player.clearVideoTextureView((TextureView) videoSurface);
        }

        //Clear listeners.
        if (componentListener != null) {
            player.removeListener(componentListener);
        }

        lastReportedCues = null;
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

        if (!isVideoViewVisible) {
            videoSurface.setVisibility(VISIBLE);
        }
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
    public void toggleVideoViewVisibility(boolean isVisible) {
        this.isVideoViewVisible = isVisible;
        if (videoSurface != null) {
            videoSurface.setVisibility(isVisible ? GONE : VISIBLE);
        }
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
    public void applySubtitlesChanges() {
        if (subtitleView != null && lastReportedCues != null) {
            subtitleView.setCues(getModifiedSubtitlePosition(lastReportedCues, subtitleViewPosition));
        }
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
    private final class ComponentListener implements Player.Listener, OnLayoutChangeListener {

        @Override
        public void onCues(CueGroup cueGroup) {
            lastReportedCues = cueGroup.cues;
            List<Cue> cueList = null;
            if (subtitleViewPosition != null) {
                cueList = getModifiedSubtitlePosition(lastReportedCues, subtitleViewPosition);
            }

            if (subtitleView != null) {
                subtitleView.setCues((cueList != null && !cueList.isEmpty()) ? cueList : lastReportedCues);
            }
        }

        @Override
        public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
            if (contentFrame == null) {
                return;
            }

            float videoAspectRatio =
                    (videoSize.height == 0 || videoSize.width == 0) ? 1 : (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height;

            if (videoSurface instanceof TextureView) {
                // Try to apply rotation transformation when our surface is a TextureView.
                if (videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270) {
                    // We will apply a rotation 90/270 degree to the output texture of the TextureView.
                    // In this case, the output video's width and height will be swapped.
                    videoAspectRatio = 1 / videoAspectRatio;
                }
                if (textureViewRotation != 0) {
                    videoSurface.removeOnLayoutChangeListener(this);
                }
                textureViewRotation = videoSize.unappliedRotationDegrees;
                if (textureViewRotation != 0) {
                    // The texture view's dimensions might be changed after layout step.
                    // So add an OnLayoutChangeListener to apply rotation after layout step.
                    videoSurface.addOnLayoutChangeListener(this);
                }
                applyTextureViewRotation((TextureView) videoSurface, textureViewRotation);
            }


            contentFrame.setResizeMode(resizeMode);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) { // FEM-2651
                contentFrame.setAspectRatio(videoAspectRatio);
            }
        }

        @Override
        public void onRenderedFirstFrame() {
            if (shouldHideShutterView()) {
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
        this.resizeMode = PKAspectRatioResizeMode.getExoPlayerAspectRatioResizeMode(resizeMode);
        if (contentFrame != null) {
            contentFrame.setResizeMode(this.resizeMode);
        }
    }

    @Override
    public void setSubtitleViewPosition(PKSubtitlePosition subtitleViewPosition) {
        this.subtitleViewPosition = subtitleViewPosition;
    }

    /**
     * Creates new cue configuration if `isOverrideInlineCueConfig` is set to true by application
     * Checks if the application wants to ignore the in-stream CueSettings otherwise goes with existing Cue configuration
     *
     * @param cueList cue list coming in stream
     * @param subtitleViewPosition subtitle view position configuration set by application
     * @return List of modified Cues
     */
    public List<Cue> getModifiedSubtitlePosition(List<Cue> cueList, PKSubtitlePosition subtitleViewPosition) {
        if (subtitleViewPosition != null && cueList != null && !cueList.isEmpty()) {
            List<Cue> newCueList = new ArrayList<>();
            for (Cue cue : cueList) {
                if (!subtitleViewPosition.isOverrideInlineCueConfig()) {
                    newCueList.add(cue);
                    continue;
                }

                CharSequence text = cue.text;
                if (text != null) {
                    Cue newCue = new Cue.Builder()
                            .setText(text)
                            .setTextAlignment(subtitleViewPosition.getSubtitleHorizontalPosition())
                            .setLine(subtitleViewPosition.getVerticalPositionPercentage(), subtitleViewPosition.getLineType())
                            .setSize(subtitleViewPosition.getHorizontalPositionPercentage())
                            .build();

                    newCueList.add(newCue);
                }
            }
            return newCueList;
        }

        return cueList;
    }
}

