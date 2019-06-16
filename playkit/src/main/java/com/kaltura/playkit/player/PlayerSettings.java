/*
 * ============================================================================
 * Copyright (C) 2018 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.player;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.player.vr.VRInteractionMode;
import com.kaltura.playkit.player.vr.VRSettings;

public class PlayerSettings implements Player.Settings {

    private boolean useTextureView;
    private boolean isSurfaceSecured;
    private boolean cea608CaptionsEnabled;
    private boolean mpgaAudioFormatEnabled;
    private boolean crossProtocolRedirectEnabled;
    private boolean allowClearLead = true;
    private boolean adAutoPlayOnResume = true;
    private boolean vrPlayerEnabled = true;
    private boolean isVideoViewHidden;
    private LoadControlBuffers loadControlBuffers = new LoadControlBuffers();
    private SubtitleStyleSettings subtitleStyleSettings;
    private PKAspectRatioResizeMode resizeMode = PKAspectRatioResizeMode.fit;
    private ABRSettings abrSettings = new ABRSettings();
    private VRSettings vrSettings;
    /**
     * Flag helping to check if client app wants to use a single player instance at a time
     * Only if IMA plugin is there then only this flag is set to true.
     */
    private boolean useSinglePlayerInstance = false;

    private PKTrackConfig preferredTextTrackConfig;
    private PKTrackConfig preferredAudioTrackConfig;

    private PKMediaFormat preferredMediaFormat = PKMediaFormat.dash;

    private PKRequestParams.Adapter contentRequestAdapter;
    private PKRequestParams.Adapter licenseRequestAdapter;

    public PKRequestParams.Adapter getContentRequestAdapter() {
        return contentRequestAdapter;
    }

    public PKRequestParams.Adapter getLicenseRequestAdapter() {
        return licenseRequestAdapter;
    }

    public boolean useTextureView() {
        return useTextureView;
    }

    public boolean crossProtocolRedirectEnabled() {
        return crossProtocolRedirectEnabled;
    }

    public boolean allowClearLead() {
        return allowClearLead;
    }

    public boolean cea608CaptionsEnabled() {
        return cea608CaptionsEnabled;
    }

    public boolean mpgaAudioFormatEnabled() {
        return mpgaAudioFormatEnabled;
    }

    public boolean isSurfaceSecured() {
        return isSurfaceSecured;
    }

    public boolean isAdAutoPlayOnResume() {
        return adAutoPlayOnResume;
    }

    public boolean isVRPlayerEnabled() {
        return vrPlayerEnabled;
    }

    public boolean isVideoViewHidden() {
        return isVideoViewHidden;
    }

    public PKTrackConfig getPreferredTextTrackConfig() {
        return preferredTextTrackConfig;
    }

    public PKTrackConfig getPreferredAudioTrackConfig() {
        return preferredAudioTrackConfig;
    }

    public PKMediaFormat getPreferredMediaFormat() {
        return preferredMediaFormat;
    }

    public LoadControlBuffers getLoadControlBuffers() {
        return loadControlBuffers;
    }

    public SubtitleStyleSettings getSubtitleStyleSettings() {
        return subtitleStyleSettings;
    }

    public ABRSettings getAbrSettings() {
        return abrSettings;
    }

    public PKAspectRatioResizeMode getAspectRatioResizeMode(){
        return resizeMode;
    }

    public VRSettings getVRSettings() {
        return vrSettings;
    }

    public boolean isUseSinglePlayerInstance() {
        return useSinglePlayerInstance;
    }

    @Override
    public Player.Settings setVRPlayerEnabled(boolean vrPlayerEnabled) {
        this.vrPlayerEnabled = vrPlayerEnabled;
        return this;
    }

    @Override
    public Player.Settings setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter) {
        this.contentRequestAdapter = contentRequestAdapter;
        return this;
    }

    @Override
    public Player.Settings setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter) {
        this.licenseRequestAdapter = licenseRequestAdapter;
        return this;
    }

    @Override
    public Player.Settings setCea608CaptionsEnabled(boolean cea608CaptionsEnabled) {
        this.cea608CaptionsEnabled = cea608CaptionsEnabled;
        return this;
    }

    @Override
    public Player.Settings setMpgaAudioFormatEnabled(boolean mpgaAudioFormatEnabled) {
        this.mpgaAudioFormatEnabled = mpgaAudioFormatEnabled;
        return this;
    }

    @Override
    public Player.Settings useTextureView(boolean useTextureView) {
        this.useTextureView = useTextureView;
        return this;
    }

    @Override
    public Player.Settings setSecureSurface(boolean isSurfaceSecured) {
        this.isSurfaceSecured = isSurfaceSecured;
        return this;
    }

    @Override
    public Player.Settings setAdAutoPlayOnResume(boolean adAutoPlayOnResume) {
        this.adAutoPlayOnResume = adAutoPlayOnResume;
        return this;
    }

    @Override
    public Player.Settings setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig) {
        this.preferredAudioTrackConfig = preferredAudioTrackConfig;
        return this;
    }

    @Override
    public Player.Settings setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig) {
        this.preferredTextTrackConfig = preferredTextTrackConfig;
        return this;
    }

    @Override
    public Player.Settings setPreferredMediaFormat(PKMediaFormat preferredMediaFormat) {
        this.preferredMediaFormat = preferredMediaFormat;
        return this;
    }


    @Override
    public Player.Settings setAllowCrossProtocolRedirect(boolean crossProtocolRedirectEnabled) {
        this.crossProtocolRedirectEnabled = crossProtocolRedirectEnabled;
        return this;
    }

    @Override
    public Player.Settings allowClearLead(boolean allowClearLead) {
        this.allowClearLead = allowClearLead;
        return this;
    }

    @Override
    public Player.Settings setPlayerBuffers(LoadControlBuffers loadControlBuffers) {
        this.loadControlBuffers = loadControlBuffers;
        return this;
    }

    @Override
    public Player.Settings setSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings) {
        this.subtitleStyleSettings = subtitleStyleSettings;
        return this;
    }

    @Override
    public Player.Settings setABRSettings(ABRSettings abrSettings) {
        this.abrSettings = abrSettings;
        return this;
    }

    @Override
    public Player.Settings setSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        this.resizeMode = resizeMode;
        return this;
    }

    @Override
    public Player.Settings useSinglePlayerInstance(boolean isRequired) {
        useSinglePlayerInstance = isRequired;
        return this;
    }
  
    @Override
    public Player.Settings setHideVideoViews(boolean hide) {
        isVideoViewHidden = hide;
        return this;
    }

    @Override
    public Player.Settings setVRSettings(VRSettings vrSettings) {
        this.vrSettings = vrSettings;
        return this;
    }

    private boolean isVRModeSupported(Context context, VRInteractionMode mode) {
        switch (mode) {
            case Touch:
                //Always supported
                return true;
            case Motion:
            case MotionWithTouch:
                SensorManager motionSensorManager = (SensorManager) context
                        .getSystemService(Context.SENSOR_SERVICE);
                return motionSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null;
            case CardboardMotion:
            case CardboardMotionWithTouch:
                SensorManager cardboardSensorManager = (SensorManager) context
                        .getSystemService(Context.SENSOR_SERVICE);
                Sensor accelerometerSensor = cardboardSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                Sensor gyroSensor = cardboardSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                return accelerometerSensor != null && gyroSensor != null;
            default:
                return true;
        }
    }
}
