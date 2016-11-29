package com.kaltura.playkit.player;

import android.content.Context;
import android.drm.DrmManagerClient;
import android.media.MediaDrm;
import android.os.Build;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;

/**
 * Created by Noam Tamim @ Kaltura on 29/11/2016.
 */

class SourceSelector {
    static PKMediaSource selectSource(PKMediaEntry mediaEntry) {
        // TODO: really implement this.
        return mediaEntry.getSources().get(0);
    }
}

class MediaSupport {

    private static MediaSupport sharedInstance;
    
    final boolean WIDEVINE_MODULAR;
    final boolean WIDEVINE_CLASSIC;

    static MediaSupport getInstance(Context context) {
        if (sharedInstance == null) {
            synchronized (MediaSupport.class) {
                if (sharedInstance == null) {
                    sharedInstance = new MediaSupport(context);
                }
            }
        }
        return sharedInstance;
    }
    
    private static final PKLog log = PKLog.get("MediaSupport");

    private MediaSupport(Context context) {
        WIDEVINE_CLASSIC = widevineClassic(context);
        WIDEVINE_MODULAR = widevineModular();
    }
    
    
    private static boolean widevineClassic(Context context) {
        DrmManagerClient drmManagerClient = new DrmManagerClient(context);
        boolean canHandle = false;
        // adding try catch due some android devices have different canHandle method implementation regarding the arguments validation inside it
        try {
            canHandle = drmManagerClient.canHandle("", "video/wvm");
        } catch (IllegalArgumentException ex) {
            log.e("drmManagerClient.canHandle failed");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                log.i("Assuming WV Classic is supported although canHandle has failed");
                canHandle = true;
            }
        } finally {
            //noinspection deprecation
            drmManagerClient.release();
        }
        return canHandle;
    }
    
    private static boolean widevineModular() {
        // Encrypted dash is only supported in Android v4.3 and up -- needs MediaDrm class.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // Make sure Widevine is supported.
            if (MediaDrm.isCryptoSchemeSupported(ExoPlayerWrapper.WIDEVINE_UUID)) {
                return true;
            }
        }
        return false;
    }
}
