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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.drm.DrmManagerClient;
import android.media.MediaDrm;
import android.media.NotProvisionedException;
import android.os.AsyncTask;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import android.util.Base64;
import android.util.Log;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.Utils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @hide
 */
public class MediaSupport {
    private static final PKLog log = PKLog.get("MediaSupport");

    public static final UUID WIDEVINE_UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");
    public static final UUID PLAYREADY_UUID = UUID.fromString("9a04f079-9840-4286-ab92-e65be0885f95");
    private static final String WIDEVINE_SECURITY_LEVEL_1 = "L1";
    private static final String WIDEVINE_SECURITY_LEVEL_3 = "L3";
    private static final String SECURITY_LEVEL_PROPERTY = "securityLevel";

    private static boolean initSucceeded;
    @Nullable private static Boolean widevineClassic;
    @Nullable private static Boolean widevineModular;
    @Nullable private static String securityLevel;
    private static Player player;

    public static final String DEVICE_CHIPSET = getDeviceChipset();


    private static String getDeviceChipset() {
        try {
            @SuppressLint("PrivateApi")
            Class<?> aClass = Class.forName("android.os.SystemProperties");
            Method method = aClass.getMethod("get", String.class);
            Object platform = method.invoke(null, "ro.board.platform");

            return platform instanceof String ? (String) platform : "<" + platform + ">";
        } catch (Exception e) {
            return "<" + e + ">";
        }
    }

    // Should be called by applications that use DRM, to make sure they can handle provision issues.
    public static void checkDrm(Context context, boolean forceWidevineL3Provisioning) throws DrmNotProvisionedException {
        if (widevineClassic == null) {
            checkWidevineClassic(context);
        }

        if (widevineModular == null) {
            checkWidevineModular(forceWidevineL3Provisioning);
        }
    }

    /**
     * Initialize the DRM subsystem, performing provisioning if required. The callback is called
     * when done. If provisioning was required, it is performed before the callback is called.
     *
     * @param context
     * @param drmInitCallback callback object that will get the result. See {@link DrmInitCallback}.
     */
    public static void initializeDrm(Context context, final DrmInitCallback drmInitCallback) {
        initializeDrm(context, false, drmInitCallback);
    }

