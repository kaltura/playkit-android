package com.kaltura.playkit;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

    private final Context context;
    private LocalDataStore localDataStore;

    /**
     * Listener that notify about the result when registration flow is ended.
     */
    public interface AssetRegistrationListener {
        /**
         * Will notify about success.
         * @param localAssetPath - the path of the local asset.
         */
        void onRegistered(String localAssetPath);

        /**
         * Will notify if registration process was not successful.
         * @param localAssetPath - the path of the local asset.
         * @param error - error that occured during the registration process.
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
     * @param context - the application context.
     */
    public LocalAssetsManager(Context context) {
        this.context = context;
        this.localDataStore = new DefaultLocalDataStore(context);
    }

    /**
     * Constructor with custom implementation of the {@link LocalDataStore}
     * @param context - the application context.
     * @param localDataStore - custom implementation of {@link LocalDataStore}
     */
    public LocalAssetsManager(Context context, LocalDataStore localDataStore) {
        this.context = context;
        this.localDataStore = localDataStore;
    }


    /**
     * Register the asset. If the asset have drm protection it will store its keySetId in {@link LocalDataStore}
     * @param mediaSource - the source to register.
     * @param localAssetPath - the url of the locally stored asset.
     * @param assetId - the asset id.
     * @param listener - notify about the success/fail after the completion of the registration process.
     */
    public void registerAsset(@NonNull final PKMediaSource mediaSource, @NonNull final String localAssetPath,
                                 @NonNull final String assetId, final AssetRegistrationListener listener) {

        // Preflight: check that all parameters are valid.
        checkNotEmpty(mediaSource.getUrl(), "mediaSource.url");
        checkNotEmpty(localAssetPath, "localAssetPath");
        checkNotEmpty(assetId, "assetId");

        if (!isOnline(context)) {
            //TODO check with noam if it is alright to return an exception at this stage.
            listener.onFailed(localAssetPath, new Exception("Can't register/refresh when offline"));
            return;
        }

        final PKDrmParams drmParams = findSupportedDrmParams(mediaSource);
        
        if (drmParams != null) {
            doInBackground(new Runnable() {
                @Override
                public void run() {
                    try {
                        DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(drmParams.getScheme(), context, localDataStore);
                        String licenseUri = drmParams.getLicenseUri();
                        drmAdapter.registerAsset(localAssetPath, assetId, licenseUri, listener);

                    } catch (IOException e) {
                        log.e("Error", e);
                        if (listener != null) {
                            listener.onFailed(localAssetPath, e);
                        }
                    }
                }
            });
        }
    }

    private static PKDrmParams findSupportedDrmParams(@NonNull PKMediaSource mediaSource) {
        for (PKDrmParams params : mediaSource.getDrmData()) {
            switch (params.getScheme()) {
                case widevine_cenc:
                    if (MediaSupport.widevineModular()) {
                        return params;
                    }
                    break;
                case widevine_classic:
                    if (MediaSupport.widevineClassic(null)) {
                        return params;
                    }
                    break;
            }
        }
        return null;
    }

    /**
     * Unregister asset. If the asset have drm protection it will be removed from {@link LocalDataStore}
     * @param localAssetPath - the url of the locally stored asset.
     * @param assetId - the asset id
     * @param listener - notify when the asset is removed.
     */
    public void unregisterAsset(@NonNull final String localAssetPath,
                                   @NonNull final String assetId, final AssetRemovalListener listener) {

        // FIXME: what is the scheme?
        final DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(null, context, localDataStore);

        doInBackground(new Runnable() {
            @Override
            public void run() {
                drmAdapter.unregisterAsset(localAssetPath, assetId, listener);
            }
        });
    }

    /**
     * Chek the status of the desired asset.
     * @param localAssetPath - the url of the locally stored asset.
     * @param assetId - the asset id.
     * @param listener - will pass the result of the status.
     */
    public void checkAssetStatus(@NonNull final String localAssetPath, @NonNull final String assetId,
                                           @Nullable final AssetStatusListener listener) {

        // FIXME: what is the scheme?
        final DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(null, context, localDataStore);

        doInBackground(new Runnable() {
            @Override
            public void run() {
                drmAdapter.checkAssetStatus(localAssetPath, assetId, listener);
            }
        });
    }

    /**
     *
     * @param assetId - the id of the asset.
     * @param localAssetPath - the actual url of the video that should be played.
     * @return - the {@link PKMediaSource} that should be passed to the player.
     */
    public PKMediaSource getLocalMediaSource(@NonNull final String assetId, @NonNull final String localAssetPath) {
        return new LocalMediaSource(localDataStore, localAssetPath, assetId);
    }

    /**
     * Checking arguments. if the argument is not valid, will throw an {@link IllegalArgumentException}
     * @param invalid - the state of the argument.
     * @param message - message to print, to the console.
     */
    private void checkArg(boolean invalid, String message) {
        if (invalid) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * check the passed object for null.
     * @param obj - object to check.
     * @param name - the descriptive name of the object.
     */
    private void checkNotNull(Object obj, String name) {
        checkArg(obj == null, name + " must not be null");
    }

    /**
     * check the passed String.
     * @param obj - String to check.
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
     * @param context - context.
     * @return - true if there is network connection, otherwise - false.
     */
    private boolean isOnline(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        return !(netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable());
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
         * @param localPath - the local url of the media.
         * @param assetId - the id of the media.
         */
        public LocalMediaSource(LocalDataStore localDataStore, String localPath, String assetId) {
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

        private static final String LOCAL_DRM_SHARED_PREFERENCE_STORAGE = "PlayKitLocalDrmStorage";
        private final SharedPreferences sharedPreferences;

        public DefaultLocalDataStore(Context context){
            sharedPreferences = context.getSharedPreferences(LOCAL_DRM_SHARED_PREFERENCE_STORAGE, 0);
        }

        @Override
        public void save(String key, byte[] value) {
            String encodedValue = Base64.encodeToString(value, Base64.NO_WRAP);
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
