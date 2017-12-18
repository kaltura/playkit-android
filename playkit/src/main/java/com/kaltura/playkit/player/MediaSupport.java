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
import android.media.NotProvisionedException;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @hide
 */
public class MediaSupport {

    private static final PKLog log = PKLog.get("MediaSupport");

    // Should be called by applications that use DRM, to make sure they can handle provision issues.
    public static void initializeDrm(Context context) throws DrmNotProvisionedException {
        if (widevineClassic == null) {
            checkWidevineClassic(context);
        }

        if (widevineModular == null) {
            checkWidevineModular();
        }
    }

    // Should only be called by the SDK, if the app didn't call initializeDrm().
    public static void initializeDrmQuiet(Context context) {
        try {
            initializeDrm(context);
        } catch (Exception e) {
            log.e("MediaSupport.initializeDrm() failed", e);
        }
    }

    public static class DrmNotProvisionedException extends Exception {
        DrmNotProvisionedException(Exception e) {
            super(e);
        }

        DrmNotProvisionedException(String message, Exception e) {
            super(message, e);
        }
    }
    
    public interface DrmProvisionCallback {
        void onDrmProvisionComplete(Exception e);
    }

    public static final UUID WIDEVINE_UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");


    private static Boolean widevineClassic;
    private static Boolean widevineModular;

    public static void attemptDrmProvision(final Context context, final DrmProvisionCallback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    try {
                        provisionWidevine();
                        initializeDrm(context);
                        if (callback != null) {
                            callback.onDrmProvisionComplete(null);
                        }
                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onDrmProvisionComplete(e);
                        }
                    }
                }
            }
        });
    }

    public static Set<PKDrmParams.Scheme> supportedDrmSchemes(Context context) throws DrmNotProvisionedException {

        initializeDrm(context);

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
        if (widevineClassic == null) {
            log.w("MediaSupport not initialized; assuming no Widevine Classic support");
            return false;
        }
        
        return widevineClassic;
    }
    
    public static boolean widevineModular() {
        if (widevineModular == null) {
            log.w("MediaSupport not initialized; assuming no Widevine Modular support");
            return false;
        }

        return widevineModular;
    }

    private static void checkWidevineModular() throws DrmNotProvisionedException {
        // Encrypted dash is only supported in Android v4.3 and up -- needs MediaDrm class.
        // Make sure Widevine is supported
        if (widevineModular == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {

            MediaDrm mediaDrm = null;
            byte[] session = null;
            try {
                mediaDrm = new MediaDrm(WIDEVINE_UUID);
                session = mediaDrm.openSession();
                widevineModular = true;
            } catch (NotProvisionedException e) {
                log.e("Widevine Modular not provisioned", e);
                throw new DrmNotProvisionedException("Widevine Modular not provisioned", e);
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
    }

    public static boolean playReady() {
        return Boolean.FALSE;   // Not yet.
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void provisionWidevine() throws Exception {
        MediaDrm mediaDrm = null;
        try {
            mediaDrm = new MediaDrm(WIDEVINE_UUID);
            MediaDrm.ProvisionRequest provisionRequest = mediaDrm.getProvisionRequest();
            String url = provisionRequest.getDefaultUrl() + "&signedRequest=" + new String(provisionRequest.getData());

            final byte[] response = Utils.executePost(url, null, null);

            Log.d("RESULT", Base64.encodeToString(response, Base64.NO_WRAP));

            mediaDrm.provideProvisionResponse(response);
        } catch (Exception e) {
            log.e("Provision Widevine failed", e);
            throw e;
            
        } finally {
            if (mediaDrm != null) {
                mediaDrm.release();
            }
        }
    }

}
