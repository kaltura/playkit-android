package com.kaltura.playkit.player;

// Sets the maximum allowed video width and height.
public class PKMaxVideoSize {

    private int maxVideoWidth = Integer.MAX_VALUE;
    private int maxVideoHeight = Integer.MAX_VALUE;

    public PKMaxVideoSize(int maxVideoWidth, int maxVideoHeight) {
        setMaxVideoWidth(maxVideoWidth);
        setMaxVideoHeight(maxVideoHeight);
    }

    public int getMaxVideoWidth() {
        return maxVideoWidth;
    }

    public PKMaxVideoSize setMaxVideoWidth(int maxVideoWidth) {
        this.maxVideoWidth = (maxVideoWidth == 0) ? Integer.MAX_VALUE : maxVideoWidth;
        return this;
    }

    public int getMaxVideoHeight() {
        return maxVideoHeight;
    }

    public PKMaxVideoSize setMaxVideoHeight(int maxVideoHeight) {
        this.maxVideoHeight = (maxVideoHeight == 0) ? Integer.MAX_VALUE : maxVideoHeight;
        return this;
    }
}