package com.kaltura.playkit;

public enum PKMediaFormat {
    mp4_clear("mp4", "video/mp4", ".mp4", null),
    dash_clear("dash", "application/dash+xml", ".mpd", null),
    dash_widevine("dash", "application/dash+xml", ".mpd", "widevine"),
    wvm_widevine("wvm", "video/wvm", ".wvm", "widevine"),
    hls_clear("hls", "application/x-mpegURL", ".m3u8", null),
    hls_fairplay("hls", "application/vnd.apple.mpegurl", ".m3u8", "fairplay");

    public final String shortName;
    public final String mimeType;
    public final String pathExt;
    public final String drm;

    PKMediaFormat(String shortName, String mimeType, String pathExt, String drm) {
        this.shortName = shortName;
        this.mimeType = mimeType;
        this.pathExt = pathExt;
        this.drm = drm;
    }

    public static PKMediaFormat valueOfExt(String ext) {
        for(PKMediaFormat mediaFormat : values()){
            if(mediaFormat.pathExt.equals(ext)){
                return mediaFormat;
            }
        }
        return null;
    }

    public static PKMediaFormat getMediaFormat(String sourceURL) {
        if(sourceURL != null) {
            String ext = sourceURL.substring(sourceURL.lastIndexOf("."));
            return PKMediaFormat.valueOfExt(ext);
        }
        return null;
    }
}