package com.kaltura.playkit;

/**
 * Created by Noam Tamim @ Kaltura on 18/09/2016.
 */
public class PlayerConfig {
    private boolean shouldAutoPlay = false;
    private long startPosition = 0;
    private PKMediaEntry entry;

    public boolean shouldAutoPlay() {
        return shouldAutoPlay;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public PKMediaEntry getEntry() {
        return entry;
    }

    public void setShouldAutoPlay(boolean shouldAutoPlay) {
        this.shouldAutoPlay = shouldAutoPlay;
    }

    public void setStartPosition(long startPosition) {
        this.startPosition = startPosition;
    }

    public void setEntry(PKMediaEntry entry) {
        this.entry = entry;
    }
}
