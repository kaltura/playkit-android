package com.kaltura.playkit;

/**
 * Created by anton.afanasiev on 17/11/2016.
 */
public class AudioTrackInfo extends BaseTrackInfo{

    private String language;

    public AudioTrackInfo(String language, String uniqueId, int groupIndex, int trackIndex) {
        super(uniqueId, groupIndex, trackIndex);
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }
}
