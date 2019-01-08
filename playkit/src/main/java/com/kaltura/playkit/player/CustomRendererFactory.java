package com.kaltura.playkit.player;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.google.android.kexoplayer2.DefaultRenderersFactory;
import com.google.android.kexoplayer2.Renderer;
import com.google.android.kexoplayer2.drm.DrmSessionManager;
import com.google.android.kexoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.kexoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.kexoplayer2.video.CustomVideoCodecRenderer;
import com.google.android.kexoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;

/**
 * Created by anton.afanasiev on 25/02/2018.
 */

public class CustomRendererFactory extends DefaultRenderersFactory {

    public CustomRendererFactory(Context context, int extensionRendererMode) {
        super(context, extensionRendererMode);
    }

    @Override
    protected void buildVideoRenderers(Context context, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, long allowedVideoJoiningTimeMs, Handler eventHandler, VideoRendererEventListener eventListener, int extensionRendererMode, ArrayList<Renderer> out) {
        out.add(new CustomVideoCodecRenderer(context, MediaCodecSelector.DEFAULT,
                allowedVideoJoiningTimeMs, drmSessionManager, false, eventHandler, eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY));
    }
}
