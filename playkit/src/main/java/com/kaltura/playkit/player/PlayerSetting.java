package com.kaltura.playkit.player;

import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.Player;

public class PlayerSetting implements Player.Settings {

    private PKRequestParams.Adapter contentRequestAdapter;
    private boolean useTextureView = false;
    private boolean crossProtocolRedirectEnabled = false;
    private boolean cea608CaptionsEnabled = false;
    private PKTrackConfig preferredTextTrackConfig;
    private PKTrackConfig preferredAudioTrackConfig;

    public PKRequestParams.Adapter getContentRequestAdapter() {
        return contentRequestAdapter;
    }

    public boolean isUseTextureView() {
        return useTextureView;
    }

    public boolean isCrossProtocolRedirectEnabled() {
        return crossProtocolRedirectEnabled;
    }

    public boolean isCea608CaptionsEnabled() {
        return cea608CaptionsEnabled;
    }

    public PKTrackConfig getPreferredTextTrackConfig() {
        return preferredTextTrackConfig;
    }

    public PKTrackConfig getPreferredAudioTrackConfig() {
        return preferredAudioTrackConfig;
    }

    @Override
    public PlayerSetting setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter) {
        this.contentRequestAdapter = contentRequestAdapter;
        return this;
    }

    @Override
    public PlayerSetting setCea608CaptionsEnabled(boolean cea608CaptionsEnabled) {
         this.cea608CaptionsEnabled = cea608CaptionsEnabled;
         return this;
    }

    @Override
    public PlayerSetting useTextureView(boolean useTextureView) {
        this.useTextureView = useTextureView;
        return this;
    }

    @Override
    public PlayerSetting setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig) {
        this.preferredAudioTrackConfig = preferredAudioTrackConfig;
        return this;
    }

    @Override
    public PlayerSetting setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig) {
        this.preferredTextTrackConfig = preferredTextTrackConfig;
        return this;
    }

    @Override
    public PlayerSetting setAllowCrossProtocolRedirect(boolean crossProtocolRedirectEnabled) {
        this.crossProtocolRedirectEnabled = crossProtocolRedirectEnabled;
        return this;
    }
}