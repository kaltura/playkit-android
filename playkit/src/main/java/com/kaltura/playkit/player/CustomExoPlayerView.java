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
 * Created by anton.afanasiev on 13/11/2016.
 */

public class CustomExoPlayerView extends FrameLayout implements SimpleExoPlayer.VideoListener, TextRenderer.Output{

    private static final String TAG = CustomExoPlayerView.class.getSimpleName();

    private final View surfaceView;
    private final View shutterView;
    private final SubtitleView subtitleLayout;
    private final AspectRatioFrameLayout layout;

    private SimpleExoPlayer player;

    public CustomExoPlayerView(Context context) {
        this(context, null);
    }

    public CustomExoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomExoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        layout = initFrameLayout();
        shutterView = initShutterView();
        subtitleLayout = initSubtitleLayout();

        surfaceView = initSurfaceView();

        addView(layout);
        layout.addView(surfaceView, 0);
        layout.addView(shutterView);
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

    private View initShutterView() {
        View shutterView = new View(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        shutterView.setLayoutParams(params);
        shutterView.setBackgroundColor(Color.BLACK);

        return shutterView;
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
            showShutter(true);
        } else {
            shutterView.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        layout.setAspectRatio(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
    }

    @Override
    public void onRenderedFirstFrame() {
    }

    @Override
    public void onVideoTracksDisabled() {
        shutterView.setVisibility(VISIBLE);
    }

    @Override
    public void onCues(List<Cue> cues) {
        subtitleLayout.onCues(cues);
    }

    public void showShutter(boolean doShow) {
        if(doShow){
            surfaceView.setVisibility(INVISIBLE);
            shutterView.setVisibility(VISIBLE);
        }else{
            surfaceView.setVisibility(VISIBLE);
            shutterView.setVisibility(INVISIBLE);
        }
    }
}