    /**
     * Initialize the DRM subsystem, performing provisioning if required and L3 provisioning
     * on L1 device if forceWidevineL3Provisioning is set to true. The callback is called
     * when done. If provisioning was required, it is performed before the callback is called.
     *
     * @param context
     * @param forceWidevineL3Provisioning - this flag will force L3 provision on L1 Device.
     * @param drmInitCallback callback object that will get the result. See {@link DrmInitCallback}.
     */
     private static void initializeDrm(Context context, boolean forceWidevineL3Provisioning, final DrmInitCallback drmInitCallback) {
         log.d("initializeDrm forceWidevineL3Provisioning = " + forceWidevineL3Provisioning);

        if (initSucceeded) {
            runCallback(drmInitCallback, hardwareDrm(), false, null);
            return;
        }

        if (forceWidevineL3Provisioning) {
            provisionWidevineBySecLevel(WIDEVINE_SECURITY_LEVEL_1);
        } else {
            provisionWidevineBySecLevel(WIDEVINE_SECURITY_LEVEL_3);
        }

        try {
            checkWidevineClassic(context);
            checkWidevineModular(forceWidevineL3Provisioning);

            initSucceeded = true;
            runCallback(drmInitCallback, hardwareDrm(), false, null);

        } catch (DrmNotProvisionedException drmNotProvisionedException) {
            String secLevel = (forceWidevineL3Provisioning) ? "L3" : "L1";
            log.d("Widevine Modular " + secLevel + " needs provisioning");
            AsyncTask.execute(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    try {
                        provisionWidevine(forceWidevineL3Provisioning);
                        runCallback(drmInitCallback, hardwareDrm(), true, null);
                    } catch (Exception exception) {
                        // Send any exception to the callback
                        log.e("Widevine " + secLevel + " provisioning has failed", exception);
                        runCallback(drmInitCallback, hardwareDrm(), true, exception);
                    }
                }
            });
        }
    }

    private static void provisionWidevineBySecLevel(final String secLevel) {
        //log.d("provisionWidevineBySecLevel " + secLevel);
        try {
            checkWidevineModular(WIDEVINE_SECURITY_LEVEL_3.equals(secLevel));
        } catch (DrmNotProvisionedException drmNotProvisionedException) {
            log.d("Widevine Modular " + secLevel + " needs provisioning");
            AsyncTask.execute(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    try {
                        provisionWidevine(WIDEVINE_SECURITY_LEVEL_3.equals(secLevel));
                    } catch (Exception exception) {
                        // Send any exception to the callback
                        log.e("Widevine " + secLevel + " provisioning has failed", exception);
                    }
                }
            });
        }
        // cleanup so next checkWidevineModular will not be ignored
        widevineModular = null;
    }

    private static void checkWidevineModular(boolean forceWidevineL3Provisioning) throws DrmNotProvisionedException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            widevineModular = WidevineModularUtil.checkWidevineModular(widevineModular, forceWidevineL3Provisioning);
        }
    }

    private static void runCallback(DrmInitCallback drmInitCallback, boolean isHardwareDrmSupported, boolean provisionPerformed, Exception provisionError) {

        final Set<PKDrmParams.Scheme> supportedDrmSchemes = supportedDrmSchemes();
        if (drmInitCallback != null) {
            drmInitCallback.onDrmInitComplete(new PKDeviceCapabilitiesInfo(supportedDrmSchemes, isHardwareDrmSupported, provisionPerformed,
                    PKCodecSupport.isSoftwareHevcSupported(), PKCodecSupport.isHardwareHevcSupported()), provisionError);

        } else if (!initSucceeded) {
            if (provisionError != null) {
                log.e("DRM provisioning has failed, but nobody was looking. supportedDrmSchemes may be missing Widevine Modular.");
            }
            log.i("Provisioning was" + (provisionPerformed ? " " : " not ") + "performed");
        }

        log.i("Supported DRM schemes " + supportedDrmSchemes);
    }

    /**
     * @deprecated This method does not perform possibly required DRM provisioning. Call {@link #initializeDrm(Context, DrmInitCallback)} instead.
     */
    @Deprecated
    public static Set<PKDrmParams.Scheme> supportedDrmSchemes(Context context, boolean forceWidevineL3Provisioning) {
        log.w("Warning: MediaSupport.supportedDrmSchemes(Context) is deprecated");
        checkWidevineClassic(context);
        try {
            checkWidevineModular(forceWidevineL3Provisioning);
        } catch (DrmNotProvisionedException drmNotProvisionedException) {
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

    public static boolean hardwareDrm() {
        if (widevineModular()) {
            return WIDEVINE_SECURITY_LEVEL_1.equals(securityLevel);
        }
        return false;
    }

    public static boolean playReady() {
        return Boolean.FALSE;   // Not yet.
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void provisionWidevine(boolean forceWidevineL3Provisioning) throws Exception {
        log.d("provisionWidevine forceWidevineL3Provisioning = " + forceWidevineL3Provisioning);

        MediaDrm mediaDrm = null;
        try {


            mediaDrm = new MediaDrm(WIDEVINE_UUID);
            if (forceWidevineL3Provisioning) {
                mediaDrm.setPropertyString(SECURITY_LEVEL_PROPERTY, WIDEVINE_SECURITY_LEVEL_3);
            }
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

    public interface DrmInitCallback {
        /**
         * Called when the DRM subsystem is initialized (with possible errors).
         *
         * @param pkDeviceCapabilitiesInfo model consist of various device codec and DRM level info {@link PKDeviceCapabilitiesInfo}
         * @param provisionError null if provisioning is successful, exception otherwise
         */
        void onDrmInitComplete(PKDeviceCapabilitiesInfo pkDeviceCapabilitiesInfo, Exception provisionError);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static class WidevineModularUtil {

        @SuppressLint("WrongConstant")
        private static Boolean checkWidevineModular(Boolean widevineModular, boolean forceWidevineL3Provisioning) throws MediaSupport.DrmNotProvisionedException {
            log.d("checkWidevineModular forceWidevineL3Provisioning = " + forceWidevineL3Provisioning);

            if (widevineModular != null) {
                log.d("widevineModular != null return");
                return widevineModular;
            }

            // Encrypted dash is only supported in Android v4.3 and up -- needs MediaDrm class.
            // Make sure Widevine is supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && MediaDrm.isCryptoSchemeSupported(MediaSupport.WIDEVINE_UUID)) {

                // Open a session to check if Widevine needs provisioning.
                MediaDrm mediaDrm = null;
                byte[] session = null;
                try {
                    mediaDrm = new MediaDrm(MediaSupport.WIDEVINE_UUID);
                    if (forceWidevineL3Provisioning) {
                        mediaDrm.setPropertyString(SECURITY_LEVEL_PROPERTY, WIDEVINE_SECURITY_LEVEL_3);
                    }
                    session = mediaDrm.openSession();
                    widevineModular = true;
                    try {
                        securityLevel = mediaDrm.getPropertyString(SECURITY_LEVEL_PROPERTY);
                    } catch (RuntimeException e) {
                        securityLevel = null;                    
                    }
                } catch (NotProvisionedException notProvisionedException) {
                    String secLevel = forceWidevineL3Provisioning ? "L3" : "L1";
                    log.e("Widevine Modular " + secLevel + " not provisioned");
                    throw new MediaSupport.DrmNotProvisionedException("Widevine Modular " + secLevel + " not provisioned", notProvisionedException);
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
            return widevineModular;
        }
    }

    public static class DrmNotProvisionedException extends Exception {
        DrmNotProvisionedException(String message, Exception e) {
            super(message, e);
        }
    }
}
