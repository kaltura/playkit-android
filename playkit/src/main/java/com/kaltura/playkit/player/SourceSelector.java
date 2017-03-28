package com.kaltura.playkit.player;

import android.support.annotation.Nullable;

import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;

import java.util.List;

import static com.kaltura.playkit.PlayKitManager.CLIENT_TAG;

/**
 * Created by Noam Tamim @ Kaltura on 29/11/2016.
 */

class SourceSelector {
    
    private static final PKLog log = PKLog.get("SourceSelector");
    private final PKMediaEntry mediaEntry;
    
    SourceSelector(PKMediaEntry mediaEntry) {
        this.mediaEntry = mediaEntry;
    }
    
    @Nullable
    private PKMediaSource sourceByFormat(PKMediaFormat format) {
        for (PKMediaSource source : mediaEntry.getSources()) {
            if (source.getMediaFormat() == format) {
                return source;
            }
        }
        return null;
    }
    
    @Nullable
    PKMediaSource getPreferredSource(String sessionId) {

        // If PKMediaSource is local, there is no need to look for the preferred source,
        // because it is only one.
        PKMediaSource localMediaSource = getLocalSource();
        if(localMediaSource != null){
            return localMediaSource;
        }

        // Default preference: DASH, HLS, WVM, MP4, MP3

        PKMediaFormat[] pref = {PKMediaFormat.dash, PKMediaFormat.hls, PKMediaFormat.wvm, PKMediaFormat.mp4, PKMediaFormat.mp3};
        
        for (PKMediaFormat format : pref) {
            PKMediaSource source = sourceByFormat(format);
            if (source == null) {
                continue;
            }

            List<PKDrmParams> drmParams = source.getDrmData();
            if (drmParams != null && !drmParams.isEmpty()) {
                for (PKDrmParams params : drmParams) {
                    if (params.isSchemeSupported()) {
                        source.setUrl(fixPlayManifest(source.getUrl(), sessionId));
                        return source;
                    }
                }
                // This source doesn't have supported params
                continue;
            }
            source.setUrl(fixPlayManifest(source.getUrl(), sessionId));
            return source;
        }

        log.e("No playable sources found!");
        return null;
    }

    private String fixPlayManifest(String origURL, String sessionId) {
        String fixedURL = origURL;
        if (origURL.contains("/playManifest/")) {
            if (origURL.contains("?")) {
                fixedURL += "&playSessionId=" + sessionId;
            } else {
                fixedURL += "?playSessionId=" + sessionId;
            }
            fixedURL += "&clientTag=" + CLIENT_TAG;
            return fixedURL;
        }
        return origURL;
    }

    static PKMediaSource selectSource(PKMediaEntry mediaEntry, String sessionId) {
        return new SourceSelector(mediaEntry).getPreferredSource(sessionId);
    }

    private PKMediaSource getLocalSource(){
        for (PKMediaSource source : mediaEntry.getSources()) {
            if (source instanceof LocalAssetsManager.LocalMediaSource) {
                return source;
            }
        }
        return null;
    }
}

