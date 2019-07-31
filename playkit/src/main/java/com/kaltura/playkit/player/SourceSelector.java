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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Noam Tamim @ Kaltura on 29/11/2016.
 */

public class SourceSelector {

    private static final PKLog log = PKLog.get("SourceSelector");
    private final PKMediaEntry mediaEntry;
    private final PKMediaFormat preferredMediaFormat;

    private PKMediaSource selectedSource;
    private PKDrmParams selectedDrmParams;

    public SourceSelector(PKMediaEntry mediaEntry, PKMediaFormat preferredMediaFormat) {
        this.mediaEntry = mediaEntry;
        this.preferredMediaFormat = preferredMediaFormat;

        selectSource();
    }

    @Nullable
    private PKMediaSource sourceByFormat(PKMediaFormat format) {
        if (mediaEntry != null && mediaEntry.getSources() != null) {
            for (PKMediaSource source : mediaEntry.getSources()) {
                if (source.getMediaFormat() == format) {
                    return source;
                }
            }
        }
        return null;
    }


    public PKMediaSource getSelectedSource() {
        return selectedSource;
    }

    public PKDrmParams getSelectedDrmParams() {
        return selectedDrmParams;
    }

    private void selectSource() {

        selectedSource = null;
        selectedDrmParams = null;

        // If PKMediaSource is local, there is no need to look for the preferred source,
        // because it is only one.
        PKMediaSource localMediaSource = getLocalSource();
        if (localMediaSource != null) {
            selectedSource = localMediaSource;
            return;
        }

        List<PKMediaFormat> formatsPriorityList = getFormatsPriorityList();

        for (PKMediaFormat format : formatsPriorityList) {
            PKMediaSource source = sourceByFormat(format);
            if (source == null) {
                continue;
            }

            if (source.hasDrmParams()) {
                List<PKDrmParams> drmParams = source.getDrmData();
                for (PKDrmParams params : drmParams) {
                    if (params.isSchemeSupported()) {
                        selectedSource = source;
                        selectedDrmParams = params;
                        return;
                    }
                }
                // This source doesn't have supported params

            } else {
                selectedSource = source;
                selectedDrmParams = null;   // clear
                return;
            }
        }
    }

    @NonNull
    private List<PKMediaFormat> getFormatsPriorityList() {
        List<PKMediaFormat> formatsPriorityList = new ArrayList<>();

        formatsPriorityList.add(PKMediaFormat.dash);
        formatsPriorityList.add(PKMediaFormat.hls);
        formatsPriorityList.add(PKMediaFormat.wvm);
        formatsPriorityList.add(PKMediaFormat.mp4);
        formatsPriorityList.add(PKMediaFormat.mp3);

        if (preferredMediaFormat == PKMediaFormat.dash) {
            return formatsPriorityList;
        }

        int preferredMediaFormatIndex = formatsPriorityList.indexOf(preferredMediaFormat);
        if (preferredMediaFormatIndex > 0) {
            formatsPriorityList.remove(preferredMediaFormatIndex);
            formatsPriorityList.add(0, preferredMediaFormat);
        }
        return formatsPriorityList;
    }

    public static PKMediaSource selectSource(PKMediaEntry mediaEntry, PKMediaFormat preferredMediaFormat) {
        final SourceSelector sourceSelector = new SourceSelector(mediaEntry, preferredMediaFormat);
        return sourceSelector.selectedSource;
    }

    private PKMediaSource getLocalSource() {
        if (mediaEntry != null && mediaEntry.getSources() != null) {
            for (PKMediaSource source : mediaEntry.getSources()) {

                if (source instanceof LocalAssetsManager.LocalMediaSource) {
                    return source;
                }
            }
        }
        return null;
    }
}

