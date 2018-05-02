package com.kaltura.playkit.player.vr;

/**
 * Created by anton.afanasiev on 25/03/2018.
 */

public class VRParams {

    private VRInteractionMode interactionMode = VRInteractionMode.Motion_with_touch;
    private boolean vrModeEnabled; //false by default

    public VRParams setVrModeEnabled(boolean isEnabled) {
        this.vrModeEnabled = isEnabled;
        return this;
    }

    public VRParams setInteractionMode(VRInteractionMode mode) {
        this.interactionMode = mode;
        return this;
    }

    public VRInteractionMode getInteractionMode() {
        return interactionMode;
    }

    public boolean isVrModeEnabled() {
        return vrModeEnabled;
    }
}
