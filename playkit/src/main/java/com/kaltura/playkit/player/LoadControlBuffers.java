package com.kaltura.playkit.player;

import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_MAX_BUFFER_MS;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_MIN_BUFFER_MS;

public class LoadControlBuffers {

    private int minPlayerBufferMs = DEFAULT_MIN_BUFFER_MS; //The default minimum duration of media that the player will attempt to ensure is buffered at all
    private int maxPlayerBufferMs = DEFAULT_MAX_BUFFER_MS; //The default maximum duration of media that the player will attempt to buffer
    private int minBufferAfterInteractionMs = DEFAULT_BUFFER_FOR_PLAYBACK_MS; //The default duration of media that must be buffered for playback to start or resume following a user action such as a seek
    private int minBufferAfterReBufferMs = DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS; //The default duration of media that must be buffered for playback after re-buffering

    public int getMinPlayerBufferMs() {
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
}
