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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.player.vr.VRSettings;

import java.util.List;

/**
 * Created by Noam Tamim @ Kaltura on 29/03/2017.
 */
public class PKMediaSourceConfig {

    PKMediaSource mediaSource;
    PKMediaEntry.MediaEntryType mediaEntryType;
    PlayerSettings playerSettings;
    private VRSettings vrSettings;
    private List<PKExternalSubtitle> externalSubtitlesList;
    private String externalVttThumbnailUrl;

    public PKMediaSourceConfig(PKMediaSource source, PKMediaEntry.MediaEntryType mediaEntryType, List<PKExternalSubtitle> externalSubtitlesList, String externalVttThumbnailUrl, PlayerSettings playerSettings, VRSettings vrSettings) {
        this.mediaSource = source;
        this.mediaEntryType = (mediaEntryType != null) ? mediaEntryType : PKMediaEntry.MediaEntryType.Unknown;
        this.playerSettings = playerSettings;
        this.vrSettings = vrSettings;
        this.externalSubtitlesList = externalSubtitlesList;
        this.externalVttThumbnailUrl = externalVttThumbnailUrl;
    }

    public PKMediaSourceConfig(PKMediaConfig mediaConfig, PKMediaSource source, PlayerSettings playerSettings) {
        this.mediaSource = source;
        this.playerSettings = playerSettings;

        if (mediaConfig != null && mediaConfig.getMediaEntry() != null) {
            PKMediaEntry mediaConfigEntry = mediaConfig.getMediaEntry();
            this.mediaEntryType = (mediaConfigEntry.getMediaType() != null) ? mediaConfigEntry.getMediaType() : PKMediaEntry.MediaEntryType.Unknown;

            if (mediaConfigEntry.isVRMediaType()) {
                this.vrSettings = (playerSettings.getVRSettings() != null) ? playerSettings.getVRSettings() :new VRSettings();
            }

            this.externalSubtitlesList = (mediaConfigEntry.getExternalSubtitleList() != null) ? mediaConfigEntry.getExternalSubtitleList() : null;

            this.externalVttThumbnailUrl = (!TextUtils.isEmpty(mediaConfigEntry.getExternalVttThumbnailUrl())) ? mediaConfigEntry.getExternalVttThumbnailUrl() : null;
        }
    }

    public PKMediaSourceConfig(PKMediaSource source, PKMediaEntry.MediaEntryType mediaEntryType, List<PKExternalSubtitle> externalSubtitlesList, String externalVttThumbnailUrl, PlayerSettings playerSettings) {
        this(source, mediaEntryType, externalSubtitlesList, externalVttThumbnailUrl, playerSettings, null);
    }

    public PKRequestParams getRequestParams() {
        Uri uri = Uri.parse(mediaSource.getUrl());
        if (playerSettings.getContentRequestAdapter() == null) {
            return new PKRequestParams(uri, null);
        } else {
            return playerSettings.getContentRequestAdapter().adapt(new PKRequestParams(uri, null));
        }
    }

    @Nullable
    public VRSettings getVrSettings() {
        return this.vrSettings;
    }

    public List<PKExternalSubtitle> getExternalSubtitleList() {
        return externalSubtitlesList;
    }

    public String getExternalVttThumbnailUrl() {
        return externalVttThumbnailUrl;
    }

    @NonNull
    public PKMediaEntry.MediaEntryType getMediaEntryType() {
        return mediaEntryType;
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
