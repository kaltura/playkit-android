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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.kaltura.playkit.drm.DrmAdapter;
import com.kaltura.playkit.player.MediaSupport;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Responsible for managing the local(offline) assets. When offline playback of the
 * media is required, you should first register the media files from the local storage.
 * Note, you must have network connection, while register.
 * Created by anton.afanasiev on 13/12/2016.
 */
public class LocalAssetsManager {

    private static final PKLog log = PKLog.get("LocalAssetsManager");
    private static final String ASSET_ID_PREFIX = "assetId:";

    private final Context context;
    private LocalDataStore localDataStore;

    private Handler mainHandler = new Handler(Looper.getMainLooper());

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

    /**
     * Constructor which will create {@link DefaultLocalDataStore}
     *
     * @param context - the application context.
     */
    public LocalAssetsManager(Context context) {
        this(context, new DefaultLocalDataStore(context));
    }

    /**
     * Constructor with custom implementation of the {@link LocalDataStore}
     *
     * @param context        - the application context.
     * @param localDataStore - custom implementation of {@link LocalDataStore}
     */
    public LocalAssetsManager(Context context, LocalDataStore localDataStore) {
        this.context = context;
        this.localDataStore = localDataStore;

        MediaSupport.initialize(context);
    }

    /**
     * Register the asset. If the asset have drm protection it will store keySetId and {@link PKMediaFormat} in {@link LocalDataStore}
     * If no drm available only {@link PKMediaFormat as byte[]} will be stored.
     *
     * @param mediaSource    - the source to register.
     * @param localAssetPath - the url of the locally stored asset.
     * @param assetId        - the asset id.
     * @param listener       - notify about the success/fail after the completion of the registration process.
     */
    public void registerAsset(@NonNull final PKMediaSource mediaSource, @NonNull final String localAssetPath,
                              @NonNull final String assetId, final AssetRegistrationListener listener) {

        checkIfParamsAreValid(mediaSource.getUrl(), localAssetPath, assetId);

        if (!isOnline(context)) {
            listener.onFailed(localAssetPath, new Exception("Can't register/refresh when offline"));
            return;
        }

        PKDrmParams drmParams = findSupportedDrmParams(mediaSource);

        PKMediaFormat mediaFormat = mediaSource.getMediaFormat();
        if(mediaFormat == null){
            listener.onFailed(localAssetPath,
                    new IllegalArgumentException("Can not register media, when PKMediaFormat and url of PKMediaSource not exist."));
        }

        if (drmParams != null) {
            registerDrmAsset(localAssetPath, assetId, mediaFormat, drmParams, listener);
        } else {
            registerClearAsset(localAssetPath, assetId, mediaFormat, listener);
        }
    }

