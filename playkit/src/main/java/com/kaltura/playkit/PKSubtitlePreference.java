package com.kaltura.playkit;

public enum PKSubtitlePreference {
    INTERNAL(1),
    EXTERNAL(2),
    OFF(3);

    public final int subtitlePreference;

    PKSubtitlePreference(int subtitlePreference) {
        this.subtitlePreference = subtitlePreference;
    }
}
