package com.kaltura.android.exoplayer2.video;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.kaltura.android.exoplayer2.ExoPlaybackException;
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecAdapter;
import com.kaltura.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.kaltura.playkit.PKLog;

public class KMediaCodecVideoRenderer extends MediaCodecVideoRenderer{

    private boolean renderedFirstFrameAfterResetAfterReady = false;

    private boolean shouldNotifyRenderedFirstFrameAfterStarted = false;

    private static final PKLog log = PKLog.get("KMediaCodecVideoRenderer");

    @Nullable private KVideoRendererFirstFrameWhenStartedEventListener rendererFirstFrameWhenStartedEventListener;

    public KMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector) {
        super(context, mediaCodecSelector);
    }

    public KMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs);
    }

    public KMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs, eventHandler, eventListener, maxDroppedFramesToNotify);
    }

    public KMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, boolean enableDecoderFallback, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs, enableDecoderFallback, eventHandler, eventListener, maxDroppedFramesToNotify);
    }

    public KMediaCodecVideoRenderer(Context context,
                                    MediaCodecAdapter.Factory codecAdapterFactory,
                                    MediaCodecSelector mediaCodecSelector,
                                    long allowedJoiningTimeMs,
                                    boolean enableDecoderFallback,
                                    @Nullable Handler eventHandler,
                                    @Nullable VideoRendererEventListener eventListener,
                                    int maxDroppedFramesToNotify,
                                    KVideoRendererFirstFrameWhenStartedEventListener rendererFirstFrameWhenStartedEventListener) {
        super(context, codecAdapterFactory, mediaCodecSelector, allowedJoiningTimeMs, enableDecoderFallback, eventHandler, eventListener, maxDroppedFramesToNotify);
        this.rendererFirstFrameWhenStartedEventListener = rendererFirstFrameWhenStartedEventListener;
    }

    public KMediaCodecVideoRenderer(Context context, MediaCodecAdapter.Factory codecAdapterFactory, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, boolean enableDecoderFallback, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify, float assumedMinimumCodecOperatingRate) {
        super(context, codecAdapterFactory, mediaCodecSelector, allowedJoiningTimeMs, enableDecoderFallback, eventHandler, eventListener, maxDroppedFramesToNotify, assumedMinimumCodecOperatingRate);
    }

    @Override
    void maybeNotifyRenderedFirstFrame() {
        super.maybeNotifyRenderedFirstFrame();
        if (this.shouldNotifyRenderedFirstFrameAfterStarted) {
            log.d("KMediaCodecVideoRenderer", "maybeNotifyRenderedFirstFrame");
            this.shouldNotifyRenderedFirstFrameAfterStarted = false;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (rendererFirstFrameWhenStartedEventListener != null) {
                    log.d("KMediaCodecVideoRenderer", "onRenderedFirstFrameWhenStarted");
                    rendererFirstFrameWhenStartedEventListener.onRenderedFirstFrameWhenStarted();
                }
            });
        }
    }

    @Override
    protected void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        log.d("KMediaCodecVideoRenderer", "onPositionReset() called with: positionUs = [" + positionUs + "], joining = [" + joining + "]");
        super.onPositionReset(positionUs, joining);
        this.renderedFirstFrameAfterResetAfterReady = false;
        this.shouldNotifyRenderedFirstFrameAfterStarted = false;
    }

    @RequiresApi(21)
    @Override
    protected void renderOutputBufferV21(MediaCodecAdapter codec, int index, long presentationTimeUs, long releaseTimeNs) {
        if(getState() == STATE_STARTED) {
            if(!this.renderedFirstFrameAfterResetAfterReady) {
                this.renderedFirstFrameAfterResetAfterReady = true;
                this.shouldNotifyRenderedFirstFrameAfterStarted = true;
            }
        }
        super.renderOutputBufferV21(codec, index, presentationTimeUs, releaseTimeNs);
    }
}
