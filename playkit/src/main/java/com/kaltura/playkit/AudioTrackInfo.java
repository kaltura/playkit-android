package com.kaltura.playkit;

/**
 * Created by anton.afanasiev on 17/11/2016.
 */
public class AudioTrackInfo extends BaseTrackInfo{

    private String language;
    private long bitrate;

    public AudioTrackInfo(String uniqueId, String language, long bitrate, boolean isAdaptive) {
        super(uniqueId, isAdaptive);
        this.language = language;
        this.bitrate = bitrate;
    }

    public String getLanguage() {
        return language;
    }

    public long getBitrate() {
        return bitrate;
    }
}
