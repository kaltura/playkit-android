package com.kaltura.playkit.player;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
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

public class CustomExoPlayerView extends FrameLayout {

    private static final String TAG = CustomExoPlayerView.class.getSimpleName();
    private final View surfaceView;
    private final View shutterView; // TODO should be changed to poster?
    private final SubtitleView subtitleLayout;
    private final AspectRatioFrameLayout layout;
    private final CustomExoPlayerView.ComponentListener componentListener;

    private SimpleExoPlayer player;

    public CustomExoPlayerView(Context context) {
        this(context, null);
    }

    public CustomExoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomExoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(com.google.android.exoplayer2.R.layout.exo_simple_player_view, this);
        componentListener = new CustomExoPlayerView.ComponentListener();
        layout = (AspectRatioFrameLayout) findViewById(com.google.android.exoplayer2.R.id.video_frame);
        shutterView = findViewById(com.google.android.exoplayer2.R.id.shutter);
        subtitleLayout = (SubtitleView) findViewById(com.google.android.exoplayer2.R.id.subtitles);
        subtitleLayout.setUserDefaultStyle();
        subtitleLayout.setUserDefaultTextSize();

        View view = new SurfaceView(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);
        surfaceView = view;
        layout.addView(surfaceView, 0);
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
            player.setVideoListener(componentListener);
            player.setTextOutput(componentListener);
        } else {
            shutterView.setVisibility(VISIBLE);
        }
    }

    private final class ComponentListener implements SimpleExoPlayer.VideoListener,
            TextRenderer.Output{

        // TextRenderer.Output implementation

        @Override
        public void onCues(List<Cue> cues) {
            subtitleLayout.onCues(cues);
        }

        // SimpleExoPlayer.VideoListener implementation

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                       float pixelWidthHeightRatio) {
            layout.setAspectRatio(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
        }

        @Override
        public void onRenderedFirstFrame() {
            shutterView.setVisibility(GONE);
        }

        @Override
        public void onVideoTracksDisabled() {
            shutterView.setVisibility(VISIBLE);
        }

    }

}

