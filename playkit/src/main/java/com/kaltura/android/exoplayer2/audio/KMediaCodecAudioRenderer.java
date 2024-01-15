package com.kaltura.android.exoplayer2.audio;

import static com.kaltura.android.exoplayer2.audio.DefaultAudioSink.DEFAULT_PLAYBACK_SPEED;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.ExoPlaybackException;
import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.PlaybackParameters;
import com.kaltura.android.exoplayer2.decoder.DecoderInputBuffer;
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecAdapter;
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.kaltura.playkit.PKLog;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Objects;

public class KMediaCodecAudioRenderer extends MediaCodecAudioRenderer {

    private static final String ALLOW_FIRST_BUFFER_POSITION_DISCONTINUITY_FIELD_NAME = "allowFirstBufferPositionDiscontinuity";

    private static final String CURRENT_POSITION_US_FIELD_NAME = "currentPositionUs";

    private static final String DECRYPT_ONLY_CODEC_FORMAT_FIELD_NAME = "decryptOnlyCodecFormat";

    private static final long DEFAULT_MAX_AUDIO_GAP_THRESHOLD = 3_000_000L;

    private static final boolean DEFAULT_USE_CONTINUOUS_SPEED_ADJUSTMENT = false;

    private static final float DEFAULT_MAX_SPEED_FACTOR = 4.0f;

    private static final float DEFAULT_SPEED_STEP = 3.0f;

    private static final long DEFAULT_MAX_AV_GAP = 600_000L;

    private final long maxAudioGapThreshold;

    private final boolean useContinuousSpeedAdjustment;

    private final float maxSpeedFactor;

    private final float speedStep;

    private final long maxAVGap;

    private static final PKLog log = PKLog.get("KMediaCodecAudioRenderer");

    private boolean speedAdjustedAfterPositionReset = false;

    public KMediaCodecAudioRenderer(Context context,
                                    MediaCodecAdapter.Factory codecAdapterFactory,
                                    MediaCodecSelector mediaCodecSelector,
                                    boolean enableDecoderFallback,
                                    @Nullable Handler eventHandler,
                                    @Nullable AudioRendererEventListener eventListener,
                                    AudioSink audioSink) {
        this(context,
                codecAdapterFactory,
                mediaCodecSelector,
                enableDecoderFallback,
                eventHandler,
                eventListener,
                audioSink,
                DEFAULT_MAX_AUDIO_GAP_THRESHOLD,
                DEFAULT_MAX_SPEED_FACTOR,
                DEFAULT_SPEED_STEP,
                DEFAULT_MAX_AV_GAP,
                DEFAULT_USE_CONTINUOUS_SPEED_ADJUSTMENT);
    }

    public KMediaCodecAudioRenderer(Context context,
                                    MediaCodecAdapter.Factory codecAdapterFactory,
                                    MediaCodecSelector mediaCodecSelector,
                                    boolean enableDecoderFallback,
                                    @Nullable Handler eventHandler,
                                    @Nullable AudioRendererEventListener eventListener,
                                    AudioSink audioSink,
                                    long maxAudioGapThreshold,
                                    float maxSpeedFactor,
                                    float speedStep,
                                    long maxAVGap,
                                    boolean useContinuousSpeedAdjustment) {
        super(context, codecAdapterFactory, mediaCodecSelector, enableDecoderFallback, eventHandler, eventListener, audioSink);
        this.maxAudioGapThreshold = maxAudioGapThreshold;
        this.maxSpeedFactor = maxSpeedFactor;
        this.speedStep = speedStep;
        this.maxAVGap = maxAVGap;
        this.useContinuousSpeedAdjustment = useContinuousSpeedAdjustment;
        log.d("KMediaCodecAudioRenderer", "getSpeedGap()=" + getMaxAVGap() +
                ", getSpeedFactor()=" + getMaxSpeedFactor() +
                ", getSpeedStep()=" + getSpeedStep() +
                ", continuousSpeedAdjustment=" + getContinuousSpeedAdjustment());
    }

    protected long getMaxAudioGapThreshold() {
        return maxAudioGapThreshold;
    }

    protected float getMaxSpeedFactor() {
        return maxSpeedFactor;
    }

    protected float getSpeedStep() {
        return speedStep;
    }

    protected long getMaxAVGap() {
        return maxAVGap;
    }

    protected boolean getContinuousSpeedAdjustment() {
        return useContinuousSpeedAdjustment;
    }

