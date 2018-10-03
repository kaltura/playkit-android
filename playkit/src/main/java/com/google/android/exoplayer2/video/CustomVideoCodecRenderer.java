/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.video;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.media.MediaCodec;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlayerMessage.Target;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.mediacodec.MediaFormatUtil;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.TraceUtil;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener.EventDispatcher;
import com.kaltura.playkit.player.DummySurfaceWorkaroundTest;

import java.nio.ByteBuffer;

/**
 * Decodes and renders video using {@link MediaCodec}.
 *
 * <p>This renderer accepts the following messages sent via {@link ExoPlayer#createMessage(Target)}
 * on the playback thread:
 *
 * <ul>
 * <li>Message with type {@link C#MSG_SET_SURFACE} to set the output surface. The message payload
 * should be the target {@link Surface}, or null.
 * <li>Message with type {@link C#MSG_SET_SCALING_MODE} to set the video scaling mode. The message
 * payload should be one of the integer scaling modes in {@link C.VideoScalingMode}. Note that
 * the scaling mode only applies if the {@link Surface} targeted by this renderer is owned by
 * a {@link android.view.SurfaceView}.
 * </ul>
 */
@TargetApi(16)
public class CustomVideoCodecRenderer extends MediaCodecRenderer {

    private static final String TAG = "MediaCodecVideoRenderer";
    private static final String KEY_CROP_LEFT = "crop-left";
    private static final String KEY_CROP_RIGHT = "crop-right";
    private static final String KEY_CROP_BOTTOM = "crop-bottom";
    private static final String KEY_CROP_TOP = "crop-top";

    // Long edge length in pixels for standard video formats, in decreasing in order.
    private static final int[] STANDARD_LONG_EDGE_VIDEO_PX = new int[]{
            1920, 1600, 1440, 1280, 960, 854, 640, 540, 480};

    // Generally there is zero or one pending output stream offset. We track more offsets to allow for
    // pending output streams that have fewer frames than the codec latency.
    private static final int MAX_PENDING_OUTPUT_STREAM_OFFSET_COUNT = 10;

    private final Context context;
    private final VideoFrameReleaseTimeHelper frameReleaseTimeHelper;
    private final EventDispatcher eventDispatcher;
    private final long allowedJoiningTimeMs;
    private final int maxDroppedFramesToNotify;
    private final boolean deviceNeedsAutoFrcWorkaround;
    private final long[] pendingOutputStreamOffsetsUs;
    private final long[] pendingOutputStreamSwitchTimesUs;

    private CodecMaxValues codecMaxValues;
    private boolean codecNeedsSetOutputSurfaceWorkaround;

    private Surface surface;
    private Surface dummySurface;
    @C.VideoScalingMode
    private int scalingMode;
    private boolean renderedFirstFrame;
    private long initialPositionUs;
    private long joiningDeadlineMs;
    private long droppedFrameAccumulationStartTimeMs;
    private int droppedFrames;
    private int consecutiveDroppedFrameCount;
    private int buffersInCodecCount;
    private long lastRenderTimeUs;

    private int pendingRotationDegrees;
    private float pendingPixelWidthHeightRatio;
    private int currentWidth;
    private int currentHeight;
    private int currentUnappliedRotationDegrees;
    private float currentPixelWidthHeightRatio;
    private int reportedWidth;
    private int reportedHeight;
    private int reportedUnappliedRotationDegrees;
    private float reportedPixelWidthHeightRatio;

    private boolean tunneling;
    private int tunnelingAudioSessionId;
    /* package */ OnFrameRenderedListenerV23 tunnelingOnFrameRenderedListener;

    private long lastInputTimeUs;
    private long outputStreamOffsetUs;
    private int pendingOutputStreamOffsetCount;

    /**
     * @param context            A context.
     * @param mediaCodecSelector A decoder selector.
     */
    public CustomVideoCodecRenderer(Context context, MediaCodecSelector mediaCodecSelector) {
        this(context, mediaCodecSelector, 0);
    }

    /**
     * @param context              A context.
     * @param mediaCodecSelector   A decoder selector.
     * @param allowedJoiningTimeMs The maximum duration in milliseconds for which this video renderer
     *                             can attempt to seamlessly join an ongoing playback.
     */
    public CustomVideoCodecRenderer(Context context, MediaCodecSelector mediaCodecSelector,
                                    long allowedJoiningTimeMs) {
        this(
                context,
                mediaCodecSelector,
                allowedJoiningTimeMs,
                /* eventHandler= */ null,
                /* eventListener= */ null,
                -1);
    }

    /**
     * @param context                      A context.
     * @param mediaCodecSelector           A decoder selector.
     * @param allowedJoiningTimeMs         The maximum duration in milliseconds for which this video renderer
     *                                     can attempt to seamlessly join an ongoing playback.
     * @param eventHandler                 A handler to use when delivering events to {@code eventListener}. May be
     *                                     null if delivery of events is not required.
     * @param eventListener                A listener of events. May be null if delivery of events is not required.
     * @param maxDroppedFrameCountToNotify The maximum number of frames that can be dropped between
     *                                     invocations of {@link VideoRendererEventListener#onDroppedFrames(int, long)}.
     */
    public CustomVideoCodecRenderer(Context context, MediaCodecSelector mediaCodecSelector,
                                    long allowedJoiningTimeMs, @Nullable Handler eventHandler,
                                    @Nullable VideoRendererEventListener eventListener, int maxDroppedFrameCountToNotify) {
        this(
                context,
                mediaCodecSelector,
                allowedJoiningTimeMs,
                /* drmSessionManager= */ null,
                /* playClearSamplesWithoutKeys= */ false,
                eventHandler,
                eventListener,
                maxDroppedFrameCountToNotify);
    }

