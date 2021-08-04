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

package com.kaltura.playkit;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.drm.DrmInfo;
import android.drm.DrmInfoRequest;
import android.drm.DrmManagerClient;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaDrm;
import android.os.AsyncTask;
import android.os.Build;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonObject;
import com.kaltura.playkit.player.MediaSupport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

public class PKDeviceCapabilities {
    private static final PKLog log = PKLog.get("PKDeviceCapabilities");

    private static final UUID WIDEVINE_UUID = new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
    private static final UUID PLAYREADY_UUID = new UUID(0x9A04F07998404286L, 0xAB92E65BE0885F95L);

    public static final String SHARED_PREFS_NAME = "PKDeviceCapabilities";
    private static final String PREFS_ENTRY_FINGERPRINT = "Build.FINGERPRINT";
    private static final String DEVICE_CAPABILITIES_URL = "https://cdnapisec.kaltura.com/api_v3/index.php?service=stats&action=reportDeviceCapabilities";
    private static final String FINGERPRINT = Build.FINGERPRINT;

    private static boolean reportSent = false;

    private final Context context;
    private final JSONObject root = new JSONObject();

    public static String getReport(Context ctx) {
        PKDeviceCapabilities collector = new PKDeviceCapabilities(ctx);
        JSONObject collect = collector.collect();
        return collect.toString();
    }

    private PKDeviceCapabilities(Context context) {
        this.context = context;
    }

    static void maybeSendReport(final Context context) {
        if (reportSent) {
            return;
        }

        final SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String savedFingerprint = sharedPrefs.getString(PREFS_ENTRY_FINGERPRINT, null);

        // If we already sent capabilities for this Android build, don't send again.
        if (FINGERPRINT.equals(savedFingerprint)) {
            reportSent = true;
            return;
        }

        // Do everything else in a thread.
        AsyncTask.execute(() -> sendReport(context, sharedPrefs));
    }

    public static String getErrorReport(Exception e) {

        try {
            return new JSONObject()
                    .put("system", systemInfo())
                    .put("error", Log.getStackTraceString(e))
                    .toString();
        } catch (JSONException e1) {
            return "{\"error\": \"Failed to create error object\"}";
        }
    }

    private static void sendReport(Context context, SharedPreferences sharedPrefs) {

        String reportString;
        try {
            reportString = getReport(context);
        } catch (RuntimeException e) {
            log.e("Failed to get report", e);
            reportString = getErrorReport(e);
        }

        if (!sendReport(reportString)) {
            return;
        }

        // If we got here, save the fingerprint so we don't send again until the OS updates.
        sharedPrefs.edit().putString(PREFS_ENTRY_FINGERPRINT, FINGERPRINT).apply();
        reportSent = true;
    }

    public static boolean sendReport(String reportString) {

        // Compress the report
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(reportString.length() / 2);
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(reportString.getBytes());
            gzipOutputStream.finish();
        } catch (IOException e) {
            log.e("Failed to compress report data", e);
            return false;
        }

