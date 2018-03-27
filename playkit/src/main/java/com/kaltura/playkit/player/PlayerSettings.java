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

import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.Player;


public class PlayerSettings implements Player.Settings {

    private PKRequestParams.Adapter contentRequestAdapter;
    private boolean useTextureView = false;
    private boolean crossProtocolRedirectEnabled = false;
    private boolean cea608CaptionsEnabled = false;
    //private PKTrackConfig preferredTextTrackConfig;
    //private PKTrackConfig preferredAudioTrackConfig;

    public PKRequestParams.Adapter getContentRequestAdapter() {
        return contentRequestAdapter;
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

    @Override
    public PlayerSettings setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter) {
        this.contentRequestAdapter = contentRequestAdapter;
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

    //public PKTrackConfig getPreferredTextTrackConfig() {
    //    return preferredTextTrackConfig;
    //}

    //public PKTrackConfig getPreferredAudioTrackConfig() {
    //    return preferredAudioTrackConfig;
    //}

    //    @Override
    //    public PlayerSettings setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig) {
    //        this.preferredAudioTrackConfig = preferredAudioTrackConfig;
    //        return this;
    //    }
    //
    //    @Override
    //    public PlayerSettings setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig) {
    //        this.preferredTextTrackConfig = preferredTextTrackConfig;
    //        return this;
    //    }

    @Override
    public PlayerSettings setAllowCrossProtocolRedirect(boolean crossProtocolRedirectEnabled) {
        this.crossProtocolRedirectEnabled = crossProtocolRedirectEnabled;
        return this;
    }
}