/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.playback;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.player.PlayerSettings;

/**
 * Created by Noam Tamim @ Kaltura on 28/03/2017.
 */
public class KalturaPlaybackRequestAdapter implements PKRequestParams.Adapter {

    private final String applicationName;
    private String playSessionId;

    public static void install(Player player, String applicationName) {
        if (player.getSettings() instanceof PlayerSettings &&
                ((PlayerSettings)player.getSettings()).getContentRequestAdapter() != null &&
                TextUtils.isEmpty(applicationName)) {
            applicationName = ((PlayerSettings)player.getSettings()).getContentRequestAdapter().getApplicationName();
        }

        KalturaPlaybackRequestAdapter decorator = new KalturaPlaybackRequestAdapter(applicationName, player);
        player.getSettings().setContentRequestAdapter(decorator);
    }

    private KalturaPlaybackRequestAdapter(String applicationName, Player player) {
        this.applicationName = applicationName;
        updateParams(player);
    }

    @NonNull
    @Override
    public PKRequestParams adapt(PKRequestParams requestParams) {
        return PlaybackUtils.getPKRequestParams(requestParams, playSessionId, applicationName, null);
    }

    @Override
    public void updateParams(Player player) {
        this.playSessionId = player.getSessionId();
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }
}
