package com.kaltura.playkit.player;

/**
 * Created by anton.afanasiev on 06/12/2016.
 */

public class TrackItem {

    private String uniqueId;
    private String trackLanguage;

    public TrackItem(String uniqueId, String trackLanguage) {
        this.uniqueId = uniqueId;
        this.trackLanguage = trackLanguage;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getTrackLanguage() {
        return trackLanguage;
    }

}
