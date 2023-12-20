package com.kaltura.playkit.player;

import com.kaltura.androidx.media3.ui.AspectRatioFrameLayout;

public enum PKAspectRatioResizeMode {
    fit,
    fixedWidth,
    fixedHeight,
    fill,
    zoom;

    public static @AspectRatioFrameLayout.ResizeMode int getExoPlayerAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        @AspectRatioFrameLayout.ResizeMode int exoPlayerAspectRatioResizeMode;
        switch(resizeMode) {
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
}
