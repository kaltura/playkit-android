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

import com.kaltura.playkit.*;

import java.util.*;

/**
 * Created by Noam Tamim @ Kaltura on 29/11/2016.
 */

public class SourceSelector {

    private static final PKLog log = PKLog.get("SourceSelector");
    private final PKMediaEntry mediaEntry;
    private final PKMediaFormat preferredMediaFormat;

    private String preferredSourceId;

    private PKMediaSource selectedSource;
    private PKDrmParams selectedDrmParams;

    private static final List<PKMediaFormat> defaultFormatPriority = Collections.unmodifiableList(
            Arrays.asList(PKMediaFormat.dash, PKMediaFormat.hls, PKMediaFormat.wvm, PKMediaFormat.mp4, PKMediaFormat.mp3));

    public SourceSelector(PKMediaEntry mediaEntry, PKMediaFormat preferredMediaFormat) {
        this.mediaEntry = mediaEntry;
        this.preferredMediaFormat = preferredMediaFormat;
    }

    public void setPreferredSourceId(String preferredSourceId) {
        this.preferredSourceId = preferredSourceId;
        this.selectedSource = null;
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
        selectSource();
        return selectedSource;
    }

    public PKDrmParams getSelectedDrmParams() {
        selectSource();
        return selectedDrmParams;
    }

    private void selectSource() {

        if (selectedSource != null) {
            return;
        }

        // If PKMediaSource is local, there is no need to look for the preferred source,
        // because it is only one.
        PKMediaSource localMediaSource = findLocalSource();
        if (localMediaSource != null) {
            selectedSource = localMediaSource;
            return;
        }


        // If preferredSourceId is set, first to to select that source
        if (preferredSourceId != null) {
            for (PKMediaSource source : mediaEntry.getSources()) {
                if (preferredSourceId.equals(source.getId())) {
                    if (selectIfSupported(source)) {
                        return;
                    }
                }
            }
        }

        // Otherwise, select the first playable source given the priority
        List<PKMediaFormat> formatsPriorityList = getFormatsPriorityList();

        for (PKMediaFormat format : formatsPriorityList) {
            PKMediaSource source = sourceByFormat(format);
            if (source == null) {
                continue;
            }

            if (selectIfSupported(source)) {
                return;
            }
        }
    }

    private boolean selectIfSupported(PKMediaSource source) {
        if (source.hasDrmParams()) {
            List<PKDrmParams> drmParams = source.getDrmData();
            for (PKDrmParams params : drmParams) {
                if (params.isSchemeSupported()) {
                    selectedSource = source;
                    selectedDrmParams = params;
                    return true;
                }
            }
            // This source doesn't have supported params

        } else {
            selectedSource = source;
            selectedDrmParams = null;   // clear
            return true;
        }
        return false;
    }

    @NonNull
    private List<PKMediaFormat> getFormatsPriorityList() {

        if (preferredMediaFormat == null || preferredMediaFormat == defaultFormatPriority.get(0)) {
            return defaultFormatPriority;
        }

        List<PKMediaFormat> formatsPriorityList = new ArrayList<>();

        formatsPriorityList.add(preferredMediaFormat);
        for (PKMediaFormat format : defaultFormatPriority) {
            if (format != preferredMediaFormat) {
                formatsPriorityList.add(format);
            }
        }

        return formatsPriorityList;
    }

    static PKMediaSource selectSource(PKMediaEntry mediaEntry, PKMediaFormat preferredMediaFormat) {
        final SourceSelector sourceSelector = new SourceSelector(mediaEntry, preferredMediaFormat);
        return sourceSelector.getSelectedSource();
    }

    private PKMediaSource findLocalSource() {
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

