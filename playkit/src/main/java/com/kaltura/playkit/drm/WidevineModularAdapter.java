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

package com.kaltura.playkit.drm;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.DeniedByServerException;
import android.media.MediaCryptoException;
import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.media.NotProvisionedException;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;

import com.kaltura.android.exoplayer2.ExoPlayerLibraryInfo;
import com.kaltura.android.exoplayer2.drm.ExoMediaDrm;
import com.kaltura.android.exoplayer2.drm.FrameworkMediaDrm;
import com.kaltura.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.kaltura.android.exoplayer2.drm.UnsupportedDrmException;
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.playkit.*;
import com.kaltura.playkit.player.MediaSupport;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.kaltura.playkit.Utils.toBase64;

/**
 * Created by anton.afanasiev on 13/12/2016.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class WidevineModularAdapter extends DrmAdapter {

    private static final PKLog log = PKLog.get("WidevineModularAdapter");

    private Context context;
    private final LocalDataStore localDataStore;


    public WidevineModularAdapter(Context context, LocalDataStore localDataStore) {
        this.context = context;
        this.localDataStore = localDataStore;
    }

    @Override
    public boolean registerAsset(String localAssetPath, String assetId, String licenseUri, PKRequestParams.Adapter adapter, LocalAssetsManager.AssetRegistrationListener listener) {

        try {
            boolean result = registerAsset(localAssetPath, assetId, licenseUri, adapter);
            if (listener != null) {
                listener.onRegistered(localAssetPath);
            }
            return result;
        } catch (LocalAssetsManager.RegisterException e) {
            if (listener != null) {
                listener.onFailed(localAssetPath, e);
            }
            return false;
        }
    }

    private boolean registerAsset(String localAssetPath, String assetId, String licenseUri, PKRequestParams.Adapter requestParamsAdapter) throws LocalAssetsManager.RegisterException {

        // obtain the dash manifest.
        SimpleDashParser dash = parseDash(localAssetPath, assetId);

        if (!dash.hasContentProtection) {
            // Not protected -- nothing to do.
            return true;
        }

        String mimeType = dash.format.containerMimeType;
        byte[] initData = dash.widevineInitData;

        registerAsset(initData, mimeType, licenseUri, requestParamsAdapter);

        return true;
    }

    public void registerAsset(byte[] initData, String mimeType, String licenseUri, PKRequestParams.Adapter requestParamsAdapter) throws LocalAssetsManager.RegisterException {
        final byte[] offlineKeyId = downloadOfflineLicense(licenseUri, requestParamsAdapter, mimeType, initData);
        localDataStore.save(toBase64(initData), offlineKeyId);
    }

    private byte[] downloadOfflineLicense(String licenseUri, PKRequestParams.Adapter requestParamsAdapter, String mimeType, byte[] initData) throws LocalAssetsManager.RegisterException {
        MediaDrmSession session;
        FrameworkMediaDrm mediaDrm = createMediaDrm();
        try {
            session = MediaDrmSession.open(mediaDrm);
        } catch (Exception e) {
            throw new LocalAssetsManager.RegisterException("Can't open session", e);
        }

        // Get keyRequest
        try {
            FrameworkMediaDrm.KeyRequest keyRequest = session.getOfflineKeyRequest(initData, mimeType);
            log.d("registerAsset: init data (b64): " + toBase64(initData));

            byte[] data = keyRequest.getData();
            log.d("registerAsset: request data (b64): " + toBase64(data));

            // Send request to server
            byte[] keyResponse;
            try {
                keyResponse = executeKeyRequest(licenseUri, keyRequest, requestParamsAdapter);
                log.d("registerAsset: response data (b64): " + toBase64(keyResponse));

            } catch (Exception e) {
                throw new LocalAssetsManager.RegisterException("Can't send key request for registration", e);
            }

            // Provide keyResponse
            try {
                return session.provideKeyResponse(keyResponse);

            } catch (Exception e) {
                throw new LocalAssetsManager.RegisterException("Request denied by server", e);
            }

        } catch (WidevineNotSupportedException e) {
            throw new LocalAssetsManager.RegisterException("Can't execute KeyRequest", e);

        } finally {
            session.close();
        }
    }

    @Override
    public boolean unregisterAsset(String localAssetPath, String assetId, LocalAssetsManager.AssetRemovalListener listener) {

        try {
            unregisterAsset(localAssetPath, assetId);
            return true;
        } catch (LocalAssetsManager.RegisterException e) {
            log.e("Failed to unregister", e);
            return false;
        } finally {
            if (listener != null) {
                listener.onRemoved(localAssetPath);
            }
        }
    }

    private boolean unregisterAsset(String localAssetPath, String assetId) throws LocalAssetsManager.RegisterException {

        SimpleDashParser dash = parseDash(localAssetPath, assetId);
        if (!dash.hasContentProtection) {
            // Not protected -- nothing to do.
            return true;
        }

        // obtain key with which we will load the saved keySetId.
        String key = toBase64(dash.widevineInitData);

        byte[] keySetId;
        try {
            keySetId = localDataStore.load(key);
        } catch (FileNotFoundException e) {
            throw new LocalAssetsManager.RegisterException("Can't unregister -- keySetId not found", e);
        }

        FrameworkMediaDrm mediaDrm = createMediaDrm();
        FrameworkMediaDrm.KeyRequest releaseRequest;
        try {
            releaseRequest = mediaDrm.getKeyRequest(keySetId, null, MediaDrm.KEY_TYPE_RELEASE, null);
        } catch (Exception e) {
            throw new WidevineNotSupportedException(e);
        }

        log.d("releaseRequest:" + toBase64(releaseRequest.getData()));

        localDataStore.remove(key);

        return true;
    }

    @Override
    public boolean refreshAsset(String localAssetPath, String assetId, String licenseUri, PKRequestParams.Adapter adapter, LocalAssetsManager.AssetRegistrationListener listener) {
        // TODO -- verify that we just need to register again
        return registerAsset(localAssetPath, assetId, licenseUri, adapter, listener);
    }

    @Override
    public boolean checkAssetStatus(String localAssetPath, String assetId, LocalAssetsManager.AssetStatusListener listener) {

        try {
            Map<String, String> assetStatus = checkAssetStatus(localAssetPath, assetId);
            if (assetStatus != null) {
                long licenseDurationRemaining = 0;
                long playbackDurationRemaining = 0;
                try {
                    licenseDurationRemaining = Long.parseLong(assetStatus.get("LicenseDurationRemaining"));
                    playbackDurationRemaining = Long.parseLong(assetStatus.get("PlaybackDurationRemaining"));
                } catch (NumberFormatException e) {
                    log.e("Invalid integers in KeyStatus: " + assetStatus);
                }
                if (listener != null) {
                    listener.onStatus(localAssetPath, licenseDurationRemaining, playbackDurationRemaining, true);
                }
            } else {
                //if assetStatus was null, that means that there is no drm protection for asset,
                //but still it is count as registered.
                if (listener != null) {
                    listener.onStatus(localAssetPath, 0, 0, true);
                }
            }
        } catch (NoWidevinePSSHException e) {
            // Not a Widevine file
            if (listener != null) {
                listener.onStatus(localAssetPath, -1, -1, false);
            }
            return false;
        } catch (LocalAssetsManager.RegisterException e) {
            if (listener != null) {
                listener.onStatus(localAssetPath, 0, 0, false);
            }
            return false;
        } catch (IllegalStateException e) { // FEM-1986 in case MediaDrm.MediaDrmStateException is thrown (wrong date on device for example) need to catch this exception and inform that status is expired
            if (listener != null) {
                listener.onStatus(localAssetPath, 0, 0, true);
            }
            log.e("DRM State Error", e);
            return false;
        }
        return true;
    }

    private Map<String, String> checkAssetStatus(String localAssetPath, String assetId) throws LocalAssetsManager.RegisterException {
        SimpleDashParser dash = parseDash(localAssetPath, assetId);

        //no content protection, so there could not be any status info, so return null.
        if (!dash.hasContentProtection) {
            return null;
        }

        if (dash.widevineInitData == null) {
            throw new NoWidevinePSSHException("No Widevine PSSH in media", null);
        }

        return checkAssetStatus(dash.widevineInitData);
    }

    public Map<String, String> checkAssetStatus(byte[] widevineInitData) throws LocalAssetsManager.RegisterException {
        FrameworkMediaDrm mediaDrm = createMediaDrm();

        MediaDrmSession session;
        try {
            String key = toBase64(widevineInitData);
            session = openSessionWithKeys(mediaDrm, key);
        } catch (Exception e) {
            throw new LocalAssetsManager.RegisterException("Can't open session with keys", e);
        }


        Map<String, String> keyStatus = session.queryKeyStatus();
        log.d("keyStatus: " + keyStatus);

        session.close();
        mediaDrm.release();

        return keyStatus;
    }

    @NonNull
    private FrameworkMediaDrm createMediaDrm() throws LocalAssetsManager.RegisterException {
        FrameworkMediaDrm mediaDrm = null;
        try {
            mediaDrm = FrameworkMediaDrm.newInstance(MediaSupport.WIDEVINE_UUID);
        } catch (UnsupportedDrmException e) {
            e.printStackTrace();
        }

        if (mediaDrm == null) {
            throw new LocalAssetsManager.RegisterException("Could not create MediaDrm instance ", null);
        }

        return mediaDrm;
    }

    /**
     * Parse the dash manifest for the specified file.
     *
     * @param localPath - file from which to parse the dash manifest.
     * @param assetId   - the asset id.
     * @return - {@link SimpleDashParser} which contains the manifest data we need.
     * @throws LocalAssetsManager.RegisterException - {@link LocalAssetsManager.RegisterException}
     */
    private SimpleDashParser parseDash(String localPath, String assetId) throws LocalAssetsManager.RegisterException {
        SimpleDashParser dashParser;
        try {
            dashParser = new SimpleDashParser().parse(localPath);
            if (dashParser.format == null) {
                throw new LocalAssetsManager.RegisterException("Unknown format", null);
            }
            if (dashParser.hasContentProtection && dashParser.widevineInitData == null) {
                throw new NoWidevinePSSHException("No Widevine PSSH in media", null);
            }
        } catch (IOException e) {
            throw new LocalAssetsManager.RegisterException("Can't parse local dash", e);
        }

        return dashParser;
    }

    private MediaDrmSession openSessionWithKeys(FrameworkMediaDrm mediaDrm, String key) throws MediaDrmException, MediaCryptoException, FileNotFoundException {

        byte[] keySetId = localDataStore.load(key);

        MediaDrmSession session = MediaDrmSession.open(mediaDrm);
        session.restoreKeys(keySetId);

        return session;
    }

    private byte[] executeKeyRequest(String licenseUrl, ExoMediaDrm.KeyRequest keyRequest, PKRequestParams.Adapter adapter) throws Exception {



        HttpMediaDrmCallback httpMediaDrmCallback = new HttpMediaDrmCallback(licenseUrl, buildDataSourceFactory());
        if (adapter != null) {
            PKRequestParams params = new PKRequestParams(Uri.parse(licenseUrl), new HashMap<>());
            params = adapter.adapt(params);
            if (params != null && params.headers != null) {
                for (Map.Entry<String, String> entry : params.headers.entrySet()) {
                    if (entry != null) {
                        httpMediaDrmCallback.setKeyRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return httpMediaDrmCallback.executeKeyRequest(MediaSupport.WIDEVINE_UUID, keyRequest);
    }

    private HttpDataSource.Factory buildDataSourceFactory() {
        return new DefaultHttpDataSourceFactory(getUserAgent(context), null);
    }

    private static String getUserAgent(Context context) {
        String applicationName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            applicationName = packageName + "/" + info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            applicationName = "?";
        }

        String sdkName = "PlayKit/" + BuildConfig.VERSION_NAME;

        return sdkName + " " + applicationName + " (Linux;Android " + Build.VERSION.RELEASE
                + ") " + "ExoPlayerLib/" + ExoPlayerLibraryInfo.VERSION;
    }

    private class NoWidevinePSSHException extends LocalAssetsManager.RegisterException {
        NoWidevinePSSHException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
}
