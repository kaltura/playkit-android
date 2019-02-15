package com.kaltura.playkit.player;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.CustomVideoCodecRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;

/**
 * Created by anton.afanasiev on 25/02/2018.
 */

public class CustomRendererFactory extends DefaultRenderersFactory {

    private boolean allowClearLead;
    private long allowedVideoJoiningTimeMs;

    public CustomRendererFactory(Context context, boolean allowClearLead, long allowedVideoJoiningTimeMs) {
        super(context);
        this.allowClearLead = allowClearLead;
        this.allowedVideoJoiningTimeMs = allowedVideoJoiningTimeMs;
    }

    @Override
    protected void buildVideoRenderers(Context context, int extensionRendererMode, MediaCodecSelector mediaCodecSelector, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, VideoRendererEventListener eventListener, long allowedVideoJoiningTimeMs, ArrayList<Renderer> out) {
        out.add(new CustomVideoCodecRenderer(context, MediaCodecSelector.DEFAULT,
                this.allowedVideoJoiningTimeMs, drmSessionManager, this.allowClearLead, eventHandler, eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY));
    }

    @Override
    protected void buildAudioRenderers(Context context, int extensionRendererMode, MediaCodecSelector mediaCodecSelector, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, AudioProcessor[] audioProcessors, Handler eventHandler, AudioRendererEventListener eventListener, ArrayList<Renderer> out) {
        out.add(
                new MediaCodecAudioRenderer(
                        context,
                        MediaCodecSelector.DEFAULT,
                        drmSessionManager,
                        this.allowClearLead,
                        eventHandler,
                        eventListener,
                        AudioCapabilities.getCapabilities(context),
                        audioProcessors));
    }
}
