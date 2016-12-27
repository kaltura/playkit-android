package com.kaltura.playkit;

import android.support.annotation.Nullable;

/**
 * Text track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
public class TextTrack extends BaseTrack {

    private String label;
    private String language;


    public TextTrack(String uniqueId, String language, String label) {
        super(uniqueId, false);
        this.label = label;
        this.language = language;
    }

    /**
     * Getter for the track language.
     * Can be null if the language is unknown.
     * @return - the language of the track.
     */
    public @Nullable String getLanguage() {
        return language;
    }

    /**
     * Getter for the track label.
     * Can be null if the label is unknown.
     * @return - the label of the track.
     */
    public @Nullable String getLabel() {
        return label;
    }

}
