package com.kaltura.playkit;

public enum PKAbrFilter {
    NONE,
    BITRATE,
    HEIGHT,
    WIDTH,
    PIXEL;

    public static final String KEY_ABR_TYPE = "abrType";
    public static final String KEY_ABR_MIN = "abrMin";
    public static final String KEY_ABR_MAX = "abrMax";
}
