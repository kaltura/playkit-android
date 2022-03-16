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

import android.content.Context;
import androidx.annotation.NonNull;

import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.LocalDataStore;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKRequestParams;

import java.io.IOException;

/**
 * @hide
 */

public abstract class DrmAdapter {

    private static final PKLog log = PKLog.get("DrmAdapter");


    @NonNull
    public static DrmAdapter getDrmAdapter(PKDrmParams.Scheme scheme, Context context, LocalDataStore localDataStore) {

        if (scheme == null) {
            return new NullDrmAdapter();
        }

        switch (scheme) {
            case WidevineCENC:
                return new WidevineModularAdapter(context, localDataStore);
            case WidevineClassic:
                return new WidevineClassicAdapter(context);
            case PlayReadyCENC:
                log.d("PlayReadyCENC is supported using exoplayer default flow");
                //return new PlayreadyAdapter(context, localDataStore);
            default:
                break;
        }

        return new NullDrmAdapter();
    }

    public abstract boolean registerAsset(final String localAssetPath, final String assetId, final String licenseUri, PKRequestParams.Adapter adapter, boolean forceWidevineL3Playback, final LocalAssetsManager.AssetRegistrationListener listener) throws IOException;

    public abstract boolean refreshAsset(final String localAssetPath, final String assetId, final String licenseUri, PKRequestParams.Adapter adapter, boolean forceWidevineL3Playback, final LocalAssetsManager.AssetRegistrationListener listener);

    public abstract boolean unregisterAsset(final String localAssetPath, final String assetId, boolean forceWidevineL3Playback, final LocalAssetsManager.AssetRemovalListener listener);

    public abstract boolean checkAssetStatus(final String localAssetPath, final String assetId, boolean forceWidevineL3Playback, final LocalAssetsManager.AssetStatusListener listener);

    private static class NullDrmAdapter extends DrmAdapter {
        @Override
        public boolean checkAssetStatus(String localAssetPath, String assetId, boolean forceWidevineL3Playback, LocalAssetsManager.AssetStatusListener listener) {
            if (listener != null) {
                listener.onStatus(localAssetPath, -1, -1, false);
            }
            return true;
        }

        @Override
        public boolean registerAsset(String localAssetPath, String assetId, String licenseUri, PKRequestParams.Adapter adapter, boolean forceWidevineL3Playback, LocalAssetsManager.AssetRegistrationListener listener) {
            if (listener != null) {
                listener.onRegistered(localAssetPath);
            }
            return true;
        }

        @Override
        public boolean refreshAsset(String localAssetPath, String assetId, String licenseUri, PKRequestParams.Adapter adapter, boolean forceWidevineL3Playback, LocalAssetsManager.AssetRegistrationListener listener) {
            return registerAsset(localAssetPath, assetId, licenseUri, adapter, forceWidevineL3Playback,  listener);
        }

        @Override
        public boolean unregisterAsset(String localAssetPath, String assetId, boolean forceWidevineL3Playback, LocalAssetsManager.AssetRemovalListener listener) {
            if (listener != null) {
                listener.onRemoved(localAssetPath);
            }
            return true;
        }
    }
}
