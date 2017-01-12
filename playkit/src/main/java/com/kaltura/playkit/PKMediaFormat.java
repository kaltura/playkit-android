package com.kaltura.playkit;

import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

public enum PKMediaFormat {
    mp4_clear("video/mp4", "mp4"),
    dash_clear("application/dash+xml", "mpd"),
    dash_widevine("application/dash+xml", "mpd"),
    wvm_widevine("video/wvm", "wvm"),
    hls_clear("application/x-mpegURL", "m3u8");

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
