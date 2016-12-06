package com.kaltura.playkitdemo;

/**
 * Created by anton.afanasiev on 30/11/2016.
 */
public class TrackItem {

    private String trackName;
    private String uniqueId;

    public TrackItem(String trackName, String uniqueId) {
        this.trackName = trackName;
        this.uniqueId = uniqueId;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getUniqueId() {
        return uniqueId;
    }
}
