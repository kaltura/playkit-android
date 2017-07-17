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

package com.kaltura.playkit;

import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

public enum PKMediaFormat {
    dash("application/dash+xml", "mpd"),
    hls("application/x-mpegURL", "m3u8"),
    wvm("video/wvm", "wvm"),
    mp4("video/mp4", "mp4"),
    mp3("audio/mpeg", "mp3"),
    unknown(null, null);

    public final String mimeType;
    public final String pathExt;
    
    private static Map<String, PKMediaFormat> extensionLookup = new HashMap<>(); 
    
    static {
        for (PKMediaFormat format : values()) {
            if (extensionLookup.get(format.pathExt) == null) {
                extensionLookup.put(format.pathExt, format);
            }
        }
    }

    PKMediaFormat(String mimeType, String pathExt) {
        this.mimeType = mimeType;
        this.pathExt = pathExt;
    }

    public static PKMediaFormat valueOfExt(String ext) {
        return extensionLookup.get(ext);
    }

    public static PKMediaFormat valueOfUrl(String sourceURL) {
        PKMediaFormat mediaFormat = null;
        if (sourceURL != null) {
            String path = Uri.parse(sourceURL).getPath();
            int extIndex = path.lastIndexOf('.');
            if (extIndex < 0) {
                return null;
            }
            mediaFormat = PKMediaFormat.valueOfExt(path.substring(extIndex + 1));
        }
        return mediaFormat;
    }
}
