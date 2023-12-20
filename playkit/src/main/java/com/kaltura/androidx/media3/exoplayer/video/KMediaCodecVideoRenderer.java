package com.kaltura.androidx.media3.exoplayer.video;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.kaltura.androidx.media3.exoplayer.ExoPlaybackException;
import com.kaltura.androidx.media3.exoplayer.mediacodec.MediaCodecAdapter;
import com.kaltura.androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import com.kaltura.playkit.PKLog;

public class KMediaCodecVideoRenderer extends MediaCodecVideoRenderer {

    private boolean renderedFirstFrameAfterResetAfterReady = false;

    private boolean shouldNotifyRenderedFirstFrameAfterStarted = false;

    private static final PKLog log = PKLog.get("KMediaCodecVideoRenderer");

    @Nullable private KVideoRendererFirstFrameWhenStartedEventListener rendererFirstFrameWhenStartedEventListener;

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

    // TODO: This should be revisited once migartion to media3 is complete.
    //  Seems like it's not needed anymore for media3
//    @Override
//    void maybeNotifyRenderedFirstFrame() {
//        super.maybeNotifyRenderedFirstFrame();
//        if (this.shouldNotifyRenderedFirstFrameAfterStarted) {
//            log.d("KMediaCodecVideoRenderer", "maybeNotifyRenderedFirstFrame");
//            this.shouldNotifyRenderedFirstFrameAfterStarted = false;
//            new Handler(Looper.getMainLooper()).post(() -> {
//                if (rendererFirstFrameWhenStartedEventListener != null) {
//                    log.d("KMediaCodecVideoRenderer", "onRenderedFirstFrameWhenStarted");
//                    rendererFirstFrameWhenStartedEventListener.onRenderedFirstFrameWhenStarted();
//                }
//            });
//        }
//    }

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
