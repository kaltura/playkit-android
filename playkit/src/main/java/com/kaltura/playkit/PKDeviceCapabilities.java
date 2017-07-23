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
import android.media.UnsupportedSchemeException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Noam Tamim @ Kaltura on 25/04/2017.
 */
public class PKDeviceCapabilities {
    private static final PKLog log = PKLog.get("PKDeviceCapabilities");
    
    private static final UUID WIDEVINE_UUID = new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
    private static final String SHARED_PREFS_NAME = "PKDeviceCapabilities";
    private static final String PREFS_ENTRY_FINGERPRINT = "Build.FINGERPRINT";
    private static final String DEVICE_CAPABILITIES_URL = "https://cdnapisec.kaltura.com/api_v3/index.php?service=stats&action=reportDeviceCapabilities";
    
    private static boolean reportSent = false;
    
    private final Context context;
    private final JSONObject root = new JSONObject();

    public static JsonObject getReport(Context ctx) {
        PKDeviceCapabilities collector = new PKDeviceCapabilities(ctx);
        JSONObject collect = collector.collect();
        return new JsonParser().parse(collect.toString()).getAsJsonObject();
    }

    private PKDeviceCapabilities(Context context) {
        this.context = context;
    }

    static void maybeSendReport(final Context context) {
        if (reportSent) {
            return;
        }

        final SharedPreferences sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String fingerprint = sharedPrefs.getString(PREFS_ENTRY_FINGERPRINT, null);
        
        // If we already sent capabilities for this Android build, don't send again.
        if (Build.FINGERPRINT.equals(fingerprint)) {
            reportSent = true;
            return;
        }

        new Thread() {
            @Override
            public void run() {
                sendReport(context, sharedPrefs);
            }
        }.start();
    }

    private static void sendReport(Context context, SharedPreferences sharedPrefs) {

        JsonObject report = getReport(context);
        
        JsonObject data = new JsonObject();
        data.addProperty("data", report.toString());

        String dataString = data.toString();
        try {
            Map<String, String> headers = new HashMap<>(1);
            headers.put("Content-Type", "application/json");
            
            byte[] bytes = Utils.executePost(DEVICE_CAPABILITIES_URL, dataString.getBytes(), headers);
            log.d("Sent report, response was: " + new String(bytes));
        } catch (IOException e) {
            log.e("Failed to report device capabilities", e);
            return;
        }

        // If we got here, save the fingerprint so we don't send again until the OS updates.
        sharedPrefs.edit().putString(PREFS_ENTRY_FINGERPRINT, Build.FINGERPRINT).apply();
        reportSent = true;
    }

    private JSONObject collect() {
        try {
            JSONObject root = this.root;
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
        } catch (Exception e) {
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
        return new JSONObject()
                .put("supportedTypes", jsonArray(mediaCodec.getSupportedTypes()));
    }

    private JSONObject mediaCodecInfo() throws JSONException {

        ArrayList<MediaCodecInfo> mediaCodecs = new ArrayList<>();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            MediaCodecInfo[] codecInfos = mediaCodecList.getCodecInfos();

            Collections.addAll(mediaCodecs, codecInfos);
        } else {
            for (int i=0, n=MediaCodecList.getCodecCount(); i<n; i++) {
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
                    .put("widevine", widevineModularDrmInfo());
        } else {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private JSONObject widevineModularDrmInfo() throws JSONException {
        if (!MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {
            return null;
        }

        MediaDrm mediaDrm;
        try {
            mediaDrm = new MediaDrm(WIDEVINE_UUID);
        } catch (UnsupportedSchemeException e) {
            return null;
        }

        final JSONArray mediaDrmEvents = new JSONArray();

        mediaDrm.setOnEventListener(new MediaDrm.OnEventListener() {
            @Override
            public void onEvent(@NonNull MediaDrm md, byte[] sessionId, int event, int extra, byte[] data) {
                try {
                    String encodedData = data == null ? null : Base64.encodeToString(data, Base64.NO_WRAP);

                    mediaDrmEvents.put(new JSONObject().put("event", event).put("extra", extra).put("data", encodedData));
                } catch (JSONException e) {
                    log.e("JSONError", e);
                }
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
            } catch (IllegalStateException e) {
                value = "<unknown>";
            }
            props.put(prop, value);
        }
        for (String prop : byteArrayProps) {
            String value;
            try {
                value = Base64.encodeToString(mediaDrm.getPropertyByteArray(prop), Base64.NO_WRAP);
            } catch (IllegalStateException|NullPointerException e) {
                value = "<unknown>";
            }
            props.put(prop, value);
        }

        JSONObject response = new JSONObject();
        response.put("properties", props);
        response.put("events", mediaDrmEvents);

        return response;
    }

    private JSONObject systemInfo() throws JSONException {
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
                .put("TAGS", Build.TAGS)
                .put("FINGERPRINT", Build.FINGERPRINT)
                .put("ARCH", arch);
    }
}
