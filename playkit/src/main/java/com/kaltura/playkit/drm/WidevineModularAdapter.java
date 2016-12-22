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
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.playkit.BuildConfig;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.LocalDrmStorage;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.player.MediaSupport;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Created by anton.afanasiev on 13/12/2016.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class WidevineModularAdapter extends DrmAdapter {

    private static final PKLog log = PKLog.get("WidevineModularAdapter");

    private Context context;
    private final LocalDrmStorage localDrmStorage;


    WidevineModularAdapter(Context context, LocalDrmStorage localDrmStorage) {
        this.context = context;
        this.localDrmStorage = localDrmStorage;
    }

    @Override
    public boolean registerAsset(String localAssetPath, String assetId, String licenseUri, LocalAssetsManager.AssetRegistrationListener listener) {

        try {
            boolean result = registerAsset(localAssetPath, assetId, licenseUri);
            if (listener != null) {
                listener.onRegistered(localAssetPath);
            }
            return result;
        } catch (RegisterException e) {
            if (listener != null) {
                listener.onFailed(localAssetPath, e);
            }
            return false;
        }
    }

    private boolean registerAsset(String localAssetPath, String assetId, String licenseUri) throws RegisterException {

        // obtain the dash manifest.
        SimpleDashParser dash = parseDash(localAssetPath, assetId);

        if (!dash.hasContentProtection) {
            // Not protected -- nothing to do.
            return true;
        }

        String mimeType = dash.format.containerMimeType;
        byte[] initData = dash.widevineInitData;

        MediaDrmSession session;
        FrameworkMediaDrm mediaDrm = createMediaDrm();
        try {
            session = MediaDrmSession.open(mediaDrm);
        } catch (MediaDrmException e) {
            throw new RegisterException("Can't open session", e);
        }


        // Get keyRequest
        FrameworkMediaDrm.KeyRequest keyRequest = session.getOfflineKeyRequest(initData, mimeType);
        log.d("registerAsset: init data (b64): " + encodeToString(initData));

        byte[] data = keyRequest.getData();
        log.d("registerAsset: request data (b64): " + encodeToString(data));

        // Send request to server
        byte[] keyResponse;
        try {
            keyResponse = executeKeyRequest(licenseUri, keyRequest);
            log.d("registerAsset: response data (b64): " + encodeToString(keyResponse));
        } catch (IOException e) {
            throw new RegisterException("Can't send key request for registration", e);
        }

        // Provide keyResponse
        try {
            byte[] offlineKeyId = session.provideKeyResponse(keyResponse);
            localDrmStorage.save(encodeToString(initData), offlineKeyId);
        } catch (DeniedByServerException e) {
            throw new RegisterException("Request denied by server", e);
        }

        session.close();

        return true;
    }

    @Override
    public boolean unregisterAsset(String localAssetPath, String assetId, LocalAssetsManager.AssetRemovalListener listener) {

        try {
            unregisterAsset(localAssetPath, assetId);
            return true;
        } catch (RegisterException e) {
            log.e("Failed to unregister", e);
            return false;
        } finally {
            if (listener != null) {
                listener.onRemoved(localAssetPath);
            }
        }
    }

    private boolean unregisterAsset(String localAssetPath, String assetId) throws RegisterException {

        SimpleDashParser dash = parseDash(localAssetPath, assetId);
        // obtain key with which we will load the saved keySetId.
        String key = encodeToString(dash.widevineInitData);

        byte[] keySetId;
        try {
            keySetId = localDrmStorage.load(key);
        } catch (FileNotFoundException e) {
            throw new RegisterException("Can't unregister -- keySetId not found", e);
        }

        FrameworkMediaDrm mediaDrm = createMediaDrm();
        FrameworkMediaDrm.KeyRequest releaseRequest;
        try {
            releaseRequest = mediaDrm.getKeyRequest(keySetId, null, null, MediaDrm.KEY_TYPE_RELEASE, null);
        } catch (NotProvisionedException e) {
            throw new WidevineNotSupportedException(e);
        }

        log.d("releaseRequest:" + encodeToString(releaseRequest.getData()));

        localDrmStorage.remove(key);

        return true;
    }

    @Override
    public boolean refreshAsset(String localAssetPath, String assetId, String licenseUri, LocalAssetsManager.AssetRegistrationListener listener) {
        // TODO -- verify that we just need to register again
        return registerAsset(localAssetPath, assetId, licenseUri, listener);
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
                    listener.onStatus(localAssetPath, licenseDurationRemaining, playbackDurationRemaining);
                }
            }
        } catch (NoWidevinePSSHException e) {
            // Not a Widevine file
            if (listener != null) {
                listener.onStatus(localAssetPath, -1, -1);
            }
            return false;
        } catch (RegisterException e) {
            if (listener != null) {
                listener.onStatus(localAssetPath, 0, 0);
            }
            return false;
        }

        return true;
    }

    private Map<String, String> checkAssetStatus(String localAssetPath, String assetId) throws RegisterException {
        SimpleDashParser dash = parseDash(localAssetPath, assetId);
        if (dash.widevineInitData == null) {
            throw new NoWidevinePSSHException("No Widevine PSSH in media", null);
        }

        FrameworkMediaDrm mediaDrm = createMediaDrm();

        MediaDrmSession session;
        try {
            String key = encodeToString(dash.widevineInitData);
            session = openSessionWithKeys(mediaDrm, key);
        } catch (MediaDrmException | FileNotFoundException | MediaCryptoException e) {
            throw new RegisterException("Can't open session with keys", e);
        }


        Map<String, String> keyStatus = session.queryKeyStatus();
        log.d("keyStatus: " + keyStatus);

        session.close();
        mediaDrm.release();

        return keyStatus;
    }

    @NonNull
    private FrameworkMediaDrm createMediaDrm() throws RegisterException {
        FrameworkMediaDrm mediaDrm = null;
        try {
            mediaDrm = FrameworkMediaDrm.newInstance(MediaSupport.WIDEVINE_UUID);
        } catch (UnsupportedDrmException e) {
            e.printStackTrace();
        }

        if(mediaDrm == null){
            throw new RegisterException("Could not create MediaDrm instance ", null);
        }

        return mediaDrm;
    }

    /**
     * Parse the dash manifest for the specified file.
     * @param localPath - file from which to parse the dash manifest.
     * @param assetId - the asset id.
     * @return - {@link SimpleDashParser} which contains the manifest data we need.
     * @throws RegisterException
     */
    private SimpleDashParser parseDash(String localPath, String assetId) throws RegisterException {
        SimpleDashParser dashParser;
        try {
            dashParser = new SimpleDashParser().parse(localPath, assetId);
            if (dashParser.format == null) {
                throw new RegisterException("Unknown format", null);
            }
            if (dashParser.hasContentProtection && dashParser.widevineInitData == null) {
                throw new NoWidevinePSSHException("No Widevine PSSH in media", null);
            }
        } catch (IOException e) {
            throw new RegisterException("Can't parse local dash", e);
        }

        return dashParser;
    }

    private MediaDrmSession openSessionWithKeys(FrameworkMediaDrm mediaDrm, String key) throws MediaDrmException, MediaCryptoException, FileNotFoundException {

        byte[] keySetId = localDrmStorage.load(key);

        MediaDrmSession session = MediaDrmSession.open(mediaDrm);
        session.restoreKeys(keySetId);

        return session;
    }

    private byte[] executeKeyRequest(String licenseUrl, ExoMediaDrm.KeyRequest keyRequest) throws IOException {
        HttpMediaDrmCallback httpMediaDrmCallback = new HttpMediaDrmCallback(licenseUrl, buildDataSourceFactory());
        try {
            return httpMediaDrmCallback.executeKeyRequest(MediaSupport.WIDEVINE_UUID, keyRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DRMScheme getScheme() {
        return DRMScheme.WidevineCENC;
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

    private String encodeToString(byte[] data){
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private class RegisterException extends Exception {
        RegisterException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }

    private class NoWidevinePSSHException extends RegisterException {
        NoWidevinePSSHException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
}
