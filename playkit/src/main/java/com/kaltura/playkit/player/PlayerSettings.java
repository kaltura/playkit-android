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
import com.kaltura.playkit.PKSubtitlePreference;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.PKWakeMode;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.player.vr.VRSettings;

public class PlayerSettings implements Player.Settings {

    private boolean useTextureView;
    private boolean isSurfaceSecured;
    private boolean cea608CaptionsEnabled;
    private boolean mpgaAudioFormatEnabled;
    private boolean crossProtocolRedirectEnabled;
    private boolean enableDecoderFallback;
    private boolean allowClearLead = true;
    private boolean adAutoPlayOnResume = true;
    private boolean vrPlayerEnabled = true;
    private boolean isVideoViewHidden;
    private VideoCodecSettings preferredVideoCodecSettings = new VideoCodecSettings();
    private AudioCodecSettings preferredAudioCodecSettings = new AudioCodecSettings();
    private boolean isTunneledAudioPlayback;
    private boolean handleAudioBecomingNoisyEnabled;
    private PKWakeMode wakeMode = PKWakeMode.NONE;
    private boolean handleAudioFocus;
    private PKSubtitlePreference subtitlePreference = PKSubtitlePreference.INTERNAL;
    private Integer maxVideoBitrate;
    private Integer maxAudioBitrate;
    private int maxAudioChannelCount = -1;

    private LoadControlBuffers loadControlBuffers = new LoadControlBuffers();
    private SubtitleStyleSettings subtitleStyleSettings;
    private PKAspectRatioResizeMode resizeMode = PKAspectRatioResizeMode.fit;
    private ABRSettings abrSettings = new ABRSettings();
    private VRSettings vrSettings;
    /**
     * Flag helping to check if client app wants to use a single player instance at a time
     * Only if IMA plugin is there then only this flag is set to true.
     */
    private boolean forceSinglePlayerEngine = false;

    private PKTrackConfig preferredTextTrackConfig;
    private PKTrackConfig preferredAudioTrackConfig;
    private PKMediaFormat preferredMediaFormat = PKMediaFormat.dash;
    private PKRequestParams.Adapter contentRequestAdapter;
    private PKRequestParams.Adapter licenseRequestAdapter;
    private Object customLoadControlStrategy = null;
    private PKMaxVideoSize maxVideoSize;


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

    public boolean enableDecoderFallback() {
        return enableDecoderFallback;
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
        if (abrSettings.getMinVideoBitrate() > abrSettings.getMaxVideoBitrate()) {
            abrSettings.setMinVideoBitrate(Long.MIN_VALUE);
            abrSettings.setMaxVideoBitrate(Long.MAX_VALUE);
        }
        return abrSettings;
    }

    public PKAspectRatioResizeMode getAspectRatioResizeMode(){
        return resizeMode;
    }

    public VRSettings getVRSettings() {
        return vrSettings;
    }

    public boolean isForceSinglePlayerEngine() {
        return forceSinglePlayerEngine;
    }

    public VideoCodecSettings getPreferredVideoCodecSettings() {
        return preferredVideoCodecSettings;
    }

    public AudioCodecSettings getPreferredAudioCodecSettings() {
        return preferredAudioCodecSettings;
    }

    public Object getCustomLoadControlStrategy() {
        return customLoadControlStrategy;
    }

    public boolean isTunneledAudioPlayback() {
        return isTunneledAudioPlayback;
    }

    public boolean isHandleAudioBecomingNoisyEnabled() {
        return handleAudioBecomingNoisyEnabled;
    }

    public PKWakeMode getWakeMode() {
        return wakeMode;
    }

    public boolean isHandleAudioFocus() {
        return handleAudioFocus;
    }

    public PKSubtitlePreference getSubtitlePreference() {
        return subtitlePreference;
    }

    public PKMaxVideoSize getMaxVideoSize() { return maxVideoSize; }

    public Integer getMaxVideoBitrate() {
        return maxVideoBitrate;
    }

    public Integer getMaxAudioBitrate() {
        return maxAudioBitrate;
    }

    public int getMaxAudioChannelCount() {
        return maxAudioChannelCount;
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
    public Player.Settings enableDecoderFallback(boolean enableDecoderFallback) {
        this.enableDecoderFallback = enableDecoderFallback;
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
    public Player.Settings forceSinglePlayerEngine(boolean forceSinglePlayerEngine) {
        this.forceSinglePlayerEngine = forceSinglePlayerEngine;
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

    @Override
    public Player.Settings setPreferredVideoCodecSettings(VideoCodecSettings preferredVideoCodecSettings) {
        if (preferredVideoCodecSettings == null) {
            this.preferredVideoCodecSettings = new VideoCodecSettings().setAllowMixedCodecAdaptiveness(true);
        } else {
            this.preferredVideoCodecSettings = preferredVideoCodecSettings;
        }
        return this;
    }

    @Override
    public Player.Settings setPreferredAudioCodecSettings(AudioCodecSettings preferredAudioCodecSettings) {
        if (preferredAudioCodecSettings == null) {
            this.preferredAudioCodecSettings = new AudioCodecSettings().setAllowMixedCodecs(true);
        } else {
            this.preferredAudioCodecSettings = preferredAudioCodecSettings;
        }
        return this;
    }

    public Player.Settings setCustomLoadControlStrategy(Object customLoadControlStrategy) {
        this.customLoadControlStrategy = customLoadControlStrategy;
        return this;
    }

    @Override
    public Player.Settings setTunneledAudioPlayback(boolean isTunneledAudioPlayback) {
        this.isTunneledAudioPlayback = isTunneledAudioPlayback;
        return this;
    }

    @Override
    public Player.Settings setHandleAudioBecomingNoisy(boolean handleAudioBecomingNoisyEnabled) {
        this.handleAudioBecomingNoisyEnabled = handleAudioBecomingNoisyEnabled;
        return this;
    }

    @Override
    public Player.Settings setWakeMode(PKWakeMode wakeMode) {
        if (wakeMode != null) {
            this.wakeMode = wakeMode;
        }
        return this;
    }

    @Override
    public Player.Settings setHandleAudioFocus(boolean handleAudioFocus) {
        this.handleAudioFocus = handleAudioFocus;
        return this;
    }

    @Override
    public Player.Settings setSubtitlePreference(PKSubtitlePreference subtitlePreference) {
        if (subtitlePreference == null) {
            this.subtitlePreference = PKSubtitlePreference.OFF;
        } else {
            this.subtitlePreference = subtitlePreference;
        }
        return this;
    }

    @Override
    public Player.Settings setMaxVideoSize(PKMaxVideoSize maxVideoSize) {
        this.maxVideoSize = maxVideoSize;
        return this;
    }

    @Override
    public Player.Settings setMaxVideoBitrate(Integer maxVideoBitrate) {
        this.maxVideoBitrate = maxVideoBitrate;
        return this;
    }

    @Override
    public Player.Settings setMaxAudioBitrate(Integer maxAudioBitrate) {
        this.maxAudioBitrate = maxAudioBitrate;
        return this;
    }

    @Override
    public Player.Settings setMaxAudioChannelCount(int maxAudioChannelCount) {
        this.maxAudioChannelCount = maxAudioChannelCount;
        return this;
    }
}

