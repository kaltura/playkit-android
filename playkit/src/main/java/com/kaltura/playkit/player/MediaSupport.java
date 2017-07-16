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

import android.content.Context;
import android.drm.DrmManagerClient;
import android.media.MediaDrm;
import android.os.Build;
import android.support.annotation.NonNull;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @hide
 */
public class MediaSupport {

    public static final UUID WIDEVINE_UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");


    private static boolean widevineClassic = false;
    private static boolean widevineModular = false;
    private static boolean initialized = false;

    private static final PKLog log = PKLog.get("MediaSupport");

    public static void initialize(@NonNull final Context context) {
        if (initialized) {
            return;
        }
        checkWidevineClassic(context);
        widevineModular();
        initialized = true;
    }

    public static Set<PKDrmParams.Scheme> supportedDrmSchemes(Context context) {

        initialize(context);

        HashSet<PKDrmParams.Scheme> schemes = new HashSet<>();

        if (widevineModular()) {
            schemes.add(PKDrmParams.Scheme.WidevineCENC);
        }

        if (widevineClassic()) {
            schemes.add(PKDrmParams.Scheme.WidevineClassic);
        }

        if (playReady()) {
            schemes.add(PKDrmParams.Scheme.PlayReadyCENC);
        }

        return schemes;
    }

    private static void checkWidevineClassic(Context context) {
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
    }

    public static boolean widevineClassic() {
        if (initialized) {
            return widevineClassic;
        }

        log.w("MediaSupport not initialized; assuming no Widevine Classic support");
        return false;
    }

    public static boolean widevineModular() {
        // Encrypted dash is only supported in Android v4.3 and up -- needs MediaDrm class.
        // Make sure Widevine is supported
        if (!initialized && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {

            MediaDrm mediaDrm = null;
            byte[] session = null;
            try {
                mediaDrm = new MediaDrm(WIDEVINE_UUID);
                session = mediaDrm.openSession();
                widevineModular = true;
            } catch (Exception e) {
                widevineModular = false;
            } finally {
                if (session != null) {
                    mediaDrm.closeSession(session);
                }
                if (mediaDrm != null) {
                    mediaDrm.release();
                }
            }
        }
        return widevineModular;
    }

    public static boolean playReady() {
        return Boolean.parseBoolean("false");   // Not yet.
    }
}
