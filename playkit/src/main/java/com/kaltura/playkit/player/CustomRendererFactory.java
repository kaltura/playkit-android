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

    public CustomRendererFactory(Context context, boolean allowClearLead) {
        super(context);
        setPlayClearSamplesWithoutKeys(allowClearLead);
    }

    @Override
    public DefaultRenderersFactory setPlayClearSamplesWithoutKeys(boolean playClearSamplesWithoutKeys) {
        return super.setPlayClearSamplesWithoutKeys(playClearSamplesWithoutKeys);
    }

    @Override
    public DefaultRenderersFactory setExtensionRendererMode(int extensionRendererMode) {
        return super.setExtensionRendererMode(extensionRendererMode);
    }

    /**
     * Default maximum duration (5000 ms) for which a video renderer can attempt to seamlessly join an ongoing playback.
     */
    @Override
    public DefaultRenderersFactory setAllowedVideoJoiningTimeMs(long allowedVideoJoiningTimeMs) {
        return super.setAllowedVideoJoiningTimeMs(allowedVideoJoiningTimeMs);
    }
}
