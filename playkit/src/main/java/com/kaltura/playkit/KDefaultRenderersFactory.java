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
    private static final int DEFAULT_INIT_CODEC_FAILURE_RETRY_TIMEOUT_MS = 100;
    private static final int DEFAULT_INIT_CODEC_FAILURE_RETRY_COUNT = 10;

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
                                                for(int i = 0; i < DEFAULT_INIT_CODEC_FAILURE_RETRY_COUNT; i++) {
                                                    log.d("Retrying on coded init failure (" + i + ")");
                                                    super.onReset();
                                                    try {
                                                        Thread.sleep(DEFAULT_INIT_CODEC_FAILURE_RETRY_TIMEOUT_MS);
                                                    } catch (Exception e1) {
                                                        log.d("Interrupted while sleeping: " + e1.getMessage());
                                                        e1.printStackTrace();
                                                    }
                                                    try {
                                                        super.render(positionUs, elapsedRealtimeUs);
                                                        log.d("Retrying on coded init failure successful");
                                                        // Stop retrying if no exception was thrown
                                                        break;
                                                    } catch (ExoPlaybackException e2) {
                                                        if (e2.errorCode != PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                                                            || i == DEFAULT_INIT_CODEC_FAILURE_RETRY_COUNT - 1) {
                                                            // Some other error happened or last retry. Throw exception to the caller
                                                            log.d("Codec init retry failed: " + e2.getMessage());
                                                            throw e2;
                                                        }
                                                    }
                                                }
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
