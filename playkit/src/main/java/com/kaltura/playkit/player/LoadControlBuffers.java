package com.kaltura.playkit.player;

import static com.kaltura.androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_BACK_BUFFER_DURATION_MS;
import static com.kaltura.androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;
import static com.kaltura.androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS;
import static com.kaltura.androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MAX_BUFFER_MS;
import static com.kaltura.androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MIN_BUFFER_MS;
import static com.kaltura.androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS;
import static com.kaltura.androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME;
import static com.kaltura.androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES;
import static com.kaltura.androidx.media3.exoplayer.DefaultRenderersFactory.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS;

public class LoadControlBuffers {

    private int minPlayerBufferMs = DEFAULT_MIN_BUFFER_MS; //The default minimum duration of media that the player will attempt to ensure is buffered at all
    private int maxPlayerBufferMs = DEFAULT_MAX_BUFFER_MS; //The default maximum duration of media that the player will attempt to buffer
    private int minBufferAfterInteractionMs = DEFAULT_BUFFER_FOR_PLAYBACK_MS; //The default duration of media that must be buffered for playback to start or resume following a user action such as a seek
    private int minBufferAfterReBufferMs = DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS; //The default duration of media that must be buffered for playback after re-buffering
    private int backBufferDurationMs = DEFAULT_BACK_BUFFER_DURATION_MS;
    private boolean retainBackBufferFromKeyframe = DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME;
    private long allowedVideoJoiningTimeMs = DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS; //Maximum duration for which a video renderer can attempt to seamlessly join an ongoing playback. Default is 5000ms
    private int targetBufferBytes = DEFAULT_TARGET_BUFFER_BYTES;
    private boolean prioritizeTimeOverSizeThresholds = DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS;

    public int getMinPlayerBufferMs() {
        if (maxPlayerBufferMs < minPlayerBufferMs) {
            return maxPlayerBufferMs;
        }
        return minPlayerBufferMs;
    }

    public LoadControlBuffers setMinPlayerBufferMs(int minPlayerBufferMs) {
        this.minPlayerBufferMs = minPlayerBufferMs;
        return this;
    }

    public int getMaxPlayerBufferMs() {
        if (maxPlayerBufferMs < minPlayerBufferMs) {
            return minPlayerBufferMs;
        }
        return maxPlayerBufferMs;
    }

    public LoadControlBuffers setMaxPlayerBufferMs(int maxPlayerBufferMs) {
        this.maxPlayerBufferMs = maxPlayerBufferMs;
        return this;
    }

    public int getMinBufferAfterInteractionMs() {
        if (minPlayerBufferMs < minBufferAfterInteractionMs) {
            return minPlayerBufferMs;
        }
        return minBufferAfterInteractionMs;
    }

    public LoadControlBuffers setMinBufferAfterInteractionMs(int minBufferAfterInteractionMs) {
        this.minBufferAfterInteractionMs = minBufferAfterInteractionMs;
        return this;
    }

    public int getMinBufferAfterReBufferMs() {
        if (minPlayerBufferMs < minBufferAfterReBufferMs) {
            return minPlayerBufferMs;
        }
        return minBufferAfterReBufferMs;
    }

    public LoadControlBuffers setMinBufferAfterReBufferMs(int minBufferAfterReBufferMs) {
        this.minBufferAfterReBufferMs = minBufferAfterReBufferMs;
        return this;
    }

    public int getBackBufferDurationMs() {
        return backBufferDurationMs;
    }

    public LoadControlBuffers setBackBufferDurationMs(int backBufferDurationMs) {
        this.backBufferDurationMs = backBufferDurationMs;
        return this;
    }

    public boolean getRetainBackBufferFromKeyframe() {
        return retainBackBufferFromKeyframe;
    }

    public LoadControlBuffers setRetainBackBufferFromKeyframe(boolean retainBackBufferFromKeyframe) {
        this.retainBackBufferFromKeyframe = retainBackBufferFromKeyframe;
        return this;
    }

    public long getAllowedVideoJoiningTimeMs() {
        return allowedVideoJoiningTimeMs;
    }

    public LoadControlBuffers setAllowedVideoJoiningTimeMs(long allowedVideoJoiningTimeMs) {
        this.allowedVideoJoiningTimeMs = allowedVideoJoiningTimeMs;
        return this;
    }

    public int getTargetBufferBytes() {
        return targetBufferBytes;
    }

    public LoadControlBuffers setTargetBufferBytes(int targetBufferBytes) {
        this.targetBufferBytes = targetBufferBytes;
        return this;
    }

    public boolean getPrioritizeTimeOverSizeThresholds() {
        return prioritizeTimeOverSizeThresholds;
    }

    public LoadControlBuffers setPrioritizeTimeOverSizeThresholds(boolean prioritizeTimeOverSizeThresholds) {
        this.prioritizeTimeOverSizeThresholds = prioritizeTimeOverSizeThresholds;
        return this;
    }

    public boolean isDefaultValuesModified() {
        return minPlayerBufferMs != DEFAULT_MIN_BUFFER_MS ||
                maxPlayerBufferMs != DEFAULT_MAX_BUFFER_MS ||
                minBufferAfterInteractionMs != DEFAULT_BUFFER_FOR_PLAYBACK_MS ||
                minBufferAfterReBufferMs != DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS ||
                backBufferDurationMs != DEFAULT_BACK_BUFFER_DURATION_MS ||
                retainBackBufferFromKeyframe != DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME ||
                allowedVideoJoiningTimeMs != DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS ||
                targetBufferBytes != DEFAULT_TARGET_BUFFER_BYTES ||
                prioritizeTimeOverSizeThresholds != DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS;
    }
}
