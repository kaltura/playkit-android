package com.kaltura.playkit.player;

import android.support.annotation.Nullable;

import com.kaltura.playkit.LocalAssetsManager;
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
    PKMediaSource getPreferredSource() {

        // If PKMediaSource is local, there is no need to look for the preferred source,
        // because it is only one.
        PKMediaSource localMediaSource = getLocalSource();
        if(localMediaSource != null){
            return localMediaSource;
        }

        // Default preference: DASH, HLS, WVM, MP4

        List<PKMediaFormat> pref = new ArrayList<>(10);

        // Dash is always available.
        pref.add(PKMediaFormat.dash_clear);
        
        // Dash+Widevine is only available from Android 4.3 
        if (MediaSupport.widevineModular()) {
            pref.add(PKMediaFormat.dash_widevine);  
        }
        
        // HLS clear is always available
        pref.add(PKMediaFormat.hls_clear);
        
        // Widevine Classic is OPTIONAL from Android 6. 
        if (MediaSupport.widevineClassic(null)) {
            pref.add(PKMediaFormat.wvm_widevine);
        }
        
        // MP4 is always available, but gives inferior performance.
        pref.add(PKMediaFormat.mp4_clear);
        
        for (PKMediaFormat format : pref) {
            PKMediaSource source = sourceByFormat(format);
            if (source != null) {
                return source;
            }
        }

        log.e("No playable sources found!");
        return null;
    }

    static PKMediaSource selectSource(PKMediaEntry mediaEntry) {
        return new SourceSelector(mediaEntry).getPreferredSource();
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

