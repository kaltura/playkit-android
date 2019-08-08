package com.kaltura.playkit;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kaltura.playkit.drm.DrmAdapter;
import com.kaltura.playkit.player.MediaSupport;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class LocalAssetsManagerImp extends LocalAssetsManager {

    private final LocalAssetsManagerHelper helper;

    /**
     * Constructor which will create {@link DefaultLocalDataStore}
     *
     * @param context - the application context.
     */
    public LocalAssetsManagerImp(Context context) {
        this(context, new DefaultLocalDataStore(context));
    }

    /**
     * Constructor with custom implementation of the {@link LocalDataStore}
     *
     * @param context        - the application context.
     * @param localDataStore - custom implementation of {@link LocalDataStore}
     */
    public LocalAssetsManagerImp(Context context, LocalDataStore localDataStore) {
        helper = new LocalAssetsManagerHelper(context, localDataStore);
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

    @Override
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
        doInBackground(() -> {
            try {
                DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(drmParams.getScheme(), helper.context, helper.localDataStore);
                String licenseUri = drmParams.getLicenseUri();

                boolean isRegistered = drmAdapter.registerAsset(localAssetPath, assetId, licenseUri, helper.licenseRequestParamAdapter, listener);
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

    @Override
    public void unregisterAsset(@NonNull final String localAssetPath,
                                @NonNull final String assetId, final AssetRemovalListener listener) {


        PKDrmParams.Scheme scheme = helper.getLocalAssetScheme(assetId);

        if (scheme == null) {
            removeAsset(localAssetPath, assetId, listener);
            return;
        }

        final DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(scheme, helper.context, helper.localDataStore);

        doInBackground(() -> {
            drmAdapter.unregisterAsset(localAssetPath, assetId, localAssetPath1 -> {
                helper.mainHandler.post(() -> {
                    removeAsset(localAssetPath1, assetId, listener);
                });
            });
        });
    }

    @Override
    public void refreshAsset(@NonNull final PKMediaSource mediaSource, @NonNull final String localAssetPath,
                             @NonNull final String assetId, final AssetRegistrationListener listener) {
        registerAsset(mediaSource, localAssetPath, assetId, listener);
    }

    private void removeAsset(String localAssetPath, String assetId, AssetRemovalListener listener) {
        helper.removeAssetKey(assetId);
        listener.onRemoved(localAssetPath);
    }

    @Override
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

        doInBackground(() -> drmAdapter.checkAssetStatus(localAssetPath, assetId, (localAssetPath1, expiryTimeSeconds, availableTimeSeconds, isRegistered) -> {
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

    @Override
    public PKMediaSource getLocalMediaSource(@NonNull final String assetId, @NonNull final String localAssetPath) {
        return new LocalMediaSourceImp(helper.localDataStore, localAssetPath, assetId, helper.getLocalAssetScheme(assetId));
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

    public static class LocalMediaSourceImp extends LocalMediaSource {
        private PKDrmParams.Scheme scheme;
        private LocalDataStore localDataStore;

        /**
         * @param localDataStore - the storage from where drm keySetId is stored.
         * @param localPath      - the local url of the media.
         * @param assetId        - the id of the media.
         * @param scheme
         */
        LocalMediaSourceImp(LocalDataStore localDataStore, String localPath, String assetId, PKDrmParams.Scheme scheme) {
            this.scheme = scheme;
            setId(assetId);
            setUrl(localPath);
            this.localDataStore = localDataStore;
        }

        @Override
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
