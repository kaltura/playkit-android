package com.kaltura.android.exoplayer2.video;

import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.DefaultLoadControl;
import com.kaltura.android.exoplayer2.upstream.DefaultAllocator;

public class CustomLoadControl extends DefaultLoadControl {
        public CustomLoadControl(DefaultAllocator allocator,
                                 int minBufferAudioMs,
                                 int minBufferVideoMs,
                                 int maxBufferMs,
                                 int bufferForPlaybackMs,
                                 int bufferForPlaybackAfterRebufferMs,
                                 int targetBufferBytes,
                                 boolean prioritizeTimeOverSizeThresholds,
                                 int backBufferDurationMs,
                                 boolean retainBackBufferFromKeyframe)
        {
            super(allocator,
                    minBufferAudioMs,
                    minBufferVideoMs,
                    maxBufferMs,
                    bufferForPlaybackMs,
                    bufferForPlaybackAfterRebufferMs,
                    targetBufferBytes,
                    prioritizeTimeOverSizeThresholds,
                    backBufferDurationMs,
                    retainBackBufferFromKeyframe);
        }
}
