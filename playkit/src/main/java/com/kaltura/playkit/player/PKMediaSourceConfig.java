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

package com.kaltura.playkit.player;

import android.net.Uri;

import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKRequestParams;

/**
 * Created by Noam Tamim @ Kaltura on 29/03/2017.
 */
class PKMediaSourceConfig {

    PKMediaSource mediaSource;
    PKMediaEntry.MediaEntryType mediaEntryType;
    PlayerSettings playerSettings;

    PKMediaSourceConfig(PKMediaConfig mediaConfig, PKMediaSource source, PlayerSettings playerSettings) {
        this.mediaSource = source;
        this.mediaEntryType = mediaConfig.getMediaEntry().getMediaType();
        this.playerSettings = playerSettings;
    }

    Uri getUrl() {
        Uri uri = Uri.parse(mediaSource.getUrl());
        if (playerSettings.getContentRequestAdapter() == null) {
            return uri;
        } else {
            return playerSettings.getContentRequestAdapter().adapt(new PKRequestParams(uri, null)).url;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PKMediaSourceConfig that = (PKMediaSourceConfig) o;

        if (mediaSource != null ? !mediaSource.equals(that.mediaSource) : that.mediaSource != null) {
            return false;
        }
        return playerSettings.getContentRequestAdapter() != null ? playerSettings.getContentRequestAdapter().equals(that.playerSettings.getContentRequestAdapter()) : that.playerSettings.getContentRequestAdapter() == null;
    }

    @Override
    public int hashCode() {
        int result = mediaSource != null ? mediaSource.hashCode() : 0;
        result = 31 * result + (playerSettings.getContentRequestAdapter() != null ? playerSettings.getContentRequestAdapter().hashCode() : 0);
        return result;
    }
}
