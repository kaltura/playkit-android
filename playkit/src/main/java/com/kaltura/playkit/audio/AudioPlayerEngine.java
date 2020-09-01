package com.kaltura.playkit.audio;

public interface AudioPlayerEngine {
    /**
     * Start players playback.
     * The player will start to play as soon as enough
     * data is buffered for playback.
     */
    void play();

    /**
     * Pause the players playback.
     */
    void pause();

}
