package com.kaltura.playkit.player.vr;

import javax.annotation.Nonnull;

/**
 * Created by anton.afanasiev on 25/03/2018.
 */

public class VRSettings {

    private VRInteractionMode interactionMode = VRInteractionMode.Touch;
    private boolean vrModeEnabled; //false by default
    private boolean zoomWithPinchEnabled = true; // true by default.
    private boolean flingEnabled; //false by default.
    private VRDistortionConfig vrDistortionConfig;

    /**
     * Allows to enable/disable VR mode. Where content is shown in
     * 2 split screens allowing to consume it on vr devices.
     * Default is false, which means content will be displayed in regular 360 mode.
     *
     * @param isEnabled - should enable.
     * @return - {@link VRSettings}
     */
    public VRSettings setVrModeEnabled(boolean isEnabled) {
        this.vrModeEnabled = isEnabled;
        return this;
    }

    /**
     * Configure user interaction with surface on which content is rendered.
     * Default is VRInteractionMode.Motion_with_touch which means that surface will react
     * both on device movement and user touch input.
     *
     * @param mode - desired mode.
     * @return - {@link VRSettings}
     */
    public VRSettings setInteractionMode(VRInteractionMode mode) {
        this.interactionMode = mode;
        return this;
    }

    /**
     * Allows to enable/disable zoom in/out with pinch.
     * Default is - true.
     *
     * @param shouldEnable - should enable zoom in with pinch.
     * @return - {@link VRSettings}
     */
    public VRSettings setZoomWithPinchEnabled(boolean shouldEnable) {
        this.zoomWithPinchEnabled = shouldEnable;
        return this;
    }

    /**
     * Allows to enable/disable fling gesture on surface. When set to true
     * surface will continue display motion animation depending on acceleration power of fling gesture.
     * Default is - false.
     *
     * @param shouldEnable - should enable fling.
     * @return - {@link VRSettings}
     */
    public VRSettings setFlingEnabled(boolean shouldEnable) {
        this.flingEnabled = shouldEnable;
        return this;
    }

    public VRSettings setVrDistortionConfig(@Nonnull VRDistortionConfig vrDistortionConfig) {
        this.vrDistortionConfig = vrDistortionConfig;
        return this;
    }

    public VRInteractionMode getInteractionMode() {
        return interactionMode;
    }

    public boolean isVrModeEnabled() {
        return vrModeEnabled;
    }

    public boolean isZoomWithPinchEnabled() {
        return zoomWithPinchEnabled;
    }

    public boolean isFlingEnabled() {
        return flingEnabled;
    }

    public VRDistortionConfig getVrDistortionConfig() {
        return vrDistortionConfig;
    }
}
