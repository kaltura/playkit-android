package com.kaltura.playkit.drm;

import android.content.Context;
import android.support.annotation.NonNull;

import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.LocalDrmStorage;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;

import java.io.IOException;

/**
 * Created by anton.afanasiev on 13/12/2016.
 */

public abstract class DrmAdapter {
    
    private static final PKLog log = PKLog.get("DrmAdapter");
    

    @NonNull
    public static DrmAdapter getDrmAdapter(PKDrmParams.Scheme scheme, Context context, LocalDrmStorage localDrmStorage) {

        if (scheme == null) {
            return new NullDrmAdapter();
        }
        
        switch (scheme) {
            case widevine_cenc:
                return new WidevineModularAdapter(context, localDrmStorage);
            case widevine_classic:
                return new WidevineClassicAdapter(context);
        }

        return new NullDrmAdapter();
    }

    public abstract boolean registerAsset(final String localAssetPath, final String assetId,final String licenseUri, final LocalAssetsManager.AssetRegistrationListener listener) throws IOException;

    public abstract boolean refreshAsset(final String localAssetPath, final String assetId, final String licenseUri, final LocalAssetsManager.AssetRegistrationListener listener);

    public abstract boolean unregisterAsset(final String localAssetPath, final String assetId, final LocalAssetsManager.AssetRemovalListener listener);

    public abstract boolean checkAssetStatus(final String localAssetPath, final String assetId, final LocalAssetsManager.AssetStatusListener listener);

    private static class NullDrmAdapter extends DrmAdapter {
        @Override
        public boolean checkAssetStatus(String localAssetPath, String assetId, LocalAssetsManager.AssetStatusListener listener) {
            if (listener != null) {
                listener.onStatus(localAssetPath, -1, -1, false);
            }
            return true;
        }

        @Override
        public boolean registerAsset(String localAssetPath, String assetId, String licenseUri, LocalAssetsManager.AssetRegistrationListener listener) {
            if (listener != null) {
                listener.onRegistered(localAssetPath);
            }
            return true;
        }

        @Override
        public boolean refreshAsset(String localAssetPath, String assetId, String licenseUri, LocalAssetsManager.AssetRegistrationListener listener) {
            return registerAsset(localAssetPath, assetId, licenseUri, listener);
        }

        @Override
        public boolean unregisterAsset(String localAssetPath, String assetId, LocalAssetsManager.AssetRemovalListener listener) {
            if (listener != null) {
                listener.onRemoved(localAssetPath);
            }
            return true;
        }
    }
}
