package com.kaltura.playkit.player;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.Renderer;
import com.kaltura.android.exoplayer2.drm.DrmSessionManager;
import com.kaltura.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.kaltura.android.exoplayer2.video.CustomVideoCodecRenderer;
import com.kaltura.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;

/**
 * Created by anton.afanasiev on 25/02/2018.
 */

public class CustomRendererFactory extends DefaultRenderersFactory {
    private boolean allowClearLead;

    public CustomRendererFactory(Context context, int extensionRendererMode, boolean allowClearLead) {
        super(context, extensionRendererMode);
        this.allowClearLead = allowClearLead;
    }

    @Override
    protected void buildVideoRenderers(Context context, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, long allowedVideoJoiningTimeMs, Handler eventHandler, VideoRendererEventListener eventListener, int extensionRendererMode, ArrayList<Renderer> out) {

        out.add(new CustomVideoCodecRenderer(context, MediaCodecSelector.DEFAULT,
                allowedVideoJoiningTimeMs, drmSessionManager, allowClearLead, eventHandler, eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY));
    }

    @Override
    protected void buildAudioRenderers(Context context, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, AudioProcessor[] audioProcessors, Handler eventHandler, AudioRendererEventListener eventListener, int extensionRendererMode, ArrayList<Renderer> out) {

        out.add(
                new MediaCodecAudioRenderer(
                        context,
                        MediaCodecSelector.DEFAULT,
                        drmSessionManager,
                        allowClearLead,
                        eventHandler,
                        eventListener,
                        AudioCapabilities.getCapabilities(context),
                        audioProcessors));
    }
}