        final String compressedString = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP);

        JsonObject data = new JsonObject();
        data.addProperty("data", compressedString);

        String dataString = data.toString();
        try {
            Map<String, String> headers = new HashMap<>(1);
            headers.put("Content-Type", "application/json");

            byte[] bytes = Utils.executePost(DEVICE_CAPABILITIES_URL, dataString.getBytes(), headers);
            log.d("Sent report, response was: " + new String(bytes));
        } catch (IOException e) {
            log.e("Failed to report device capabilities", e);
            return false;
        }

        return true;
    }

    private JSONObject collect() {
        try {
            JSONObject root = this.root;
            root.put("reportType", "DeviceCapabilities");
            root.put("playkitVersion", PlayKitManager.VERSION_STRING);
            root.put("kalturaPlayer", isKalturaPlayerAvailable());
            root.put("host", hostInfo());
            root.put("system", systemInfo());
            root.put("drm", drmInfo());
            root.put("display", displayInfo());
            root.put("media", mediaCodecInfo());

        } catch (JSONException e) {
            log.e("Error", e);
        }

        return root;
    }

    public static boolean isKalturaPlayerAvailable() {
        try {
            Class.forName( "com.kaltura.tvplayer.KalturaPlayer" );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private JSONObject hostInfo() throws JSONException {
        JSONObject result = new JSONObject();
        String packageName = context.getPackageName();
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);

            result
                    .put("packageName", packageName)
                    .put("versionCode", packageInfo.versionCode)
                    .put("versionName", packageInfo.versionName)
                    .put("firstInstallTime", packageInfo.firstInstallTime)
                    .put("lastUpdateTime", packageInfo.lastUpdateTime)
                    .put("playkitVersion", PlayKitManager.VERSION_STRING);

        } catch (PackageManager.NameNotFoundException e) {
            log.e("Failed to get package info", e);
            result.put("error", "Failed to get package info");
        }

        return result;
    }

    private JSONObject displayInfo() throws JSONException {
        return new JSONObject().put("metrics", context.getResources().getDisplayMetrics().toString());
    }

    private JSONObject drmInfo() throws JSONException {
        return new JSONObject()
                .put("modular", modularDrmInfo())
                .put("classic", classicDrmInfo());

    }

    private JSONObject classicDrmInfo() throws JSONException {
        JSONObject json = new JSONObject();

        DrmManagerClient drmManagerClient = new DrmManagerClient(context);
        String[] availableDrmEngines = drmManagerClient.getAvailableDrmEngines();

        JSONArray engines = jsonArray(availableDrmEngines);
        json.put("engines", engines);

        try {
            if (drmManagerClient.canHandle("", "video/wvm")) {
                DrmInfoRequest request = new DrmInfoRequest(DrmInfoRequest.TYPE_REGISTRATION_INFO, "video/wvm");
                request.put("WVPortalKey", "OEM");
                DrmInfo response = drmManagerClient.acquireDrmInfo(request);
                String status = (String) response.get("WVDrmInfoRequestStatusKey");

                status = new String[]{"HD_SD", null, "SD"}[Integer.parseInt(status)];
                json.put("widevine",
                        new JSONObject()
                                .put("version", response.get("WVDrmInfoRequestVersionKey"))
                                .put("status", status)
                );
            }
        } catch (RuntimeException e) {
            json.put("error", e.getMessage() + '\n' + Log.getStackTraceString(e));
        }

        //noinspection deprecation
        drmManagerClient.release();

        return json;
    }

    @NonNull
    private JSONArray jsonArray(String[] stringArray) {
        JSONArray jsonArray = new JSONArray();
        for (String string : stringArray) {
            if (!TextUtils.isEmpty(string)) {
                jsonArray.put(string);
            }
        }
        return jsonArray;
    }

    private JSONObject mediaCodecInfo(MediaCodecInfo mediaCodec) throws JSONException {

        JSONObject codecInfo =  new JSONObject();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            codecInfo.put("isVendor", mediaCodec.isVendor());
            codecInfo.put("isSoftwareOnly", mediaCodec.isSoftwareOnly());
            codecInfo.put("isHardwareAccelerated", mediaCodec.isHardwareAccelerated());
        }
        codecInfo.put("supportedTypes", jsonArray(mediaCodec.getSupportedTypes()));
        return codecInfo;
    }

    private JSONObject mediaCodecInfo() throws JSONException {

        ArrayList<MediaCodecInfo> mediaCodecs = new ArrayList<>();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            MediaCodecInfo[] codecInfos = mediaCodecList.getCodecInfos();

            Collections.addAll(mediaCodecs, codecInfos);
        } else {
            for (int i = 0, n = MediaCodecList.getCodecCount(); i < n; i++) {
                mediaCodecs.add(MediaCodecList.getCodecInfoAt(i));
            }
        }

        ArrayList<MediaCodecInfo> decoders = new ArrayList<>();
        for (MediaCodecInfo mediaCodec : mediaCodecs) {
            if (!mediaCodec.isEncoder()) {
                decoders.add(mediaCodec);
            }
        }

        JSONObject info = new JSONObject();
        JSONObject jsonDecoders = new JSONObject();
        for (MediaCodecInfo mediaCodec : decoders) {
            jsonDecoders.put(mediaCodec.getName(), mediaCodecInfo(mediaCodec));
        }
        info.put("decoders", jsonDecoders);

        return info;

    }

    private JSONObject modularDrmInfo() throws JSONException {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return new JSONObject()
                    .put("widevine", widevineModularDrmInfo())
                    .put("playready", playreadyDrmInfo());
        } else {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private JSONObject playreadyDrmInfo() throws JSONException {
        if (!MediaDrm.isCryptoSchemeSupported(PLAYREADY_UUID)) {
            return null;
        }

        // No information other than "supported".
        return new JSONObject().put("supported", true);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private JSONObject widevineModularDrmInfo() throws JSONException {
        if (!MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {
            return null;
        }

        MediaDrm mediaDrm;
        try {
            mediaDrm = new MediaDrm(WIDEVINE_UUID);
        } catch (Exception e) {
            return null;
        }

        final JSONArray mediaDrmEvents = new JSONArray();

        mediaDrm.setOnEventListener((md, sessionId, event, extra, data) -> {
            try {
                String encodedData = data == null ? null : Base64.encodeToString(data, Base64.NO_WRAP);

                mediaDrmEvents.put(new JSONObject().put("event", event).put("extra", extra).put("data", encodedData));
            } catch (JSONException e) {
                log.e("JSONError", e);
            }
        });

        try {
            byte[] session;
            session = mediaDrm.openSession();
            mediaDrm.closeSession(session);
        } catch (Exception e) {
            mediaDrmEvents.put(new JSONObject().put("Exception(openSession)", e.toString()));
        }


        String[] stringProps = {MediaDrm.PROPERTY_VENDOR, MediaDrm.PROPERTY_VERSION, MediaDrm.PROPERTY_DESCRIPTION, MediaDrm.PROPERTY_ALGORITHMS, "securityLevel", "systemId", "privacyMode", "sessionSharing", "usageReportingSupport", "appId", "origin", "hdcpLevel", "maxHdcpLevel", "maxNumberOfSessions", "numberOfOpenSessions"};
        String[] byteArrayProps = {"serviceCertificate"};

        JSONObject props = new JSONObject();

        for (String prop : stringProps) {
            String value;
            try {
                value = mediaDrm.getPropertyString(prop);

            } catch (RuntimeException e) {
                value = "<" + e + ">";
            }
            props.put(prop, value);
        }
        for (String prop : byteArrayProps) {
            String value;
            try {
                value = Base64.encodeToString(mediaDrm.getPropertyByteArray(prop), Base64.NO_WRAP);

            } catch (RuntimeException e) {
                value = "<" + e + ">";
            }
            props.put(prop, value);
        }

        JSONObject response = new JSONObject();
        response.put("properties", props);
        response.put("events", mediaDrmEvents);

        mediaDrm.release();

        return response;
    }

    public static JSONObject systemInfo() throws JSONException {
        JSONObject arch = new JSONObject().put("os.arch", System.getProperty("os.arch"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            arch.put("SUPPORTED_ABIS", new JSONArray(Build.SUPPORTED_ABIS));
            arch.put("SUPPORTED_32_BIT_ABIS", new JSONArray(Build.SUPPORTED_32_BIT_ABIS));
            arch.put("SUPPORTED_64_BIT_ABIS", new JSONArray(Build.SUPPORTED_64_BIT_ABIS));
        }
        return new JSONObject()
                .put("RELEASE", Build.VERSION.RELEASE)
                .put("SDK_INT", Build.VERSION.SDK_INT)
                .put("BRAND", Build.BRAND)
                .put("MODEL", Build.MODEL)
                .put("MANUFACTURER", Build.MANUFACTURER)
                .put("DEVICE", Build.DEVICE)
                .put("TAGS", Build.TAGS)
                .put("FINGERPRINT", FINGERPRINT)
                .put("ARCH", arch)
                .put("CHIPSET", MediaSupport.DEVICE_CHIPSET);
    }
}