    /**
     * @param context                     A context.
     * @param mediaCodecSelector          A decoder selector.
     * @param allowedJoiningTimeMs        The maximum duration in milliseconds for which this video renderer
     *                                    can attempt to seamlessly join an ongoing playback.
     * @param drmSessionManager           For use with encrypted content. May be null if support for encrypted
     *                                    content is not required.
     * @param playClearSamplesWithoutKeys Encrypted media may contain clear (un-encrypted) regions.
     *                                    For example a media file may start with a short clear region so as to allow playback to
     *                                    begin in parallel with key acquisition. This parameter specifies whether the renderer is
     *                                    permitted to play clear regions of encrypted media files before {@code drmSessionManager}
     *                                    has obtained the keys necessary to decrypt encrypted regions of the media.
     * @param eventHandler                A handler to use when delivering events to {@code eventListener}. May be
     *                                    null if delivery of events is not required.
     * @param eventListener               A listener of events. May be null if delivery of events is not required.
     * @param maxDroppedFramesToNotify    The maximum number of frames that can be dropped between
     *                                    invocations of {@link VideoRendererEventListener#onDroppedFrames(int, long)}.
     */
    public CustomVideoCodecRenderer(Context context, MediaCodecSelector mediaCodecSelector,
                                    long allowedJoiningTimeMs,
                                    @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                    boolean playClearSamplesWithoutKeys, @Nullable Handler eventHandler,
                                    @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(C.TRACK_TYPE_VIDEO, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys);
        this.allowedJoiningTimeMs = allowedJoiningTimeMs;
        this.maxDroppedFramesToNotify = maxDroppedFramesToNotify;
        this.context = context.getApplicationContext();
        frameReleaseTimeHelper = new VideoFrameReleaseTimeHelper(this.context);
        eventDispatcher = new EventDispatcher(eventHandler, eventListener);
        deviceNeedsAutoFrcWorkaround = deviceNeedsAutoFrcWorkaround();
        pendingOutputStreamOffsetsUs = new long[MAX_PENDING_OUTPUT_STREAM_OFFSET_COUNT];
        pendingOutputStreamSwitchTimesUs = new long[MAX_PENDING_OUTPUT_STREAM_OFFSET_COUNT];
        outputStreamOffsetUs = C.TIME_UNSET;
        lastInputTimeUs = C.TIME_UNSET;
        joiningDeadlineMs = C.TIME_UNSET;
        currentWidth = Format.NO_VALUE;
        currentHeight = Format.NO_VALUE;
        currentPixelWidthHeightRatio = Format.NO_VALUE;
        pendingPixelWidthHeightRatio = Format.NO_VALUE;
        scalingMode = C.VIDEO_SCALING_MODE_DEFAULT;
        clearReportedVideoSize();
    }

    @Override
    protected int supportsFormat(MediaCodecSelector mediaCodecSelector,
                                 DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, Format format)
            throws DecoderQueryException {
        String mimeType = format.sampleMimeType;
        if (!MimeTypes.isVideo(mimeType)) {
            return FORMAT_UNSUPPORTED_TYPE;
        }
        boolean requiresSecureDecryption = false;
        DrmInitData drmInitData = format.drmInitData;
        if (drmInitData != null) {
            for (int i = 0; i < drmInitData.schemeDataCount; i++) {
                requiresSecureDecryption |= drmInitData.get(i).requiresSecureDecryption;
            }
        }
        MediaCodecInfo decoderInfo = mediaCodecSelector.getDecoderInfo(mimeType,
                requiresSecureDecryption);
        if (decoderInfo == null) {
            return requiresSecureDecryption && mediaCodecSelector.getDecoderInfo(mimeType, false) != null
                    ? FORMAT_UNSUPPORTED_DRM : FORMAT_UNSUPPORTED_SUBTYPE;
        }
        if (!supportsFormatDrm(drmSessionManager, drmInitData)) {
            return FORMAT_UNSUPPORTED_DRM;
        }
        boolean decoderCapable = decoderInfo.isCodecSupported(format.codecs);
        if (decoderCapable && format.width > 0 && format.height > 0) {
            if (Util.SDK_INT >= 21) {
                decoderCapable = decoderInfo.isVideoSizeAndRateSupportedV21(format.width, format.height,
                        format.frameRate);
            } else {
                decoderCapable = format.width * format.height <= MediaCodecUtil.maxH264DecodableFrameSize();
                if (!decoderCapable) {
                    Log.d(TAG, "FalseCheck [legacyFrameSize, " + format.width + "x" + format.height + "] ["
                            + Util.DEVICE_DEBUG_INFO + "]");
                }
            }
        }

        int adaptiveSupport = decoderInfo.adaptive ? ADAPTIVE_SEAMLESS : ADAPTIVE_NOT_SEAMLESS;
        int tunnelingSupport = decoderInfo.tunneling ? TUNNELING_SUPPORTED : TUNNELING_NOT_SUPPORTED;
        int formatSupport = decoderCapable ? FORMAT_HANDLED : FORMAT_EXCEEDS_CAPABILITIES;
        return adaptiveSupport | tunnelingSupport | formatSupport;
    }

    @Override
    protected void onEnabled(boolean joining) throws ExoPlaybackException {
        super.onEnabled(joining);
        tunnelingAudioSessionId = getConfiguration().tunnelingAudioSessionId;
        tunneling = tunnelingAudioSessionId != C.AUDIO_SESSION_ID_UNSET;
        eventDispatcher.enabled(decoderCounters);
        frameReleaseTimeHelper.enable();
    }

    @Override
    protected void onStreamChanged(Format[] formats, long offsetUs) throws ExoPlaybackException {
        if (outputStreamOffsetUs == C.TIME_UNSET) {
            outputStreamOffsetUs = offsetUs;
        } else {
            if (pendingOutputStreamOffsetCount == pendingOutputStreamOffsetsUs.length) {
                Log.w(TAG, "Too many stream changes, so dropping offset: "
                        + pendingOutputStreamOffsetsUs[pendingOutputStreamOffsetCount - 1]);
            } else {
                pendingOutputStreamOffsetCount++;
            }
            pendingOutputStreamOffsetsUs[pendingOutputStreamOffsetCount - 1] = offsetUs;
            pendingOutputStreamSwitchTimesUs[pendingOutputStreamOffsetCount - 1] = lastInputTimeUs;
        }
        super.onStreamChanged(formats, offsetUs);
    }

