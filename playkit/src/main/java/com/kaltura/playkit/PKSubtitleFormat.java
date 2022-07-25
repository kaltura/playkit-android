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

import com.kaltura.android.exoplayer2.util.MimeTypes;

import java.util.HashMap;
import java.util.Map;

public enum PKSubtitleFormat {
    vtt(MimeTypes.TEXT_VTT, "vtt"),
    srt(MimeTypes.APPLICATION_SUBRIP, "srt"),
    ttml(MimeTypes.APPLICATION_TTML, "ttml");

    public final String mimeType;
    public final String pathExt;

    private static Map<String, PKSubtitleFormat> extensionLookup = new HashMap<>();

    static {
        for (PKSubtitleFormat format : values()) {
            if (extensionLookup.get(format.pathExt) == null) {
                extensionLookup.put(format.pathExt, format);
            }
        }
    }

    PKSubtitleFormat(String mimeType, String pathExt) {
        this.mimeType = mimeType;
        this.pathExt = pathExt;
    }

    public static PKSubtitleFormat valueOfExt(String ext) {
        return extensionLookup.get(ext);
    }

    public static PKSubtitleFormat valueOfUrl(String sourceURL) {
        PKSubtitleFormat subtitleFormat = null;
        if (sourceURL != null) {
            String path = Uri.parse(sourceURL).getPath();
            if (path != null) {
                int extIndex = path.lastIndexOf('.');
                if (extIndex < 0) {
                    return null;
                }
                subtitleFormat = PKSubtitleFormat.valueOfExt(path.substring(extIndex + 1));
            }
        }
        return subtitleFormat;
    }
}
