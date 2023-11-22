/*
 * ============================================================================
 * Copyright (C) 2023 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.Renderer;
import com.kaltura.android.exoplayer2.audio.AudioRendererEventListener;
import com.kaltura.android.exoplayer2.audio.AudioSink;
import com.kaltura.android.exoplayer2.audio.KMediaCodecAudioRenderer;
import com.kaltura.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.kaltura.android.exoplayer2.video.KMediaCodecVideoRenderer;
import com.kaltura.android.exoplayer2.video.KVideoRendererFirstFrameWhenStartedEventListener;
import com.kaltura.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.kaltura.android.exoplayer2.video.VideoRendererEventListener;
import com.kaltura.playkit.player.PlayerSettings;

import java.util.ArrayList;

/**
 * Utility class, providing a mechanism for creating renderers factory, which in its turn
 * creates Audio/Video renderers, which are capable to adjust playback speed, in case when
 * there's a big gap between Audio and Video streams buffers position at playback startup.
 * Also, Video renderer takes a callback interface for providing notification once playback
 * actually begins (i.e. in addition to onFirstFrameRendered, when no playback is actually
 * happening yet).
 * Speed adjustment behavioral values may be provided inside the {@link com.kaltura.playkit.player.PlayerSettings}
 * instance passed into factory method
 * Currently this mechanism is used only for multicast streams
 */
public class SpeedAdjustedRenderersFactory {
    public static DefaultRenderersFactory createSpeedAdjustedRenderersFactory(
            Context context,
            PlayerSettings playerSettings,
            KVideoRendererFirstFrameWhenStartedEventListener rendererFirstFrameWhenStartedEventListener
    ) {
        return new DefaultRenderersFactory(context) {
            @Override
            protected void buildAudioRenderers(@NonNull Context context,
                                               int extensionRendererMode,
                                               @NonNull MediaCodecSelector mediaCodecSelector,
                                               boolean enableDecoderFallback,
                                               @NonNull AudioSink audioSink,
                                               @NonNull Handler eventHandler,
                                               @NonNull AudioRendererEventListener eventListener,
                                               @NonNull ArrayList<Renderer> out) {
                ArrayList<Renderer> renderersArrayList = new ArrayList<>();
                super.buildAudioRenderers(context,
                        extensionRendererMode,
                        mediaCodecSelector,
                        enableDecoderFallback,
                        audioSink,
                        eventHandler,
                        eventListener,
                        renderersArrayList);
                for (Renderer renderer : renderersArrayList) {
                    if (renderer instanceof MediaCodecAudioRenderer) {
                        if (playerSettings.getMulticastSettings() != null) {
                            out.add(new KMediaCodecAudioRenderer(
                                    context,
                                    getCodecAdapterFactory(),
                                    mediaCodecSelector,
                                    enableDecoderFallback,
                                    eventHandler,
                                    eventListener,
                                    audioSink,
                                    playerSettings.getMulticastSettings().getExperimentalMaxAudioGapThreshold(),
                                    playerSettings.getMulticastSettings().getExperimentalMaxSpeedFactor(),
                                    playerSettings.getMulticastSettings().getExperimentalSpeedStep(),
                                    playerSettings.getMulticastSettings().getExperimentalAVGapForSpeedAdjustment(),
                                    playerSettings.getMulticastSettings().getExperimentalContinuousSpeedAdjustment()));
                        } else {
                            out.add(new KMediaCodecAudioRenderer(
                                    context,
                                    getCodecAdapterFactory(),
                                    mediaCodecSelector,
                                    enableDecoderFallback,
                                    eventHandler,
                                    eventListener,
                                    audioSink));
                        }
                    } else {
                        out.add(renderer);
                    }
                }
            }

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
                        out.add(new KMediaCodecVideoRenderer(
                                context,
                                this.getCodecAdapterFactory(),
                                mediaCodecSelector,
                                allowedVideoJoiningTimeMs,
                                enableDecoderFallback,
                                eventHandler,
                                eventListener,
                                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY,
                                rendererFirstFrameWhenStartedEventListener));
                    } else {
                        out.add(renderer);
                    }
                }
            }
        };
    }
}
