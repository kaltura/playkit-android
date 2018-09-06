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

import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.Player;

import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_MAX_BUFFER_MS;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_MIN_BUFFER_MS;


public class PlayerSettings implements Player.Settings {

    private boolean useTextureView;
    private boolean isSurfaceSecured;
    private boolean cea608CaptionsEnabled;
    private boolean crossProtocolRedirectEnabled;
    private boolean adAutoPlayOnResume = true;
    private boolean vrPlayerEnabled = true;

    private int minPlayerBufferMS = DEFAULT_MIN_BUFFER_MS;
    private int maxPlayerBufferMS = DEFAULT_MAX_BUFFER_MS;
    private int minBufferAfterInteractionMS = DEFAULT_BUFFER_FOR_PLAYBACK_MS;

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

    public boolean cea608CaptionsEnabled() {
        return cea608CaptionsEnabled;
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

    public PKTrackConfig getPreferredTextTrackConfig() {
        return preferredTextTrackConfig;
    }

    public PKTrackConfig getPreferredAudioTrackConfig() {
        return preferredAudioTrackConfig;
    }

    public PKMediaFormat getPreferredMediaFormat() {
        return preferredMediaFormat;
    }

    public int getMinPlayerBufferMS() {
        return minPlayerBufferMS;
    }

    public int getMaxPlayerBufferMS() {
        return maxPlayerBufferMS;
    }

    public int getMinBufferAfterInteractionMS() {
        return minBufferAfterInteractionMS;
    }

    @Override
    public PlayerSettings setVRPlayerEnabled(boolean vrPlayerEnabled) {
        this.vrPlayerEnabled = vrPlayerEnabled;
        return this;
    }

    @Override
    public PlayerSettings setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter) {
        this.contentRequestAdapter = contentRequestAdapter;
        return this;
    }

    @Override
    public PlayerSettings setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter) {
        this.licenseRequestAdapter = licenseRequestAdapter;
        return this;
    }

    @Override
    public PlayerSettings setCea608CaptionsEnabled(boolean cea608CaptionsEnabled) {
        this.cea608CaptionsEnabled = cea608CaptionsEnabled;
        return this;
    }

    @Override
    public PlayerSettings useTextureView(boolean useTextureView) {
        this.useTextureView = useTextureView;
        return this;
    }

    @Override
    public PlayerSettings setSecureSurface(boolean isSurfaceSecured) {
        this.isSurfaceSecured = isSurfaceSecured;
        return this;
    }

    @Override
    public PlayerSettings setAdAutoPlayOnResume(boolean adAutoPlayOnResume) {
        this.adAutoPlayOnResume = adAutoPlayOnResume;
        return this;
    }

    @Override
    public PlayerSettings setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig) {
        this.preferredAudioTrackConfig = preferredAudioTrackConfig;
        return this;
    }

    @Override
    public PlayerSettings setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig) {
        this.preferredTextTrackConfig = preferredTextTrackConfig;
        return this;
    }

    @Override
    public PlayerSettings setPreferredMediaFormat(PKMediaFormat preferredMediaFormat) {
        this.preferredMediaFormat = preferredMediaFormat;
        return this;
    }


    @Override
    public PlayerSettings setAllowCrossProtocolRedirect(boolean crossProtocolRedirectEnabled) {
        this.crossProtocolRedirectEnabled = crossProtocolRedirectEnabled;
        return this;
    }

    @Override
    public PlayerSettings setPlayerBuffers(int minPlayerBufferMS, int maxPlayerBufferMS, int minBufferAfterInteractionMS) {
        this.minPlayerBufferMS = minPlayerBufferMS;
        this.maxPlayerBufferMS = maxPlayerBufferMS;
        this.minBufferAfterInteractionMS = minBufferAfterInteractionMS;
        return this;
    }
}