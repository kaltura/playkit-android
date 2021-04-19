package com.kaltura.android.exoplayer2.video;

import com.kaltura.android.exoplayer2.DefaultLoadControl;
import com.kaltura.android.exoplayer2.upstream.DefaultAllocator;

public class CustomLoadControl extends DefaultLoadControl {
        public CustomLoadControl(DefaultAllocator allocator,
                                 int minBufferMs,
                                 int maxBufferMs,
                                 int bufferForPlaybackMs,
                                 int bufferForPlaybackAfterRebufferMs,
                                 int targetBufferBytes,
                                 boolean prioritizeTimeOverSizeThresholds,
                                 int backBufferDurationMs,
                                 boolean retainBackBufferFromKeyframe)
        {
            super(allocator,
                    minBufferMs,
                    maxBufferMs,
                    bufferForPlaybackMs,
                    bufferForPlaybackAfterRebufferMs,
                    targetBufferBytes,
                    prioritizeTimeOverSizeThresholds,
                    backBufferDurationMs,
                    retainBackBufferFromKeyframe);
        }
}
