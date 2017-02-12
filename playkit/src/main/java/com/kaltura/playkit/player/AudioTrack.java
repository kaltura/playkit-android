package com.kaltura.playkit.player;

import android.support.annotation.Nullable;

import com.kaltura.playkit.PKPublicAPI;

/**
 * Audio track data holder.
 * Created by anton.afanasiev on 17/11/2016.
 */
@PKPublicAPI
public class AudioTrack extends BaseTrack {

    private long bitrate;
    private String label;
    private String language;

     AudioTrack(String uniqueId, String language, String label, long bitrate, int selectionFlag, boolean isAdaptive) {
        super(uniqueId, selectionFlag, isAdaptive);
        this.label = label;
        this.bitrate = bitrate;
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
     * Getter for the track bitrate.
     * Can be -1 if unknown or not applicable.
     * @return - the bitrate of the track.
     */
    public long getBitrate() {
        return bitrate;
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
