package com.kaltura.playkit.offline;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Created by anton.afanasiev on 13/12/2016.
 */

public abstract class DrmAdapter {

    @NonNull
    public static DrmAdapter getDrmAdapter(@NonNull final Context context, @NonNull OfflineStorage offlineStorage, @NonNull final String localAssetPath) {
        if (localAssetPath.endsWith(".wvm")) {
            return new WidevineClassicAdapter(context);
        }

        if (localAssetPath.endsWith(".mpd")) {
            return new WidevineModularAdapter(context, offlineStorage);
        }

        return new NullDrmAdapter();
    }

    public abstract boolean registerAsset(@NonNull final String localAssetPath, String licenseUri, @Nullable final LocalAssetsManager.AssetRegistrationListener listener) throws IOException;

    public abstract boolean refreshAsset(@NonNull final String localAssetPath, String licenseUri, @Nullable final LocalAssetsManager.AssetRegistrationListener listener);

    public abstract boolean unregisterAsset(@NonNull final String localAssetPath, final LocalAssetsManager.AssetRemovalListener listener);

    public abstract boolean checkAssetStatus(@NonNull String localAssetPath, @Nullable final LocalAssetsManager.AssetStatusListener listener);

    public abstract DRMScheme getScheme();

    public enum DRMScheme {
        Null, WidevineClassic, WidevineCENC
    }

    static class NullDrmAdapter extends DrmAdapter {
        @Override
        public boolean checkAssetStatus(@NonNull String localAssetPath, @Nullable LocalAssetsManager.AssetStatusListener listener) {
            if (listener != null) {
                listener.onStatus(localAssetPath, -1, -1);
            }
            return true;
        }

        @Override
        public DRMScheme getScheme() {
            return DRMScheme.Null;
        }

        @Override
        public boolean registerAsset(@NonNull String localAssetPath, @NonNull String licenseUri, @Nullable LocalAssetsManager.AssetRegistrationListener listener) {
            if (listener != null) {
                listener.onRegistered(localAssetPath);
            }
            return true;
        }

        @Override
        public boolean refreshAsset(@NonNull String localAssetPath, @NonNull String licenseUri, @Nullable LocalAssetsManager.AssetRegistrationListener listener) {
            return registerAsset(localAssetPath, licenseUri, listener);
        }

        @Override
        public boolean unregisterAsset(@NonNull String localAssetPath, LocalAssetsManager.AssetRemovalListener listener) {
            if (listener != null) {
                listener.onRemoved(localAssetPath);
            }
            return true;
        }
    }
}