    @Override
    protected void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        super.onPositionReset(positionUs, joining);
        speedAdjustedAfterPositionReset = false;
    }

    @Override
    protected void onQueueInputBuffer(DecoderInputBuffer buffer) {
        try {
            Field allowFirstBufferPositionDiscontinuityField = Objects.requireNonNull(
                            getClass().getSuperclass())
                    .getDeclaredField(ALLOW_FIRST_BUFFER_POSITION_DISCONTINUITY_FIELD_NAME);
            allowFirstBufferPositionDiscontinuityField.setAccessible(true);
            Field currentPositionUsField = Objects.requireNonNull(
                            getClass().getSuperclass())
                    .getDeclaredField(CURRENT_POSITION_US_FIELD_NAME);
            currentPositionUsField.setAccessible(true);
            if (allowFirstBufferPositionDiscontinuityField.getBoolean(this) && !buffer.isDecodeOnly()) {
                log.d("KMediaCodecAudioRenderer", "A/V start buffers gap measured: "
                        + Math.abs(buffer.timeUs - currentPositionUsField.getLong(this)) + " uS");
                if (Math.abs(buffer.timeUs - currentPositionUsField.getLong(this)) > getMaxAudioGapThreshold()) {
                    currentPositionUsField.setLong(this, buffer.timeUs);
                }
                allowFirstBufferPositionDiscontinuityField.setBoolean(this, false);
            }
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.e("KMediaCodecAudioRenderer", "Error subclassing audio renderer: " + e.getMessage());
            // Fallback to superclass in case something goes wrong
            super.onQueueInputBuffer(buffer);
        }
    }

    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, @Nullable MediaCodecAdapter codec, @Nullable ByteBuffer buffer, int bufferIndex, int bufferFlags, int sampleCount, long bufferPresentationTimeUs, boolean isDecodeOnlyBuffer, boolean isLastBuffer, Format format) throws ExoPlaybackException {
        Format decryptOnlyCodecFormat = null;
        try {
            Field decryptOnlyCodeFormatField = Objects.requireNonNull(
                    getClass().getSuperclass()).getDeclaredField(DECRYPT_ONLY_CODEC_FORMAT_FIELD_NAME);
            decryptOnlyCodeFormatField.setAccessible(true);
            decryptOnlyCodecFormat = (Format)decryptOnlyCodeFormatField.get(this);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.e("KMediaCodecAudioRenderer", "Error getting decryptOnlyCodecFormat: " + e.getMessage());
        }

        if ((decryptOnlyCodecFormat != null
                && (bufferFlags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) || isDecodeOnlyBuffer) {
            return super.processOutputBuffer(
                    positionUs,
                    elapsedRealtimeUs,
                    codec,
                    buffer,
                    bufferIndex,
                    bufferFlags,
                    sampleCount,
                    bufferPresentationTimeUs,
                    isDecodeOnlyBuffer,
                    isLastBuffer,
                    format);
        }

        if (!speedAdjustedAfterPositionReset || getContinuousSpeedAdjustment()) {
//            log.d("KMediaCodecAudioRenderer", "currentSpeed=" + getPlaybackParameters().speed +
//                    ", bufferPresentationTimeUs=" + bufferPresentationTimeUs +
//                    ", positionUs=" + positionUs);
            if (bufferPresentationTimeUs - positionUs > getMaxAVGap()
                    && getPlaybackParameters().speed < getMaxSpeedFactor()) {
                float newSpeed = getPlaybackParameters().speed + getSpeedStep();
                newSpeed = min(newSpeed, getMaxSpeedFactor());
                log.d("KMediaCodecAudioRenderer", "Setting speed to " + newSpeed);
                setPlaybackParameters(new PlaybackParameters(newSpeed));
            } else if (getPlaybackParameters().speed != DEFAULT_PLAYBACK_SPEED) {
                float newSpeed = getPlaybackParameters().speed - getSpeedStep();
                newSpeed = max(newSpeed, DEFAULT_PLAYBACK_SPEED);
                log.d("KMediaCodecAudioRenderer", "Setting speed to " + newSpeed);
                setPlaybackParameters(new PlaybackParameters(newSpeed));
                if (newSpeed == DEFAULT_PLAYBACK_SPEED) {
                    speedAdjustedAfterPositionReset = true;
                }
            }
        }

        return super.processOutputBuffer(
                positionUs,
                elapsedRealtimeUs,
                codec,
                buffer,
                bufferIndex,
                bufferFlags,
                sampleCount,
                bufferPresentationTimeUs,
                isDecodeOnlyBuffer,
                isLastBuffer,
                format);
    }
}
