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
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Noam Tamim @ Kaltura on 29/11/2016.
 */

public class SourceSelector {
    
    private static final PKLog log = PKLog.get("SourceSelector");
    private final PKMediaConfig mediaConfig;

    public SourceSelector(PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;
    }
    
    @Nullable
    private PKMediaSource sourceByFormat(PKMediaFormat format) {
        if (mediaConfig != null && mediaConfig.getMediaEntry()!= null && mediaConfig.getMediaEntry().getSources() != null) {
            for (PKMediaSource source : mediaConfig.getMediaEntry().getSources()) {
                if (source.getMediaFormat() == format) {
                    return source;
                }
            }
        }
        return null;
    }
    
    @Nullable
    public PKMediaSource getPreferredSource() {

        // If PKMediaSource is local, there is no need to look for the preferred source,
        // because it is only one.
        PKMediaSource localMediaSource = getLocalSource();
        if(localMediaSource != null){
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

        int firstIndex = formatsPriorityList.indexOf(PKMediaFormat.dash);
        int secondIndex = formatsPriorityList.indexOf(mediaConfig.getPreferredMediaFormat());
        if (secondIndex > 0) {
            Collections.swap(formatsPriorityList, firstIndex, secondIndex);
        } else if (mediaConfig.getMediaEntry().getMetadata().containsKey("pref")) {
            secondIndex = formatsPriorityList.indexOf(PKMediaFormat.valueOf(mediaConfig.getMediaEntry().getMetadata().get("pref")));
            if (secondIndex > 0) {
                Collections.swap(formatsPriorityList, firstIndex, secondIndex);
            }
        }

        return formatsPriorityList;
    }

    public static PKMediaSource selectSource(PKMediaConfig mediaConfig) {
        return new SourceSelector(mediaConfig).getPreferredSource();
    }

    private PKMediaSource getLocalSource(){
        if (mediaConfig != null && mediaConfig.getMediaEntry()!= null && mediaConfig.getMediaEntry().getSources() != null) {
            for (PKMediaSource source : mediaConfig.getMediaEntry().getSources()) {
                if (source instanceof LocalAssetsManager.LocalMediaSource) {
                    return source;
                }
            }
        }
        return null;
    }
}

