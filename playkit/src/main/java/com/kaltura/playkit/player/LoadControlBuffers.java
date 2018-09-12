package com.kaltura.playkit.player;

import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_MAX_BUFFER_MS;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_MIN_BUFFER_MS;

public class LoadControlBuffers {

    private int minPlayerBufferMS = DEFAULT_MIN_BUFFER_MS; //The default minimum duration of media that the player will attempt to ensure is buffered at all
    private int maxPlayerBufferMS = DEFAULT_MAX_BUFFER_MS; //The default maximum duration of media that the player will attempt to buffer
    private int minBufferAfterInteractionMS = DEFAULT_BUFFER_FOR_PLAYBACK_MS; //The default duration of media that must be buffered for playback to start or resume following a user action such as a seek
    private int minBufferAfterReBufferMS = DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS; //The default duration of media that must be buffered for playback after re-buffering

    public int getMinPlayerBufferMS() {
        return minPlayerBufferMS;
    }

    public LoadControlBuffers setMinPlayerBufferMS(int minPlayerBufferMS) {
        this.minPlayerBufferMS = minPlayerBufferMS;
        return this;
    }

    public int getMaxPlayerBufferMS() {
        if (maxPlayerBufferMS < minPlayerBufferMS) {
            return minPlayerBufferMS;
        }
        return maxPlayerBufferMS;
    }

    public LoadControlBuffers setMaxPlayerBufferMS(int maxPlayerBufferMS) {
        this.maxPlayerBufferMS = maxPlayerBufferMS;
        return this;
    }

    public int getMinBufferAfterInteractionMS() {
        if (minPlayerBufferMS < minBufferAfterInteractionMS) {
            return minPlayerBufferMS;
        }
        return minBufferAfterInteractionMS;
    }

    public LoadControlBuffers setMinBufferAfterInteractionMS(int minBufferAfterInteractionMS) {
        this.minBufferAfterInteractionMS = minBufferAfterInteractionMS;
        return this;
    }

    public int getMinBufferAfterReBufferMS() {
        if (minPlayerBufferMS < minBufferAfterReBufferMS) {
            return minPlayerBufferMS;
        }
        return minBufferAfterReBufferMS;
    }

    public LoadControlBuffers setMinBufferAfterReBufferMS(int minBufferAfterReBufferMS) {
        this.minBufferAfterReBufferMS = minBufferAfterReBufferMS;
        return this;
    }
}
