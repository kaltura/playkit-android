package com.kaltura.playkit;

/**
 * Created by anton.afanasiev on 17/11/2016.
 */
public class TextTrackInfo extends BaseTrackInfo{

    private String language;


    public TextTrackInfo(String uniqueId, String language) {
        super(uniqueId, false);
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

}
