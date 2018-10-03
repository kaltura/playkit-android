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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

class SourceSelector {

    private static final PKLog log = PKLog.get("SourceSelector");
    private final PKMediaEntry mediaEntry;
    private final PKMediaFormat preferredMediaFormat;

    public SourceSelector(PKMediaEntry mediaEntry, PKMediaFormat preferredMdieaFormat) {
        this.mediaEntry = mediaEntry;
        this.preferredMediaFormat = preferredMdieaFormat;
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

    @Nullable
    PKMediaSource getPreferredSource() {

        // If PKMediaSource is local, there is no need to look for the preferred source,
        // because it is only one.
        PKMediaSource localMediaSource = getLocalSource();
        if (localMediaSource != null) {
            return localMediaSource;
        }

        // Default preference: DASH, HLS, WVM, MP4, MP3

        List<PKMediaFormat> formatsPriorityList = getFormatsPriorityList();

        for (PKMediaFormat format : formatsPriorityList) {
            PKMediaSource source = sourceByFormat(format);
            if (source == null) {
                continue;
            }

            List<PKDrmParams> drmParams = source.getDrmData();
            if (drmParams != null && !drmParams.isEmpty()) {
                for (PKDrmParams params : drmParams) {
                    if (params.isSchemeSupported()) {
                        return source;
                    }
                }
                // This source doesn't have supported params
                continue;
            }
            return source;
        }
        return null;
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
        return new SourceSelector(mediaEntry, preferredMediaFormat).getPreferredSource();
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

