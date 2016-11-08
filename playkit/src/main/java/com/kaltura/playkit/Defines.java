package com.kaltura.playkit;

/**
 * Created by tehilarozin on 27/10/2016.
 */

public class Defines {
    public static String getMimeType(String url) {
        String extension = url.substring(url.lastIndexOf("."));
        switch (extension){
            case ".mpd": return "application/dash+xml";
            case ".mp4": return "video/mp4";
            case ".wvm": return "video/wvm";
            case ".m3u8": return "application/vnd.apple.mpegurl";
        }
        return "";
    }
}