    /**
     * Will register the drm asset and store the keyset id and {@link PKMediaFormat} in local storage.
     *
     * @param localAssetPath - the local asset path of the asset.
     * @param assetId        - the asset id.
     * @param mediaFormat    - the media format converted to byte[].
     * @param drmParams      - drm params of the media.
     * @param listener       - notify about the success/fail after the completion of the registration process.
     */
    private void registerDrmAsset(final String localAssetPath, final String assetId, final PKMediaFormat mediaFormat, final PKDrmParams drmParams, final AssetRegistrationListener listener) {
        doInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(drmParams.getScheme(), context, localDataStore);
                    String licenseUri = drmParams.getLicenseUri();

                    boolean isRegistered = drmAdapter.registerAsset(localAssetPath, assetId, licenseUri, listener);
                    if (isRegistered) {
                        localDataStore.save(buildAssetKey(assetId), buildMediaFormatValueAsByteArray(mediaFormat, drmParams.getScheme()));
                    }
                } catch (final IOException e) {
                    log.e("Error", e);
                    if (listener != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onFailed(localAssetPath, e);
                            }
                        });
                    }
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
        localDataStore.save(buildAssetKey(assetId), buildMediaFormatValueAsByteArray(mediaFormat, null));
        listener.onRegistered(localAssetPath);
    }

    /**
     * Unregister asset. If the asset have drm protection it will be removed from {@link LocalDataStore}
     * In any case the {@link PKMediaFormat} will be removed from {@link LocalDataStore}
     *
     * @param localAssetPath - the url of the locally stored asset.
     * @param assetId        - the asset id
     * @param listener       - notify when the asset is removed.
     */
    public void unregisterAsset(@NonNull final String localAssetPath,
                                @NonNull final String assetId, final AssetRemovalListener listener) {


        PKDrmParams.Scheme scheme = getLocalAssetScheme(assetId);

        if (scheme == null) {
            removeAsset(localAssetPath, assetId, listener);
            return;
        }

        final DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(scheme, context, localDataStore);

        doInBackground(new Runnable() {
            @Override
            public void run() {
                drmAdapter.unregisterAsset(localAssetPath, assetId, new AssetRemovalListener() {
                    @Override
                    public void onRemoved(final String localAssetPath) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                removeAsset(localAssetPath, assetId, listener);
                            }
                        });
                    }
                });
            }
        });
    }

    private void removeAsset(String localAssetPath, String assetId, AssetRemovalListener listener) {
        localDataStore.remove(buildAssetKey(assetId));
        listener.onRemoved(localAssetPath);
    }


    /**
     * Check the status of the desired asset.
     *
     * @param localAssetPath - the url of the locally stored asset.
     * @param assetId        - the asset id.
     * @param listener       - will pass the result of the status.
     */
    public void checkAssetStatus(@NonNull final String localAssetPath, @NonNull final String assetId,
                                 @Nullable final AssetStatusListener listener) {

        PKDrmParams.Scheme scheme = getLocalAssetScheme(assetId);
        if (scheme == null) {
            checkClearAssetStatus(localAssetPath, assetId, listener);
        } else {
            checkDrmAssetStatus(localAssetPath, assetId, scheme, listener);
        }
    }

    private void checkDrmAssetStatus(final String localAssetPath, final String assetId, PKDrmParams.Scheme scheme, final AssetStatusListener listener) {

        final DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(scheme, context, localDataStore);

        doInBackground(new Runnable() {
            @Override
            public void run() {
                drmAdapter.checkAssetStatus(localAssetPath, assetId, new AssetStatusListener() {
                    @Override
                    public void onStatus(final String localAssetPath, final long expiryTimeSeconds, final long availableTimeSeconds, final boolean isRegistered) {
                        if (listener != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onStatus(localAssetPath, expiryTimeSeconds, availableTimeSeconds, isRegistered);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void checkClearAssetStatus(String localAssetPath, String assetId, AssetStatusListener listener) {
        try {
            localDataStore.load(buildAssetKey(assetId));
            listener.onStatus(localAssetPath, 0, 0, true);
        } catch (FileNotFoundException e) {
            listener.onStatus(localAssetPath, 0, 0, false);
        }
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

    @Nullable
    private PKDrmParams.Scheme getLocalAssetScheme(@NonNull String assetId) {
        try {
            String mediaFormatValue = new String(localDataStore.load(buildAssetKey(assetId)));
            String[] splitFormatValue = mediaFormatValue.split(":");
            String schemeName = splitFormatValue[1];
            if(schemeName.equals("null")) {
                return null;
            }

            return PKDrmParams.Scheme.valueOf(schemeName);

        } catch (FileNotFoundException e) {
            log.e(e.getMessage());
            return null;
        }
    }

    /**
     * @param assetId        - the id of the asset.
     * @param localAssetPath - the actual url of the video that should be played.
     * @return - the {@link PKMediaSource} that should be passed to the player.
     */
    public PKMediaSource getLocalMediaSource(@NonNull final String assetId, @NonNull final String localAssetPath) {
        return new LocalMediaSource(localDataStore, localAssetPath, assetId);
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
     * @param obj  - String to check.
     * @param name - the descriptive name of the String.
     */
    private void checkNotEmpty(String obj, String name) {
        checkArg(obj == null || obj.length() == 0, name + " must not be empty");
    }

    private void doInBackground(Runnable runnable) {
        new Thread(runnable).start();
    }

    /**
     * Check if network connection is available.
     *
     * @param context - context.
     * @return - true if there is network connection, otherwise - false.
     */
    private boolean isOnline(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        return !(netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable());
    }

    private String buildAssetKey(String assetId) {
        return ASSET_ID_PREFIX + assetId;
    }

    private byte[] buildMediaFormatValueAsByteArray(PKMediaFormat mediaFormat, PKDrmParams.Scheme scheme) {
        String mediaFormatName = mediaFormat.toString();
        String schemeName = "null";
        if(scheme != null) {
            schemeName = scheme.toString();
        }
        String stringBuilder = mediaFormatName +
                ":" +
                schemeName;
        return stringBuilder.getBytes();
    }

    /**
     * The local media source that should be passed to the player
     * when offline(locally stored) media want to be played.
     * Created by anton.afanasiev on 18/12/2016.
     */
    public static class LocalMediaSource extends PKMediaSource {

        private LocalDataStore localDataStore;

        /**
         * @param localDataStore - the storage from where drm keySetId is stored.
         * @param localPath      - the local url of the media.
         * @param assetId        - the id of the media.
         */
        LocalMediaSource(LocalDataStore localDataStore, String localPath, String assetId) {
            setId(assetId);
            setUrl(localPath);
            this.localDataStore = localDataStore;
        }

        /**
         * @return - the {@link LocalDataStore}
         */
        public LocalDataStore getStorage() {
            return localDataStore;
        }

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

}
