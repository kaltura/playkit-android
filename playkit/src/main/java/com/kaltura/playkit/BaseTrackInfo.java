package com.kaltura.playkit;

/**
 * Base track info object that is common to all the trackInfo types.
 * Created by anton.afanasiev on 27/11/2016.
 */

public abstract class BaseTrackInfo {

    private String uniqueId;
    private int groupIndex;
    private int trackIndex;

    public BaseTrackInfo(String uniqueId, int groupIndex, int trackIndex) {
        this.uniqueId = uniqueId;
        this.groupIndex = groupIndex;
        this.trackIndex = trackIndex;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    protected int getGroupIndex() {
        return groupIndex;
    }

    protected int getTrackIndex() {
        return trackIndex;
    }
}
