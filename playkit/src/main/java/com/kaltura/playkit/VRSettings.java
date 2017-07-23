package com.kaltura.playkit;

/**
 * Created by anton.afanasiev on 18/07/2017.
 */

public class VRSettings {

    private boolean support360;

    public VRSettings(boolean support360) {
        this.support360 = support360;
    }

    public boolean is360Supported() {
        return support360;
    }

    public void setSupport360(boolean support360) {
        this.support360 = support360;
    }
}
