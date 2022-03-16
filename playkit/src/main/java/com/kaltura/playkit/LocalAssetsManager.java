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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;

import com.kaltura.playkit.drm.DrmAdapter;
import com.kaltura.playkit.player.MediaSupport;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Responsible for managing the local(offline) assets. When offline playback of the
 * media is required, you should first register the media files from the local storage.
 * Note, you must have network connection, while register.
 */
public class LocalAssetsManager {

    private static final PKLog log = PKLog.get("LocalAssetsManager");
    private final LocalAssetsManagerHelper helper;
    private boolean forceWidevineL3Playback = false;

    public LocalAssetsManager(Context context, LocalDataStore localDataStore) {
        helper = new LocalAssetsManagerHelper(context, localDataStore);
    }

    public LocalAssetsManager(Context context) {
        helper = new LocalAssetsManagerHelper(context);
    }

    public void setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter) {
        helper.setLicenseRequestAdapter(licenseRequestAdapter);
    }

    /**
     * If the device codec is known to fail if security level L1 is used
     * then set flag to true, it will force the player to use Widevine L3
     * Will work only SDK level 18 or above
     *
     * @param forceWidevineL3Playback - force the L3 Playback. Default is false
     */
    public void forceWidevineL3Playback(boolean forceWidevineL3Playback) {
        this.forceWidevineL3Playback = forceWidevineL3Playback;
        if (forceWidevineL3Playback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(MediaSupport::provisionWidevineL3);
            }
        }
    }

    /**
     * Will check if passed parameters are valid.
     *
     * @param url            - url of the media.
     * @param localAssetPath - the local asset path of the media.
     * @param assetId        - the asset id.
     */
    private void checkIfParamsAreValid(String url, String localAssetPath, String assetId) {
        checkNotEmpty(url, "mediaSource.url");
        checkNotEmpty(localAssetPath, "localAssetPath");
        checkNotEmpty(assetId, "assetId");
    }

    /**
     * Checking arguments. if the argument is not valid, will throw an {@link IllegalArgumentException}
     *
     * @param invalid - the state of the argument.
     * @param message - message to print, to the console.
     */
    private void checkArg(boolean invalid, String message) {
        if (invalid) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * check the passed String.
     *
     * @param str  - String to check.
     * @param name - the descriptive name of the String.
     */
    private void checkNotEmpty(String str, String name) {
        checkArg(TextUtils.isEmpty(str), name + " must not be empty");
    }

    private void doInBackground(Runnable runnable) {
        AsyncTask.execute(runnable);
    }

    /**
     * Listener that notify about the result when registration flow is ended.
     */
    public interface AssetRegistrationListener {
        /**
         * Will notify about success.
         *
         * @param localAssetPath - the path of the local asset.
         */
        void onRegistered(String localAssetPath);

        /**
         * Will notify if registration process was not successful.
         *
         * @param localAssetPath - the path of the local asset.
         * @param error          - error that occured during the registration process.
         */
        void onFailed(String localAssetPath, Exception error);
    }

    /**
     * Will notify about the status of the requested asset.
     */
    public interface AssetStatusListener {
        void onStatus(String localAssetPath, long expiryTimeSeconds, long availableTimeSeconds, boolean isRegistered);
    }

    /**
     * Will notify when the asset was removed from the manager.
     */
    public interface AssetRemovalListener {
        void onRemoved(String localAssetPath);
    }

    private static PKDrmParams findSupportedDrmParams(@NonNull PKMediaSource mediaSource) {
        if (mediaSource.getDrmData() == null) {
            return null;
        }

        for (PKDrmParams params : mediaSource.getDrmData()) {
            switch (params.getScheme()) {
                case WidevineCENC:
                    if (MediaSupport.widevineModular()) {
                        return params;
                    }
                    break;
                case WidevineClassic:
                    if (MediaSupport.widevineClassic()) {
                        return params;
                    }
                    break;
                case PlayReadyCENC:
                    log.d("Skipping unsupported PlayReady params");
                    break;
            }
        }
        return null;
    }

    public void registerAsset(@NonNull final PKMediaSource mediaSource, @NonNull final String localAssetPath,
                              @NonNull final String assetId, final AssetRegistrationListener listener) {

        checkIfParamsAreValid(mediaSource.getUrl(), localAssetPath, assetId);

        if (!helper.isOnline()) {
            listener.onFailed(localAssetPath, new Exception("Can't register/refresh when offline"));
            return;
        }

        PKDrmParams drmParams = findSupportedDrmParams(mediaSource);

        PKMediaFormat mediaFormat = mediaSource.getMediaFormat();
        if (mediaFormat == null) {
            listener.onFailed(localAssetPath,
                    new IllegalArgumentException("Can not register media, when PKMediaFormat and url of PKMediaSource not exist."));
        }

        if (drmParams != null) {
            registerDrmAsset(localAssetPath, assetId, mediaFormat, drmParams, forceWidevineL3Playback, listener);
        } else {
            registerClearAsset(localAssetPath, assetId, mediaFormat, listener);
        }
    }

    /**
     * Will register the drm asset and store the keyset id and {@link PKMediaFormat} in local storage.
     *
     * @param localAssetPath                - the local asset path of the asset.
     * @param assetId                       - the asset id.
     * @param mediaFormat                   - the media format converted to byte[].
     * @param drmParams                     - drm params of the media.
     * @param forceWidevineL3Playback     - if the device codec is known to fail if security level L1 is used then set flag to true, it will force the player to use Widevine L3
     * @param listener                      - notify about the success/fail after the completion of the registration process.
     */
    private void registerDrmAsset(final String localAssetPath, final String assetId, final PKMediaFormat mediaFormat, final PKDrmParams drmParams, boolean forceWidevineL3Playback, final AssetRegistrationListener listener) {
        doInBackground(() -> {
            try {
                DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(drmParams.getScheme(), helper.context, helper.localDataStore);
                String licenseUri = drmParams.getLicenseUri();

                boolean isRegistered = drmAdapter.registerAsset(localAssetPath, assetId, licenseUri, helper.licenseRequestParamAdapter, forceWidevineL3Playback, listener);
                if (isRegistered) {
                    helper.saveMediaFormat(assetId, mediaFormat, drmParams.getScheme());
                }
            } catch (final IOException e) {
                log.e("Error", e);
                if (listener != null) {
                    helper.mainHandler.post(() -> listener.onFailed(localAssetPath, e));
                }
            }
        });
    }

    /**
     * Will register clear asset and store {@link PKMediaFormat} in local storage.
     *
     * @param localAssetPath - the local asset path of the asset.
     * @param assetId        - the asset id.
     * @param mediaFormat    - the media format converted to byte[].
     * @param listener       - notify about the success/fail after the completion of the registration process.
     */
    private void registerClearAsset(String localAssetPath, String assetId, PKMediaFormat mediaFormat, AssetRegistrationListener listener) {
        helper.saveMediaFormat(assetId, mediaFormat, null);
        listener.onRegistered(localAssetPath);
    }

    public void unregisterAsset(@NonNull final String localAssetPath,
                                @NonNull final String assetId, final AssetRemovalListener listener) {
        PKDrmParams.Scheme scheme = helper.getLocalAssetScheme(assetId);

        if (scheme == null) {
            removeAsset(localAssetPath, assetId, listener);
            return;
        }

        final DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(scheme, helper.context, helper.localDataStore);

        doInBackground(() -> {
            drmAdapter.unregisterAsset(localAssetPath, assetId, forceWidevineL3Playback, localAssetPath1 -> {
                helper.mainHandler.post(() -> {
                    removeAsset(localAssetPath1, assetId, listener);
                });
            });
        });
    }

    public void refreshAsset(@NonNull final PKMediaSource mediaSource, @NonNull final String localAssetPath,
                             @NonNull final String assetId, final AssetRegistrationListener listener) {
        registerAsset(mediaSource, localAssetPath, assetId, listener);
    }


    private void removeAsset(String localAssetPath, String assetId, AssetRemovalListener listener) {
        helper.removeAssetKey(assetId);
        listener.onRemoved(localAssetPath);
    }

    public void checkAssetStatus(@NonNull final String localAssetPath, @NonNull final String assetId,
                                 @Nullable final AssetStatusListener listener) {

        PKDrmParams.Scheme scheme = helper.getLocalAssetScheme(assetId);
        if (scheme == null) {
            checkClearAssetStatus(localAssetPath, assetId, listener);
        } else {
            checkDrmAssetStatus(localAssetPath, assetId, scheme, listener);
        }
    }

    private void checkDrmAssetStatus(final String localAssetPath, final String assetId, PKDrmParams.Scheme scheme, final AssetStatusListener listener) {

        final DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(scheme, helper.context, helper.localDataStore);

        doInBackground(() -> drmAdapter.checkAssetStatus(localAssetPath, assetId, forceWidevineL3Playback, (localAssetPath1, expiryTimeSeconds, availableTimeSeconds, isRegistered) -> {
            if (listener != null) {
                helper.mainHandler.post(() ->  {
                    listener.onStatus(localAssetPath1, expiryTimeSeconds, availableTimeSeconds, isRegistered);
                });
            }
        }));
    }

    private void checkClearAssetStatus(String localAssetPath, String assetId, AssetStatusListener listener) {
        try {
            helper.localDataStore.load(LocalAssetsManagerHelper.buildAssetKey(assetId));
            listener.onStatus(localAssetPath, Long.MAX_VALUE, Long.MAX_VALUE, true);
        } catch (FileNotFoundException e) {
            listener.onStatus(localAssetPath, 0, 0, false);
        }
    }

    public PKMediaSource getLocalMediaSource(@NonNull final String assetId, @NonNull final String localAssetPath) {
        return new LocalMediaSource(helper.localDataStore, localAssetPath, assetId, helper.getLocalAssetScheme(assetId));
    }

    /**
     * Default implementation of the {@link LocalDataStore}. Actually doing the basic save/load/remove actions
     * to the {@link SharedPreferences}.
     * Created by anton.afanasiev on 13/12/2016.
     */

    public static class DefaultLocalDataStore implements LocalDataStore {

        private final PKLog log = PKLog.get("DefaultLocalDataStore");

        private static final String LOCAL_SHARED_PREFERENCE_STORAGE = "PlayKitLocalStorage";
        private final SharedPreferences sharedPreferences;

        public DefaultLocalDataStore(Context context) {

            log.d("context: " + context);
            sharedPreferences = context.getSharedPreferences(LOCAL_SHARED_PREFERENCE_STORAGE, 0);
        }

        @Override
        public void save(String key, byte[] value) {
            String encodedValue = Utils.toBase64(value);
            log.i("save to storage with key " + key + " and value " + encodedValue);
            sharedPreferences.edit()
                    .putString(key, encodedValue)
                    .apply();
        }

        @Override
        public byte[] load(String key) throws FileNotFoundException {

            String value = sharedPreferences.getString(key, null);
            log.i("load from storage with key " + key);
            if (value == null) {
                throw new FileNotFoundException("Key not found in the storage " + key);
            }

            return Base64.decode(value, Base64.NO_WRAP);
        }

        @Override
        public void remove(String key) {
            log.i("remove from storage with key " + key);
            sharedPreferences.edit()
                    .remove(key)
                    .apply();
        }
    }

    public static class AssetStatus {
        public final boolean registered;
        public final long licenseDuration;
        public final long totalDuration;
        public final boolean hasContentProtection;

        static AssetStatus invalid = new AssetStatus(false, -1, -1, false);

        private AssetStatus(boolean registered, long licenseDuration, long totalDuration, boolean hasContentProtection) {
            this.registered = registered;
            this.licenseDuration = licenseDuration;
            this.totalDuration = totalDuration;
            this.hasContentProtection = hasContentProtection;
        }

        public static AssetStatus clear(boolean registered) {
            return new AssetStatus(registered, 0, 0, false);
        }

        public static AssetStatus withDrm(boolean registered, long licenseDuration, long totalDuration) {
            return new AssetStatus(registered, licenseDuration, totalDuration, true);
        }
    }

    public static class LocalMediaSource extends PKMediaSource {
        private PKDrmParams.Scheme scheme;
        private LocalDataStore localDataStore;

        /**
         * @param localDataStore - the storage from where drm keySetId is stored.
         * @param localPath      - the local url of the media.
         * @param assetId        - the id of the media.
         * @param scheme
         */
        public LocalMediaSource(LocalDataStore localDataStore, String localPath, String assetId, PKDrmParams.Scheme scheme) {
            this.scheme = scheme;
            setId(assetId);
            setUrl(localPath);
            this.localDataStore = localDataStore;
        }

        public LocalDataStore getStorage() {
            return localDataStore;
        }

        @Override
        public boolean hasDrmParams() {
            return this.scheme != null;
        }

        @Override
        public List<PKDrmParams> getDrmData() {
            return Collections.emptyList();
        }
    }

    public static class RegisterException extends Exception {
        public RegisterException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
}
