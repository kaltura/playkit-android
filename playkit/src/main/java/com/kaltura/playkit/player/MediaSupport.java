package com.kaltura.playkit.player;

import android.content.Context;
import android.drm.DrmManagerClient;
import android.media.MediaDrm;
import android.os.Build;
import android.support.annotation.NonNull;

import com.kaltura.playkit.PKLog;

import java.util.UUID;

public class MediaSupport {

    public static final UUID WIDEVINE_UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");


    private static Boolean widevineClassic;
    private static Boolean widevineModular;

    private static final PKLog log = PKLog.get("MediaSupport");
    
    public static void initialize(@NonNull Context context) {
        widevineClassic(context);
        widevineModular();
    }

    public static boolean widevineClassic(Context context) {
        if (widevineClassic != null) {
            return widevineClassic;
        }
        DrmManagerClient drmManagerClient = new DrmManagerClient(context);
        try {
            widevineClassic = drmManagerClient.canHandle("", "video/wvm");
        } catch (IllegalArgumentException ex) {
            // On some Android devices, canHandle() fails when given an empty path (despite what
            // the API says). In that case, make a guess: Widevine Classic is always supported on
            // Google-certified devices from JellyBean (inclusive) to Marshmallow (exclusive).
            log.e("drmManagerClient.canHandle failed");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                log.w("Assuming WV Classic is supported although canHandle has failed");
                widevineClassic = true;
            }
        } finally {
            //noinspection deprecation
            drmManagerClient.release();
        }
        return widevineClassic;
    }

    public static boolean widevineModular() {
        if (widevineModular != null) {
            return widevineModular;
        }
        // Encrypted dash is only supported in Android v4.3 and up -- needs MediaDrm class.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // Make sure Widevine is supported.
            if (MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {
                widevineModular = true;
                return true;
            }
        }
        return false;
    }
}
