package com.kaltura.playkit;

/**
 * Audio track data holder.
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

    /**
     * Getter for the track language.
     * @return - the language of the track.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Getter for the track bitrate.
     * @return - the bitrate of the track.
     */
    public long getBitrate() {
        return bitrate;
    }
}
