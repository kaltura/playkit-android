package com.kaltura.playkit;

/**
 * Base track info object that is common to all the trackInfo types.
 * Created by anton.afanasiev on 27/11/2016.
 */

public abstract class BaseTrackInfo {

    private String uniqueId;
    private boolean isAdaptive;

    public BaseTrackInfo(String uniqueId, boolean isAdaptive) {
        this.uniqueId = uniqueId;
        this.isAdaptive = isAdaptive;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public boolean isAdaptive() {
        return isAdaptive;
    }
}
