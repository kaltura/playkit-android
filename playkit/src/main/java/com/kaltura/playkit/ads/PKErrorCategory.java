package com.kaltura.playkit.ads;

public enum PKErrorCategory {
    LOAD(1),
    PLAY(2),
    UNKNOWN(3);

    public final int errorCategory;

    PKErrorCategory(int errorCategory) {
        this.errorCategory = errorCategory;
    }
}
