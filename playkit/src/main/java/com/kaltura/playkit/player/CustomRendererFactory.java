package com.kaltura.playkit.player;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.Nullable;


import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.Renderer;
import com.kaltura.android.exoplayer2.drm.DrmSessionManager;
import com.kaltura.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.kaltura.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.kaltura.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;

public class CustomRendererFactory extends DefaultRenderersFactory {

    public CustomRendererFactory(Context context, boolean allowClearLead, boolean enableDecoderFallback, long allowedVideoJoiningTimeMs) {
        super(context);
        setAllowedVideoJoiningTimeMs(allowedVideoJoiningTimeMs);
        setPlayClearSamplesWithoutKeys(allowClearLead);
        setEnableDecoderFallback(enableDecoderFallback);
    }

    @Override
    protected void buildVideoRenderers(Context context, int extensionRendererMode, MediaCodecSelector mediaCodecSelector, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, boolean enableDecoderFallback, Handler eventHandler, VideoRendererEventListener eventListener, long allowedVideoJoiningTimeMs, ArrayList<Renderer> out) {
        out.add(new MediaCodecVideoRenderer(context, MediaCodecSelector.DEFAULT,
                allowedVideoJoiningTimeMs, drmSessionManager, playClearSamplesWithoutKeys, enableDecoderFallback, eventHandler, eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY));
    }
}
