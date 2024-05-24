package com.kaltura.playkit;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.ExoPlaybackException;
import com.kaltura.android.exoplayer2.PlaybackException;
import com.kaltura.android.exoplayer2.Renderer;
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.kaltura.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.kaltura.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;

public class KDefaultRenderersFactory {
    private static final PKLog log = PKLog.get("KDefaultRenderersFactory");
    public static DefaultRenderersFactory createDecoderInitErrorRetryFactory(
            Context context
    ) {
        return new DefaultRenderersFactory(context) {
            @Override
            protected void buildVideoRenderers(@NonNull Context context,
                                               int extensionRendererMode,
                                               @NonNull MediaCodecSelector mediaCodecSelector,
                                               boolean enableDecoderFallback,
                                               @NonNull Handler eventHandler,
                                               @NonNull VideoRendererEventListener eventListener,
                                               long allowedVideoJoiningTimeMs,
                                               @NonNull ArrayList<Renderer> out) {
                ArrayList<Renderer> renderersArrayList = new ArrayList<>();
                super.buildVideoRenderers(context,
                        extensionRendererMode,
                        mediaCodecSelector,
                        enableDecoderFallback,
                        eventHandler,
                        eventListener,
                        allowedVideoJoiningTimeMs,
                        renderersArrayList);
                for (Renderer renderer : renderersArrayList) {
                    if (renderer instanceof MediaCodecVideoRenderer) {
                        out.add(new MediaCodecVideoRenderer(
                                context,
                                this.getCodecAdapterFactory(),
                                mediaCodecSelector,
                                allowedVideoJoiningTimeMs,
                                enableDecoderFallback,
                                eventHandler,
                                eventListener,
                                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY) {
                                    @Override
                                    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
                                        try {
                                            super.render(positionUs, elapsedRealtimeUs);
                                        } catch (ExoPlaybackException e) {
                                            if (e.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                                                // try one more time
                                                try {
                                                    Thread.sleep(50);
                                                } catch (Exception e1) {
                                                    log.d("Interrupted while sleeping: " + e.getMessage());
                                                    e.printStackTrace();
                                                }
                                                super.render(positionUs, elapsedRealtimeUs);
                                            }
                                        }
                                    }
                                }
                        );
                    } else {
                        out.add(renderer);
                    }
                }
            }
        };
    }
}
