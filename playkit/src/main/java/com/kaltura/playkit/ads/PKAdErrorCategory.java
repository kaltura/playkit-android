package com.kaltura.playkit.ads;

public enum PKAdErrorCategory {
    LOAD(1),
    PLAY(2),
    UNKNOWN_CATEGORY(3);

    public final int errorCategory;

    PKAdErrorCategory(int errorCategory) {
        this.errorCategory = errorCategory;
    }
}
