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
import android.support.annotation.Nullable;

import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.player.vr.VRSettings;

/**
 * Created by Noam Tamim @ Kaltura on 29/03/2017.
 */
public class PKMediaSourceConfig {

    PKMediaSource mediaSource;
    PKMediaEntry.MediaEntryType mediaEntryType;
    LiveStreamMode dvrStatus;
    PlayerSettings playerSettings;
    private VRSettings vrSettings;

    public enum LiveStreamMode {
        LIVE(0),
        LIVE_DVR(1);

        private final int value;

        LiveStreamMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    PKMediaSourceConfig(PKMediaConfig mediaConfig, PKMediaSource source, PlayerSettings playerSettings, VRSettings vrSettings) {
        this.mediaSource = source;
        this.mediaEntryType = mediaConfig.getMediaEntry().getMediaType();
        if (mediaConfig.getMediaEntry().getMetadata() != null &&
                mediaConfig.getMediaEntry().getMetadata().containsKey("dvrStatus")) {
            if ("0".equals(mediaConfig.getMediaEntry().getMetadata().get("dvrStatus"))) {
                this.dvrStatus = LiveStreamMode.LIVE;
            } else {
                this.dvrStatus = LiveStreamMode.LIVE_DVR;
            }
        }
        this.playerSettings = playerSettings;
        this.vrSettings = vrSettings;
    }

    PKMediaSourceConfig(PKMediaConfig mediaConfig, PKMediaSource source, PlayerSettings playerSettings) {
        this(mediaConfig, source, playerSettings, null);
    }

    Uri getUrl() {
        Uri uri = Uri.parse(mediaSource.getUrl());
        if (playerSettings.getContentRequestAdapter() == null) {
            return uri;
        } else {
            return playerSettings.getContentRequestAdapter().adapt(new PKRequestParams(uri, null)).url;
        }
    }

    @Nullable
    public VRSettings getVrSettings() {
        return this.vrSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

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