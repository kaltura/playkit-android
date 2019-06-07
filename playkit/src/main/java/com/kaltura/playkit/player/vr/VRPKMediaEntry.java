package com.kaltura.playkit.player.vr;

import com.kaltura.playkit.PKMediaEntry;

public class VRPKMediaEntry extends PKMediaEntry {

    private VRSettings vrSettings;

    public VRPKMediaEntry() {
        super();
        //init with default settings.
        vrSettings = new VRSettings();
    }

    public VRPKMediaEntry setVRParams(VRSettings vrSettings) {
        this.vrSettings = vrSettings;
        return this;
    }

    public VRSettings getVrSettings() {
        return this.vrSettings;
    }

    public boolean hasVRParams() {
        return vrSettings != null;
    }

}
