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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;

import com.kaltura.android.exoplayer2.source.MediaSource;
import com.kaltura.playkit.drm.DrmAdapter;
import com.kaltura.playkit.drm.WidevineModularAdapter;
import com.kaltura.playkit.player.MediaSupport;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Responsible for managing the local(offline) assets. When offline playback of the
 * media is required, you should first register the media files from the local storage.
 * Note, you must have network connection, while register.
 * Created by anton.afanasiev on 13/12/2016.
 */
public abstract class LocalAssetsManager {

    static final PKLog log = PKLog.get("LocalAssetsManager");

    public abstract void setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter);

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
     * Register the asset. If the asset have drm protection it will store keySetId and {@link PKMediaFormat} in {@link LocalDataStore}
     * If no drm available only {@link PKMediaFormat as byte[]} will be stored.
     *
     * @param mediaSource    - the source to register.
     * @param localAssetPath - the url of the locally stored asset.
     * @param assetId        - the asset id.
     * @param listener       - notify about the success/fail after the completion of the registration process.
     */
    public abstract void registerAsset(@NonNull PKMediaSource mediaSource, @NonNull String localAssetPath,
                                       @NonNull String assetId, AssetRegistrationListener listener);

    /**
     * Unregister asset. If the asset have drm protection it will be removed from {@link LocalDataStore}
     * In any case the {@link PKMediaFormat} will be removed from {@link LocalDataStore}
     *
     * @param localAssetPath - the url of the locally stored asset.
     * @param assetId        - the asset id
     * @param listener       - notify when the asset is removed.
     */
    public abstract void unregisterAsset(@NonNull String localAssetPath,
                                         @NonNull String assetId, AssetRemovalListener listener);

    public abstract void refreshAsset(@NonNull PKMediaSource mediaSource, @NonNull String localAssetPath,
                                      @NonNull String assetId, AssetRegistrationListener listener);


    /**
     * Check the status of the desired asset.
     *
     * @param localAssetPath - the url of the locally stored asset.
     * @param assetId        - the asset id.
     * @param listener       - will pass the result of the status.
     */
    public abstract void checkAssetStatus(@NonNull String localAssetPath, @NonNull String assetId,
                                          @Nullable AssetStatusListener listener);

    /**
     * @param assetId        - the id of the asset.
     * @param localAssetPath - the actual url of the video that should be played.
     * @return - the {@link PKMediaSource} that should be passed to the player.
     */
    public abstract PKMediaSource getLocalMediaSource(@NonNull String assetId, @NonNull String localAssetPath);

    /**
     * The local media source that should be passed to the player
     * when offline(locally stored) media want to be played.
     */
    public static abstract class LocalMediaSource extends PKMediaSource {

        /**
         * @return - the {@link LocalDataStore}
         */
        public abstract LocalDataStore getStorage();

        @Override
        public abstract boolean hasDrmParams();

        @Override
        public abstract List<PKDrmParams> getDrmData();
    }

    public static class LocalExoMediaSource extends LocalAssetsManagerImp.LocalMediaSourceImp {
        private MediaSource exoMediaSource;

        /**
         * @param localDataStore - the storage from where drm keySetId is stored.
         * @param assetId        - the id of the media.
         */
        LocalExoMediaSource(LocalDataStore localDataStore, @NonNull MediaSource exoMediaSource, String assetId) {
            super(localDataStore, null, assetId);

            this.exoMediaSource = exoMediaSource;
        }

        public MediaSource getExoMediaSource() {
            return exoMediaSource;
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

        private AssetStatus(boolean registered, long licenseDuration, long totalDuration, boolean hasContentProtection) {
            this.registered = registered;
            this.licenseDuration = licenseDuration;
            this.totalDuration = totalDuration;
            this.hasContentProtection = hasContentProtection;
        }

        public static AssetStatus fromWidevineMap(Map<String, String> map) {
            long licenseDurationRemaining = 0;
            long playbackDurationRemaining = 0;
            try {
                final String licenseDurationRemainingString = map.get("LicenseDurationRemaining");
                final String playbackDurationRemainingString = map.get("PlaybackDurationRemaining");
                if (playbackDurationRemainingString == null || licenseDurationRemainingString == null) {
                    log.e("Missing keys in KeyStatus: " + map);
                    return withDrm(false, -1, -1);
                }

                licenseDurationRemaining = Long.parseLong(licenseDurationRemainingString);
                playbackDurationRemaining = Long.parseLong(playbackDurationRemainingString);

                return withDrm(true, licenseDurationRemaining, playbackDurationRemaining);

            } catch (NumberFormatException e) {
                log.e("Invalid integers in KeyStatus: " + map);
                return withDrm(false, -1, -1);
            }
        }

        public static AssetStatus clear(boolean registered) {
            return new AssetStatus(registered, 0, 0, false);
        }

        public static AssetStatus withDrm(boolean registered, long licenseDuration, long totalDuration) {
            return new AssetStatus(registered, licenseDuration, totalDuration, true);
        }
    }

}
