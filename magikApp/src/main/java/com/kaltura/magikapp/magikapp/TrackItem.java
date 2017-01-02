package com.kaltura.magikapp.magikapp;

/**
 * Created by anton.afanasiev on 06/12/2016.
 */

public class TrackItem {

    private String uniqueId;
    private String trackDescription;

    public TrackItem(String uniqueId, String trackDescription) {
        this.uniqueId = uniqueId;
        this.trackDescription = trackDescription;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getTrackDescription() {
        return trackDescription;
    }

}
