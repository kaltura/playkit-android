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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.kaltura.playkit.PKLog;

/**
 * Created by gilad.nadav on 01/01/2017.
 */

public class MediaPlayerView extends PlayerView implements SurfaceHolder.Callback {

    private static final PKLog log = PKLog.get("MediaPlayerView");
    private Context context;
    private SurfaceHolder holder;
    private SurfaceView surfaceView;
    private View posterView;


    public MediaPlayerView(Context context) {
        this(context, null);
    }

    public MediaPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initSurfaceView();
    }

    public SurfaceHolder getSurfaceHolder() {
        return  holder;
    }

    private View initSurfaceView() {
        log.d("initSurfaceView");
        surfaceView = new SurfaceView(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        surfaceView.setLayoutParams(params);
        posterView = initPosterView();
        addView(surfaceView, params);
        addView(posterView);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        return surfaceView;
    }

    @Override
    public void hideVideoSurface() {
        surfaceView.setVisibility(GONE);
    }

    @Override
    public void showVideoSurface() {
        surfaceView.setVisibility(VISIBLE);
    }

    @Override
    public void hideVideoSubtitles() {

    }

    @Override
    public void showVideoSubtitles() {

    }

    private View initPosterView() {
        View posterView = new View(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        posterView.setLayoutParams(params);
        posterView.setBackgroundColor(Color.BLACK);

        return posterView;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        log.d("surfaceCreated");
        posterView.setVisibility(GONE);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        // Do Nothing
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // Do Nothing
    }
}