    @Override
    protected void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        super.onPositionReset(positionUs, joining);
        clearRenderedFirstFrame();
        initialPositionUs = C.TIME_UNSET;
        consecutiveDroppedFrameCount = 0;
        lastInputTimeUs = C.TIME_UNSET;
        if (pendingOutputStreamOffsetCount != 0) {
            outputStreamOffsetUs = pendingOutputStreamOffsetsUs[pendingOutputStreamOffsetCount - 1];
            pendingOutputStreamOffsetCount = 0;
        }
        if (joining) {
            setJoiningDeadlineMs();
        } else {
            joiningDeadlineMs = C.TIME_UNSET;
        }
    }

    @Override
    public boolean isReady() {
        if (super.isReady() && (renderedFirstFrame || (dummySurface != null && surface == dummySurface)
                || getCodec() == null || tunneling)) {
            // Ready. If we were joining then we've now joined, so clear the joining deadline.
            joiningDeadlineMs = C.TIME_UNSET;
            return true;
        } else if (joiningDeadlineMs == C.TIME_UNSET) {
            // Not joining.
            return false;
        } else if (SystemClock.elapsedRealtime() < joiningDeadlineMs) {
            // Joining and still within the joining deadline.
            return true;
        } else {
            // The joining deadline has been exceeded. Give up and clear the deadline.
            joiningDeadlineMs = C.TIME_UNSET;
            return false;
        }
    }

    @Override
    protected void onStarted() {
        super.onStarted();
        droppedFrames = 0;
        droppedFrameAccumulationStartTimeMs = SystemClock.elapsedRealtime();
        lastRenderTimeUs = SystemClock.elapsedRealtime() * 1000;
    }

    @Override
    protected void onStopped() {
        joiningDeadlineMs = C.TIME_UNSET;
        maybeNotifyDroppedFrames();
        super.onStopped();
    }

    @Override
    protected void onDisabled() {
        currentWidth = Format.NO_VALUE;
        currentHeight = Format.NO_VALUE;
        currentPixelWidthHeightRatio = Format.NO_VALUE;
        pendingPixelWidthHeightRatio = Format.NO_VALUE;
        outputStreamOffsetUs = C.TIME_UNSET;
        lastInputTimeUs = C.TIME_UNSET;
        pendingOutputStreamOffsetCount = 0;
        clearReportedVideoSize();
        clearRenderedFirstFrame();
        frameReleaseTimeHelper.disable();
        tunnelingOnFrameRenderedListener = null;
        tunneling = false;
        try {
            super.onDisabled();
        } finally {
            decoderCounters.ensureUpdated();
            eventDispatcher.disabled(decoderCounters);
        }
    }

    @Override
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        if (messageType == C.MSG_SET_SURFACE) {
            setSurface((Surface) message);
        } else if (messageType == C.MSG_SET_SCALING_MODE) {
            scalingMode = (Integer) message;
            MediaCodec codec = getCodec();
            if (codec != null) {
                codec.setVideoScalingMode(scalingMode);
            }
        } else {
            super.handleMessage(messageType, message);
        }
    }

    private void setSurface(Surface surface) throws ExoPlaybackException {
        if (surface == null) {
            // Use a dummy surface if possible.
            if (dummySurface != null) {
                surface = dummySurface;
            } else {
                MediaCodecInfo codecInfo = getCodecInfo();
                if (codecInfo != null && shouldUseDummySurface(codecInfo)) {
                    dummySurface = DummySurface.newInstanceV17(context, codecInfo.secure);
                    surface = dummySurface;
                }
            }
        }
        // We only need to update the codec if the surface has changed.
        if (this.surface != surface) {
            this.surface = surface;
            @State int state = getState();
            if (state == STATE_ENABLED || state == STATE_STARTED) {
                MediaCodec codec = getCodec();
                if (Util.SDK_INT >= 23 && codec != null && surface != null
                        && !codecNeedsSetOutputSurfaceWorkaround) {
                    setOutputSurfaceV23(codec, surface);
                } else {
                    releaseCodec();
                    maybeInitCodec();
                }
            }
            if (surface != null && surface != dummySurface) {
                // If we know the video size, report it again immediately.
                maybeRenotifyVideoSizeChanged();
                // We haven't rendered to the new surface yet.
                clearRenderedFirstFrame();
                if (state == STATE_STARTED) {
                    setJoiningDeadlineMs();
                }
            } else {
                // The surface has been removed.
                clearReportedVideoSize();
                clearRenderedFirstFrame();
            }
        } else if (surface != null && surface != dummySurface) {
            // The surface is set and unchanged. If we know the video size and/or have already rendered to
            // the surface, report these again immediately.
            maybeRenotifyVideoSizeChanged();
            maybeRenotifyRenderedFirstFrame();
        }
    }

    @Override
    protected boolean shouldInitCodec(MediaCodecInfo codecInfo) {
        return surface != null || shouldUseDummySurface(codecInfo);
    }

    @Override
    protected void configureCodec(MediaCodecInfo codecInfo, MediaCodec codec, Format format,
                                  MediaCrypto crypto) throws DecoderQueryException {
        codecMaxValues = getCodecMaxValues(codecInfo, format, getStreamFormats());
        MediaFormat mediaFormat = getMediaFormat(format, codecMaxValues, deviceNeedsAutoFrcWorkaround,
                tunnelingAudioSessionId);
        if (surface == null) {
            Assertions.checkState(shouldUseDummySurface(codecInfo));
            if (dummySurface == null) {
                dummySurface = DummySurface.newInstanceV17(context, codecInfo.secure);
            }
            surface = dummySurface;
        }
        codec.configure(mediaFormat, surface, crypto, 0);
        if (Util.SDK_INT >= 23 && tunneling) {
            tunnelingOnFrameRenderedListener = new OnFrameRenderedListenerV23(codec);
        }
    }

    @Override
    protected int canKeepCodec(
            MediaCodec codec, MediaCodecInfo codecInfo, Format oldFormat, Format newFormat) {
        if (areAdaptationCompatible(codecInfo.adaptive, oldFormat, newFormat)
                && newFormat.width <= codecMaxValues.width
                && newFormat.height <= codecMaxValues.height
                && getMaxInputSize(newFormat) <= codecMaxValues.inputSize) {
            return oldFormat.initializationDataEquals(newFormat)
                    ? KEEP_CODEC_RESULT_YES_WITHOUT_RECONFIGURATION
                    : KEEP_CODEC_RESULT_YES_WITH_RECONFIGURATION;
        }
        return KEEP_CODEC_RESULT_NO;
    }

    @CallSuper
    @Override
    protected void releaseCodec() {
        try {
            super.releaseCodec();
        } finally {
            buffersInCodecCount = 0;
            if (dummySurface != null) {
                if (surface == dummySurface) {
                    surface = null;
                }
                dummySurface.release();
                dummySurface = null;
            }
        }
    }

    @CallSuper
    @Override
    protected void flushCodec() throws ExoPlaybackException {
        super.flushCodec();
        buffersInCodecCount = 0;
    }

    @Override
    protected void onCodecInitialized(String name, long initializedTimestampMs,
                                      long initializationDurationMs) {
        eventDispatcher.decoderInitialized(name, initializedTimestampMs, initializationDurationMs);
        codecNeedsSetOutputSurfaceWorkaround = codecNeedsSetOutputSurfaceWorkaround(name);
    }

    @Override
    protected void onInputFormatChanged(Format newFormat) throws ExoPlaybackException {
        super.onInputFormatChanged(newFormat);
        eventDispatcher.inputFormatChanged(newFormat);
        pendingPixelWidthHeightRatio = newFormat.pixelWidthHeightRatio;
        pendingRotationDegrees = newFormat.rotationDegrees;
    }

    /**
     * Called immediately before an input buffer is queued into the codec.
     *
     * @param buffer The buffer to be queued.
     */
    @CallSuper
    @Override
    protected void onQueueInputBuffer(DecoderInputBuffer buffer) {
        buffersInCodecCount++;
        lastInputTimeUs = Math.max(buffer.timeUs, lastInputTimeUs);
        if (Util.SDK_INT < 23 && tunneling) {
            maybeNotifyRenderedFirstFrame();
        }
    }

    @Override
    protected void onOutputFormatChanged(MediaCodec codec, MediaFormat outputFormat) {
        boolean hasCrop = outputFormat.containsKey(KEY_CROP_RIGHT)
                && outputFormat.containsKey(KEY_CROP_LEFT) && outputFormat.containsKey(KEY_CROP_BOTTOM)
                && outputFormat.containsKey(KEY_CROP_TOP);
        currentWidth = hasCrop
                ? outputFormat.getInteger(KEY_CROP_RIGHT) - outputFormat.getInteger(KEY_CROP_LEFT) + 1
                : outputFormat.getInteger(MediaFormat.KEY_WIDTH);
        currentHeight = hasCrop
                ? outputFormat.getInteger(KEY_CROP_BOTTOM) - outputFormat.getInteger(KEY_CROP_TOP) + 1
                : outputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        currentPixelWidthHeightRatio = pendingPixelWidthHeightRatio;
        if (Util.SDK_INT >= 21) {
            // On API level 21 and above the decoder applies the rotation when rendering to the surface.
            // Hence currentUnappliedRotation should always be 0. For 90 and 270 degree rotations, we need
            // to flip the width, height and pixel aspect ratio to reflect the rotation that was applied.
            if (pendingRotationDegrees == 90 || pendingRotationDegrees == 270) {
                int rotatedHeight = currentWidth;
                currentWidth = currentHeight;
                currentHeight = rotatedHeight;
                currentPixelWidthHeightRatio = 1 / currentPixelWidthHeightRatio;
            }
        } else {
            // On API level 20 and below the decoder does not apply the rotation.
            currentUnappliedRotationDegrees = pendingRotationDegrees;
        }
        // Must be applied each time the output format changes.
        codec.setVideoScalingMode(scalingMode);
    }

    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec,
                                          ByteBuffer buffer, int bufferIndex, int bufferFlags, long bufferPresentationTimeUs,
                                          boolean shouldSkip) throws ExoPlaybackException {
        if (initialPositionUs == C.TIME_UNSET) {
            initialPositionUs = positionUs;
        }

        long presentationTimeUs = bufferPresentationTimeUs - outputStreamOffsetUs;

        if (shouldSkip) {
            skipOutputBuffer(codec, bufferIndex, presentationTimeUs);
            return true;
        }

        long earlyUs = bufferPresentationTimeUs - positionUs;
        if (surface == dummySurface) {
            // Skip frames in sync with playback, so we'll be at the right frame if the mode changes.
            if (isBufferLate(earlyUs)) {
                skipOutputBuffer(codec, bufferIndex, presentationTimeUs);
                return true;
            }
            return false;
        }

        long elapsedRealtimeNowUs = SystemClock.elapsedRealtime() * 1000;
        boolean isStarted = getState() == STATE_STARTED;
        if (!renderedFirstFrame
                || (isStarted
                && shouldForceRenderOutputBuffer(earlyUs, elapsedRealtimeNowUs - lastRenderTimeUs))) {
            if (Util.SDK_INT >= 21) {
                renderOutputBufferV21(codec, bufferIndex, presentationTimeUs, System.nanoTime());
            } else {
                renderOutputBuffer(codec, bufferIndex, presentationTimeUs);
            }
            return true;
        }

        if (!isStarted || positionUs == initialPositionUs) {
            return false;
        }

        // Fine-grained adjustment of earlyUs based on the elapsed time since the start of the current
        // iteration of the rendering loop.
        long elapsedSinceStartOfLoopUs = elapsedRealtimeNowUs - elapsedRealtimeUs;
        earlyUs -= elapsedSinceStartOfLoopUs;

        // Compute the buffer's desired release time in nanoseconds.
        long systemTimeNs = System.nanoTime();
        long unadjustedFrameReleaseTimeNs = systemTimeNs + (earlyUs * 1000);

        // Apply a timestamp adjustment, if there is one.
        long adjustedReleaseTimeNs = frameReleaseTimeHelper.adjustReleaseTime(
                bufferPresentationTimeUs, unadjustedFrameReleaseTimeNs);
        earlyUs = (adjustedReleaseTimeNs - systemTimeNs) / 1000;

        if (shouldDropBuffersToKeyframe(earlyUs, elapsedRealtimeUs)
                && maybeDropBuffersToKeyframe(codec, bufferIndex, presentationTimeUs, positionUs)) {
            return false;
        } else if (shouldDropOutputBuffer(earlyUs, elapsedRealtimeUs)) {
            dropOutputBuffer(codec, bufferIndex, presentationTimeUs);
            return true;
        }

        if (Util.SDK_INT >= 21) {
            // Let the underlying framework time the release.
            if (earlyUs < 50000) {
                renderOutputBufferV21(codec, bufferIndex, presentationTimeUs, adjustedReleaseTimeNs);
                return true;
            }
        } else {
            // We need to time the release ourselves.
            if (earlyUs < 30000) {
                if (earlyUs > 11000) {
                    // We're a little too early to render the frame. Sleep until the frame can be rendered.
                    // Note: The 11ms threshold was chosen fairly arbitrarily.
                    try {
                        // Subtracting 10000 rather than 11000 ensures the sleep time will be at least 1ms.
                        Thread.sleep((earlyUs - 10000) / 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
                renderOutputBuffer(codec, bufferIndex, presentationTimeUs);
                return true;
            }
        }

        // We're either not playing, or it's not time to render the frame yet.
        return false;
    }

    /**
     * Called when an output buffer is successfully processed.
     *
     * @param presentationTimeUs The timestamp associated with the output buffer.
     */
    @CallSuper
    @Override
    protected void onProcessedOutputBuffer(long presentationTimeUs) {
        buffersInCodecCount--;
        while (pendingOutputStreamOffsetCount != 0
                && presentationTimeUs >= pendingOutputStreamSwitchTimesUs[0]) {
            outputStreamOffsetUs = pendingOutputStreamOffsetsUs[0];
            pendingOutputStreamOffsetCount--;
            System.arraycopy(
                    pendingOutputStreamOffsetsUs,
                    /* srcPos= */ 1,
                    pendingOutputStreamOffsetsUs,
                    /* destPos= */ 0,
                    pendingOutputStreamOffsetCount);
            System.arraycopy(
                    pendingOutputStreamSwitchTimesUs,
                    /* srcPos= */ 1,
                    pendingOutputStreamSwitchTimesUs,
                    /* destPos= */ 0,
                    pendingOutputStreamOffsetCount);
        }
    }

    /**
     * Returns whether the buffer being processed should be dropped.
     *
     * @param earlyUs           The time until the buffer should be presented in microseconds. A negative value
     *                          indicates that the buffer is late.
     * @param elapsedRealtimeUs {@link android.os.SystemClock#elapsedRealtime()} in microseconds,
     *                          measured at the start of the current iteration of the rendering loop.
     */
    protected boolean shouldDropOutputBuffer(long earlyUs, long elapsedRealtimeUs) {
        return isBufferLate(earlyUs);
    }

    /**
     * Returns whether to drop all buffers from the buffer being processed to the keyframe at or after
     * the current playback position, if possible.
     *
     * @param earlyUs           The time until the current buffer should be presented in microseconds. A
     *                          negative value indicates that the buffer is late.
     * @param elapsedRealtimeUs {@link android.os.SystemClock#elapsedRealtime()} in microseconds,
     *                          measured at the start of the current iteration of the rendering loop.
     */
    protected boolean shouldDropBuffersToKeyframe(long earlyUs, long elapsedRealtimeUs) {
        return isBufferVeryLate(earlyUs);
    }

    /**
     * Returns whether to force rendering an output buffer.
     *
     * @param earlyUs                  The time until the current buffer should be presented in microseconds. A
     *                                 negative value indicates that the buffer is late.
     * @param elapsedSinceLastRenderUs The elapsed time since the last output buffer was rendered, in
     *                                 microseconds.
     * @return Returns whether to force rendering an output buffer.
     */
    protected boolean shouldForceRenderOutputBuffer(long earlyUs, long elapsedSinceLastRenderUs) {
        return isBufferLate(earlyUs) && elapsedSinceLastRenderUs > 100000;
    }

    /**
     * Skips the output buffer with the specified index.
     *
     * @param codec              The codec that owns the output buffer.
     * @param index              The index of the output buffer to skip.
     * @param presentationTimeUs The presentation time of the output buffer, in microseconds.
     */
    protected void skipOutputBuffer(MediaCodec codec, int index, long presentationTimeUs) {
        TraceUtil.beginSection("skipVideoBuffer");
        codec.releaseOutputBuffer(index, false);
        TraceUtil.endSection();
        decoderCounters.skippedOutputBufferCount++;
    }

    /**
     * Drops the output buffer with the specified index.
     *
     * @param codec              The codec that owns the output buffer.
     * @param index              The index of the output buffer to drop.
     * @param presentationTimeUs The presentation time of the output buffer, in microseconds.
     */
    protected void dropOutputBuffer(MediaCodec codec, int index, long presentationTimeUs) {
        TraceUtil.beginSection("dropVideoBuffer");
        codec.releaseOutputBuffer(index, false);
        TraceUtil.endSection();
        updateDroppedBufferCounters(1);
    }

    /**
     * Drops frames from the current output buffer to the next keyframe at or before the playback
     * position. If no such keyframe exists, as the playback position is inside the same group of
     * pictures as the buffer being processed, returns {@code false}. Returns {@code true} otherwise.
     *
     * @param codec              The codec that owns the output buffer.
     * @param index              The index of the output buffer to drop.
     * @param presentationTimeUs The presentation time of the output buffer, in microseconds.
     * @param positionUs         The current playback position, in microseconds.
     * @return Whether any buffers were dropped.
     * @throws ExoPlaybackException If an error occurs flushing the codec.
     */
    protected boolean maybeDropBuffersToKeyframe(MediaCodec codec, int index, long presentationTimeUs,
                                                 long positionUs) throws ExoPlaybackException {
        int droppedSourceBufferCount = skipSource(positionUs);
        if (droppedSourceBufferCount == 0) {
            return false;
        }
        decoderCounters.droppedToKeyframeCount++;
        // We dropped some buffers to catch up, so update the decoder counters and flush the codec,
        // which releases all pending buffers buffers including the current output buffer.
        updateDroppedBufferCounters(buffersInCodecCount + droppedSourceBufferCount);
        flushCodec();
        return true;
    }

    /**
     * Updates decoder counters to reflect that {@code droppedBufferCount} additional buffers were
     * dropped.
     *
     * @param droppedBufferCount The number of additional dropped buffers.
     */
    protected void updateDroppedBufferCounters(int droppedBufferCount) {
        decoderCounters.droppedBufferCount += droppedBufferCount;
        droppedFrames += droppedBufferCount;
        consecutiveDroppedFrameCount += droppedBufferCount;
        decoderCounters.maxConsecutiveDroppedBufferCount = Math.max(consecutiveDroppedFrameCount,
                decoderCounters.maxConsecutiveDroppedBufferCount);
        if (droppedFrames >= maxDroppedFramesToNotify) {
            maybeNotifyDroppedFrames();
        }
    }

    /**
     * Renders the output buffer with the specified index. This method is only called if the platform
     * API version of the device is less than 21.
     *
     * @param codec              The codec that owns the output buffer.
     * @param index              The index of the output buffer to drop.
     * @param presentationTimeUs The presentation time of the output buffer, in microseconds.
     */
    protected void renderOutputBuffer(MediaCodec codec, int index, long presentationTimeUs) {
        maybeNotifyVideoSizeChanged();
        TraceUtil.beginSection("releaseOutputBuffer");
        codec.releaseOutputBuffer(index, true);
        TraceUtil.endSection();
        lastRenderTimeUs = SystemClock.elapsedRealtime() * 1000;
        decoderCounters.renderedOutputBufferCount++;
        consecutiveDroppedFrameCount = 0;
        maybeNotifyRenderedFirstFrame();
    }

    /**
     * Renders the output buffer with the specified index. This method is only called if the platform
     * API version of the device is 21 or later.
     *
     * @param codec              The codec that owns the output buffer.
     * @param index              The index of the output buffer to drop.
     * @param presentationTimeUs The presentation time of the output buffer, in microseconds.
     * @param releaseTimeNs      The wallclock time at which the frame should be displayed, in nanoseconds.
     */
    @TargetApi(21)
    protected void renderOutputBufferV21(
            MediaCodec codec, int index, long presentationTimeUs, long releaseTimeNs) {
        maybeNotifyVideoSizeChanged();
        TraceUtil.beginSection("releaseOutputBuffer");
        codec.releaseOutputBuffer(index, releaseTimeNs);
        TraceUtil.endSection();
        lastRenderTimeUs = SystemClock.elapsedRealtime() * 1000;
        decoderCounters.renderedOutputBufferCount++;
        consecutiveDroppedFrameCount = 0;
        maybeNotifyRenderedFirstFrame();
    }

    private boolean shouldUseDummySurface(MediaCodecInfo codecInfo) {
        return Util.SDK_INT >= 23
                && !tunneling
                && !codecNeedsSetOutputSurfaceWorkaround(codecInfo.name)
                && (!codecInfo.secure || DummySurface.isSecureSupported(context));
    }

    private void setJoiningDeadlineMs() {
        joiningDeadlineMs = allowedJoiningTimeMs > 0
                ? (SystemClock.elapsedRealtime() + allowedJoiningTimeMs) : C.TIME_UNSET;
    }

    private void clearRenderedFirstFrame() {
        renderedFirstFrame = false;
        // The first frame notification is triggered by renderOutputBuffer or renderOutputBufferV21 for
        // non-tunneled playback, onQueueInputBuffer for tunneled playback prior to API level 23, and
        // OnFrameRenderedListenerV23.onFrameRenderedListener for tunneled playback on API level 23 and
        // above.
        if (Util.SDK_INT >= 23 && tunneling) {
            MediaCodec codec = getCodec();
            // If codec is null then the listener will be instantiated in configureCodec.
            if (codec != null) {
                tunnelingOnFrameRenderedListener = new OnFrameRenderedListenerV23(codec);
            }
        }
    }

    /* package */ void maybeNotifyRenderedFirstFrame() {
        if (!renderedFirstFrame) {
            renderedFirstFrame = true;
            eventDispatcher.renderedFirstFrame(surface);
        }
    }

    private void maybeRenotifyRenderedFirstFrame() {
        if (renderedFirstFrame) {
            eventDispatcher.renderedFirstFrame(surface);
        }
    }

    private void clearReportedVideoSize() {
        reportedWidth = Format.NO_VALUE;
        reportedHeight = Format.NO_VALUE;
        reportedPixelWidthHeightRatio = Format.NO_VALUE;
        reportedUnappliedRotationDegrees = Format.NO_VALUE;
    }

    private void maybeNotifyVideoSizeChanged() {
        if ((currentWidth != Format.NO_VALUE || currentHeight != Format.NO_VALUE)
                && (reportedWidth != currentWidth || reportedHeight != currentHeight
                || reportedUnappliedRotationDegrees != currentUnappliedRotationDegrees
                || reportedPixelWidthHeightRatio != currentPixelWidthHeightRatio)) {
            eventDispatcher.videoSizeChanged(currentWidth, currentHeight, currentUnappliedRotationDegrees,
                    currentPixelWidthHeightRatio);
            reportedWidth = currentWidth;
            reportedHeight = currentHeight;
            reportedUnappliedRotationDegrees = currentUnappliedRotationDegrees;
            reportedPixelWidthHeightRatio = currentPixelWidthHeightRatio;
        }
    }

    private void maybeRenotifyVideoSizeChanged() {
        if (reportedWidth != Format.NO_VALUE || reportedHeight != Format.NO_VALUE) {
            eventDispatcher.videoSizeChanged(reportedWidth, reportedHeight,
                    reportedUnappliedRotationDegrees, reportedPixelWidthHeightRatio);
        }
    }

    private void maybeNotifyDroppedFrames() {
        if (droppedFrames > 0) {
            long now = SystemClock.elapsedRealtime();
            long elapsedMs = now - droppedFrameAccumulationStartTimeMs;
            eventDispatcher.droppedFrames(droppedFrames, elapsedMs);
            droppedFrames = 0;
            droppedFrameAccumulationStartTimeMs = now;
        }
    }

    private static boolean isBufferLate(long earlyUs) {
        // Class a buffer as late if it should have been presented more than 30 ms ago.
        return earlyUs < -30000;
    }

    private static boolean isBufferVeryLate(long earlyUs) {
        // Class a buffer as very late if it should have been presented more than 500 ms ago.
        return earlyUs < -500000;
    }

    @TargetApi(23)
    private static void setOutputSurfaceV23(MediaCodec codec, Surface surface) {
        codec.setOutputSurface(surface);
    }

    @TargetApi(21)
    private static void configureTunnelingV21(MediaFormat mediaFormat, int tunnelingAudioSessionId) {
        mediaFormat.setFeatureEnabled(CodecCapabilities.FEATURE_TunneledPlayback, true);
        mediaFormat.setInteger(MediaFormat.KEY_AUDIO_SESSION_ID, tunnelingAudioSessionId);
    }

    /**
     * Returns the framework {@link MediaFormat} that should be used to configure the decoder.
     *
     * @param format                       The format of media.
     * @param codecMaxValues               Codec max values that should be used when configuring the decoder.
     * @param deviceNeedsAutoFrcWorkaround Whether the device is known to enable frame-rate conversion
     *                                     logic that negatively impacts ExoPlayer.
     * @param tunnelingAudioSessionId      The audio session id to use for tunneling, or {@link
     *                                     C#AUDIO_SESSION_ID_UNSET} if tunneling should not be enabled.
     * @return The framework {@link MediaFormat} that should be used to configure the decoder.
     */
    @SuppressLint("InlinedApi")
    protected MediaFormat getMediaFormat(
            Format format,
            CodecMaxValues codecMaxValues,
            boolean deviceNeedsAutoFrcWorkaround,
            int tunnelingAudioSessionId) {
        MediaFormat mediaFormat = new MediaFormat();
        // Set format parameters that should always be set.
        mediaFormat.setString(MediaFormat.KEY_MIME, format.sampleMimeType);
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, format.width);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, format.height);
        MediaFormatUtil.setCsdBuffers(mediaFormat, format.initializationData);
        // Set format parameters that may be unset.
        MediaFormatUtil.maybeSetFloat(mediaFormat, MediaFormat.KEY_FRAME_RATE, format.frameRate);
        MediaFormatUtil.maybeSetInteger(mediaFormat, MediaFormat.KEY_ROTATION, format.rotationDegrees);
        MediaFormatUtil.maybeSetColorInfo(mediaFormat, format.colorInfo);
        // Set codec max values.
        mediaFormat.setInteger(MediaFormat.KEY_MAX_WIDTH, codecMaxValues.width);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_HEIGHT, codecMaxValues.height);
        MediaFormatUtil.maybeSetInteger(
                mediaFormat, MediaFormat.KEY_MAX_INPUT_SIZE, codecMaxValues.inputSize);
        // Set codec configuration values.
        if (Util.SDK_INT >= 23) {
            mediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0 /* realtime priority */);
        }
        if (deviceNeedsAutoFrcWorkaround) {
            mediaFormat.setInteger("auto-frc", 0);
        }
        if (tunnelingAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            configureTunnelingV21(mediaFormat, tunnelingAudioSessionId);
        }
        return mediaFormat;
    }

    /**
     * Returns {@link CodecMaxValues} suitable for configuring a codec for {@code format} in a way
     * that will allow possible adaptation to other compatible formats in {@code streamFormats}.
     *
     * @param codecInfo     Information about the {@link MediaCodec} being configured.
     * @param format        The format for which the codec is being configured.
     * @param streamFormats The possible stream formats.
     * @return Suitable {@link CodecMaxValues}.
     * @throws DecoderQueryException If an error occurs querying {@code codecInfo}.
     */
    protected CodecMaxValues getCodecMaxValues(
            MediaCodecInfo codecInfo, Format format, Format[] streamFormats)
            throws DecoderQueryException {
        int maxWidth = format.width;
        int maxHeight = format.height;
        int maxInputSize = getMaxInputSize(format);
        if (streamFormats.length == 1) {
            // The single entry in streamFormats must correspond to the format for which the codec is
            // being configured.
            return new CodecMaxValues(maxWidth, maxHeight, maxInputSize);
        }
        boolean haveUnknownDimensions = false;
        for (Format streamFormat : streamFormats) {
            if (areAdaptationCompatible(codecInfo.adaptive, format, streamFormat)) {
                haveUnknownDimensions |=
                        (streamFormat.width == Format.NO_VALUE || streamFormat.height == Format.NO_VALUE);
                maxWidth = Math.max(maxWidth, streamFormat.width);
                maxHeight = Math.max(maxHeight, streamFormat.height);
                maxInputSize = Math.max(maxInputSize, getMaxInputSize(streamFormat));
            }
        }
        if (haveUnknownDimensions) {
            Log.w(TAG, "Resolutions unknown. Codec max resolution: " + maxWidth + "x" + maxHeight);
            Point codecMaxSize = getCodecMaxSize(codecInfo, format);
            if (codecMaxSize != null) {
                maxWidth = Math.max(maxWidth, codecMaxSize.x);
                maxHeight = Math.max(maxHeight, codecMaxSize.y);
                maxInputSize =
                        Math.max(maxInputSize, getMaxInputSize(format.sampleMimeType, maxWidth, maxHeight));
                Log.w(TAG, "Codec max resolution adjusted to: " + maxWidth + "x" + maxHeight);
            }
        }
        return new CodecMaxValues(maxWidth, maxHeight, maxInputSize);
    }

    /**
     * Returns a maximum video size to use when configuring a codec for {@code format} in a way
     * that will allow possible adaptation to other compatible formats that are expected to have the
     * same aspect ratio, but whose sizes are unknown.
     *
     * @param codecInfo Information about the {@link MediaCodec} being configured.
     * @param format    The format for which the codec is being configured.
     * @return The maximum video size to use, or null if the size of {@code format} should be used.
     * @throws DecoderQueryException If an error occurs querying {@code codecInfo}.
     */
    private static Point getCodecMaxSize(MediaCodecInfo codecInfo, Format format)
            throws DecoderQueryException {
        boolean isVerticalVideo = format.height > format.width;
        int formatLongEdgePx = isVerticalVideo ? format.height : format.width;
        int formatShortEdgePx = isVerticalVideo ? format.width : format.height;
        float aspectRatio = (float) formatShortEdgePx / formatLongEdgePx;
        for (int longEdgePx : STANDARD_LONG_EDGE_VIDEO_PX) {
            int shortEdgePx = (int) (longEdgePx * aspectRatio);
            if (longEdgePx <= formatLongEdgePx || shortEdgePx <= formatShortEdgePx) {
                // Don't return a size not larger than the format for which the codec is being configured.
                return null;
            } else if (Util.SDK_INT >= 21) {
                Point alignedSize = codecInfo.alignVideoSizeV21(isVerticalVideo ? shortEdgePx : longEdgePx,
                        isVerticalVideo ? longEdgePx : shortEdgePx);
                float frameRate = format.frameRate;
                if (codecInfo.isVideoSizeAndRateSupportedV21(alignedSize.x, alignedSize.y, frameRate)) {
                    return alignedSize;
                }
            } else {
                // Conservatively assume the codec requires 16px width and height alignment.
                longEdgePx = Util.ceilDivide(longEdgePx, 16) * 16;
                shortEdgePx = Util.ceilDivide(shortEdgePx, 16) * 16;
                if (longEdgePx * shortEdgePx <= MediaCodecUtil.maxH264DecodableFrameSize()) {
                    return new Point(isVerticalVideo ? shortEdgePx : longEdgePx,
                            isVerticalVideo ? longEdgePx : shortEdgePx);
                }
            }
        }
        return null;
    }

    /**
     * Returns a maximum input buffer size for a given format.
     *
     * @param format The format.
     * @return A maximum input buffer size in bytes, or {@link Format#NO_VALUE} if a maximum could not
     * be determined.
     */
    private static int getMaxInputSize(Format format) {
        if (format.maxInputSize != Format.NO_VALUE) {
            // The format defines an explicit maximum input size. Add the total size of initialization
            // data buffers, as they may need to be queued in the same input buffer as the largest sample.
            int totalInitializationDataSize = 0;
            int initializationDataCount = format.initializationData.size();
            for (int i = 0; i < initializationDataCount; i++) {
                totalInitializationDataSize += format.initializationData.get(i).length;
            }
            return format.maxInputSize + totalInitializationDataSize;
        } else {
            // Calculated maximum input sizes are overestimates, so it's not necessary to add the size of
            // initialization data.
            return getMaxInputSize(format.sampleMimeType, format.width, format.height);
        }
    }

    /**
     * Returns a maximum input size for a given mime type, width and height.
     *
     * @param sampleMimeType The format mime type.
     * @param width          The width in pixels.
     * @param height         The height in pixels.
     * @return A maximum input size in bytes, or {@link Format#NO_VALUE} if a maximum could not be
     * determined.
     */
    private static int getMaxInputSize(String sampleMimeType, int width, int height) {
        if (width == Format.NO_VALUE || height == Format.NO_VALUE) {
            // We can't infer a maximum input size without video dimensions.
            return Format.NO_VALUE;
        }

        // Attempt to infer a maximum input size from the format.
        int maxPixels;
        int minCompressionRatio;
        switch (sampleMimeType) {
            case MimeTypes.VIDEO_H263:
            case MimeTypes.VIDEO_MP4V:
                maxPixels = width * height;
                minCompressionRatio = 2;
                break;
            case MimeTypes.VIDEO_H264:
                if ("BRAVIA 4K 2015".equals(Util.MODEL)) {
                    // The Sony BRAVIA 4k TV has input buffers that are too small for the calculated 4k video
                    // maximum input size, so use the default value.
                    return Format.NO_VALUE;
                }
                // Round up width/height to an integer number of macroblocks.
                maxPixels = Util.ceilDivide(width, 16) * Util.ceilDivide(height, 16) * 16 * 16;
                minCompressionRatio = 2;
                break;
            case MimeTypes.VIDEO_VP8:
                // VPX does not specify a ratio so use the values from the platform's SoftVPX.cpp.
                maxPixels = width * height;
                minCompressionRatio = 2;
                break;
            case MimeTypes.VIDEO_H265:
            case MimeTypes.VIDEO_VP9:
                maxPixels = width * height;
                minCompressionRatio = 4;
                break;
            default:
                // Leave the default max input size.
                return Format.NO_VALUE;
        }
        // Estimate the maximum input size assuming three channel 4:2:0 subsampled input frames.
        return (maxPixels * 3) / (2 * minCompressionRatio);
    }

    /**
     * Returns whether a codec with suitable {@link CodecMaxValues} will support adaptation between
     * two {@link Format}s.
     *
     * @param codecIsAdaptive Whether the codec supports seamless resolution switches.
     * @param first           The first format.
     * @param second          The second format.
     * @return Whether the codec will support adaptation between the two {@link Format}s.
     */
    private static boolean areAdaptationCompatible(
            boolean codecIsAdaptive, Format first, Format second) {
        return first.sampleMimeType.equals(second.sampleMimeType)
                && first.rotationDegrees == second.rotationDegrees
                && (codecIsAdaptive || (first.width == second.width && first.height == second.height))
                && Util.areEqual(first.colorInfo, second.colorInfo);
    }

    /**
     * Returns whether the device is known to enable frame-rate conversion logic that negatively
     * impacts ExoPlayer.
     * <p>
     * If true is returned then we explicitly disable the feature.
     *
     * @return True if the device is known to enable frame-rate conversion logic that negatively
     * impacts ExoPlayer. False otherwise.
     */
    private static boolean deviceNeedsAutoFrcWorkaround() {
        // nVidia Shield prior to M tries to adjust the playback rate to better map the frame-rate of
        // content to the refresh rate of the display. For example playback of 23.976fps content is
        // adjusted to play at 1.001x speed when the output display is 60Hz. Unfortunately the
        // implementation causes ExoPlayer's reported playback position to drift out of sync. Captions
        // also lose sync [Internal: b/26453592].
        return Util.SDK_INT <= 22 && "foster".equals(Util.DEVICE) && "NVIDIA".equals(Util.MANUFACTURER);
    }

    /**
     * Returns whether the device is known to implement {@link MediaCodec#setOutputSurface(Surface)}
     * incorrectly.
     * <p>
     * If true is returned then we fall back to releasing and re-instantiating the codec instead.
     */
    private static boolean codecNeedsSetOutputSurfaceWorkaround(String name) {
        // Work around https://github.com/google/ExoPlayer/issues/3236,
        // https://github.com/google/ExoPlayer/issues/3355,
        // https://github.com/google/ExoPlayer/issues/3439,
        // https://github.com/google/ExoPlayer/issues/3724,
        // https://github.com/google/ExoPlayer/issues/3835,
        // https://github.com/google/ExoPlayer/issues/4006,
        // https://github.com/google/ExoPlayer/issues/4084,
        // https://github.com/google/ExoPlayer/issues/4104,
        // https://github.com/google/ExoPlayer/issues/4134,
        // https://github.com/google/ExoPlayer/issues/4315.
        return DummySurfaceWorkaroundTest.workaroundRequired
                || (("deb".equals(Util.DEVICE) // Nexus 7 (2013)
                || "flo".equals(Util.DEVICE) // Nexus 7 (2013)
                || "mido".equals(Util.DEVICE) // Redmi Note 4
                || "santoni".equals(Util.DEVICE)) // Redmi 4X
                && "OMX.qcom.video.decoder.avc".equals(name))
                || (("tcl_eu".equals(Util.DEVICE) // TCL Percee TV
                || "SVP-DTV15".equals(Util.DEVICE) // Sony Bravia 4K 2015
                || "BRAVIA_ATV2".equals(Util.DEVICE) // Sony Bravia 4K GB
                || Util.DEVICE.startsWith("panell_") // Motorola Moto C Plus
                || "F3311".equals(Util.DEVICE) // Sony Xperia E5
                || "M5c".equals(Util.DEVICE) // Meizu M5C
                || "QM16XE_U".equals(Util.DEVICE) // Philips QM163E
                || "A7010a48".equals(Util.DEVICE) // Lenovo K4 Note
                || "woods_f".equals(Util.MODEL) // Moto E (4)
                || "watson".equals(Util.DEVICE)) // Moto C
                && "OMX.MTK.VIDEO.DECODER.AVC".equals(name))
                || (("ALE-L21".equals(Util.MODEL) // Huawei P8 Lite
                || "CAM-L21".equals(Util.MODEL)) // Huawei Y6II
                && "OMX.k3.video.decoder.avc".equals(name))
                || (("HUAWEI VNS-L21".equals(Util.MODEL)) // Huawei P9 Lite
                && "OMX.IMG.MSVDX.Decoder.AVC".equals(name));
    }

    protected static final class CodecMaxValues {

        public final int width;
        public final int height;
        public final int inputSize;

        public CodecMaxValues(int width, int height, int inputSize) {
            this.width = width;
            this.height = height;
            this.inputSize = inputSize;
        }

    }

    @TargetApi(23)
    private final class OnFrameRenderedListenerV23 implements MediaCodec.OnFrameRenderedListener {

        private OnFrameRenderedListenerV23(MediaCodec codec) {
            codec.setOnFrameRenderedListener(this, new Handler());
        }

        @Override
        public void onFrameRendered(@NonNull MediaCodec codec, long presentationTimeUs, long nanoTime) {
            if (this != tunnelingOnFrameRenderedListener) {
                // Stale event.
                return;
            }
            maybeNotifyRenderedFirstFrame();
        }

    }
}