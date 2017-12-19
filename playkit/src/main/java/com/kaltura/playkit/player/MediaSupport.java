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
    private static boolean initSucceeded;
    
    // Should be called by applications that use DRM, to make sure they can handle provision issues.
    public static void checkDrm(Context context) throws DrmNotProvisionedException {
        if (widevineClassic == null) {
            checkWidevineClassic(context);
        }

        if (widevineModular == null) {
            checkWidevineModular();
        }
    }

    public interface DrmInitCallback {
        /**
         * Called when the DRM subsystem is initialized (with possible errors).
         * @param supportedDrmSchemes   supported DRM schemes
         * @param provisionPerformed    true if provisioning was required and performed, false otherwise
         * @param provisionError        null if provisioning is successful, exception otherwise
         */
        void onDrmInitComplete(Set<PKDrmParams.Scheme> supportedDrmSchemes, boolean provisionPerformed, Exception provisionError);
    }
    
    /**
     * Initialize the DRM subsystem, performing provisioning if required. The callback is called
     * when done. If provisioning was required, it is performed before the callback is called.
     * @param context           
     * @param drmInitCallback   callback object that will get the result. See {@link DrmInitCallback}.
     */
    public static void initializeDrm(Context context, final DrmInitCallback drmInitCallback) {
        
        try {
            checkWidevineClassic(context);
            checkWidevineModular();

            initSucceeded = true;
            
            runCallback(drmInitCallback, false, null);
            
        } catch (DrmNotProvisionedException e) {
            log.d("Widevine Modular needs provisioning");
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        try {
                            provisionWidevine();
                            runCallback(drmInitCallback, true, null);
                        } catch (Exception e) {
                            runCallback(drmInitCallback, true, e);
                        }
                    }
                }
            });
        }
    }

    private static void runCallback(DrmInitCallback drmInitCallback, boolean provisionPerformed, Exception provisionError) {

        final Set<PKDrmParams.Scheme> supportedDrmSchemes = supportedDrmSchemes();
        if (drmInitCallback != null) {
            drmInitCallback.onDrmInitComplete(supportedDrmSchemes, provisionPerformed, provisionError);
            
        } else if (!initSucceeded) {
            if (provisionError != null) {
                log.e("DRM provisioning has failed, but nobody was looking. supportedDrmSchemes may be missing Widevine Modular.");
            }
            log.i("Provisioning was" + (provisionPerformed ? " " : " not ") + "performed");
        }
        
        log.i("Supported DRM schemes " + supportedDrmSchemes);
    }

    public static class DrmNotProvisionedException extends Exception {
        DrmNotProvisionedException(String message, Exception e) {
            super(message, e);
        }
    }
    
    public static final UUID WIDEVINE_UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");


    private static Boolean widevineClassic;
    private static Boolean widevineModular;

    /**
     * @deprecated This method does not perform possibly required DRM provisioning. Call {@link #initializeDrm(Context, DrmInitCallback)} instead.
     */
    @Deprecated
    public static Set<PKDrmParams.Scheme> supportedDrmSchemes(Context context) {
        log.w("Warning: MediaSupport.supportedDrmSchemes(Context) is deprecated");
        checkWidevineClassic(context);
        try {
            checkWidevineModular();
        } catch (DrmNotProvisionedException e) {
            log.e("Widevine Modular needs provisioning");
        }
        
        return supportedDrmSchemes();
    }
    
    private static Set<PKDrmParams.Scheme> supportedDrmSchemes() {

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
        if (widevineClassic != null) {
            return;
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
        
        // Still null? that means no.
        if (widevineClassic == null) {
            widevineClassic = false;
        }
    }

    public static boolean widevineClassic() {
        if (widevineClassic == null) {
            log.w("Widevine Classic DRM is not initialized; assuming not supported");
            return false;
        }
        
        return widevineClassic;
    }
    
    public static boolean widevineModular() {
        if (widevineModular == null) {
            log.w("Widevine Modular DRM is not initialized; assuming not supported");
            return false;
        }

        return widevineModular;
    }

    private static void checkWidevineModular() throws DrmNotProvisionedException {

        if (widevineModular != null) {
            return;
        }
        
        // Encrypted dash is only supported in Android v4.3 and up -- needs MediaDrm class.
        // Make sure Widevine is supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {
            
            // Open a session to check if Widevine needs provisioning.
            MediaDrm mediaDrm = null;
            byte[] session = null;
            try {
                mediaDrm = new MediaDrm(WIDEVINE_UUID);
                session = mediaDrm.openSession();
                widevineModular = true;
            } catch (NotProvisionedException e) {
                log.e("Widevine Modular not provisioned");
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
        } else {
            widevineModular = false;
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
            widevineModular = true; // provisioning didn't fail
            
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
