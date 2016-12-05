package com.kaltura.playkit;

/**
 * Text track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
public class TextTrackInfo extends BaseTrackInfo{

    private String language;


    public TextTrackInfo(String uniqueId, String language) {
        super(uniqueId, false);
        this.language = language;
    }

    /**
     * Getter for the track language.
     * @return - the language of the track.
     */
    public String getLanguage() {
        return language;
    }

}
