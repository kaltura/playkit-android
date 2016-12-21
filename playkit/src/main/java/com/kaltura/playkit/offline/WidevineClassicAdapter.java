package com.kaltura.playkit.offline;

import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.drm.DrmInfoEvent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playkit.PKLog;

/**
 * Created by anton.afanasiev on 13/12/2016.
 */

class WidevineClassicAdapter extends DrmAdapter {

    private static final PKLog log = PKLog.get("WidevineClassicAdapter");

    private final Context context;

    @Override
    public DRMScheme getScheme() {
        return DRMScheme.WidevineClassic;
    }

    WidevineClassicAdapter(Context context) {
        this.context = context;
    }

    @Override
    public boolean checkAssetStatus(String localAssetPath, String assetId, final LocalAssetsManager.AssetStatusListener listener) {
        WidevineDrmClient widevineDrmClient = new WidevineDrmClient(context);
        WidevineDrmClient.RightsInfo info = widevineDrmClient.getRightsInfo(localAssetPath);
        if (listener != null) {
            listener.onStatus(localAssetPath, info.expiryTime, info.availableTime);
        }
        return true;
    }

    @Override
    public boolean registerAsset(final String localAssetPath, String assetId, String licenseUri, final LocalAssetsManager.AssetRegistrationListener listener) {
        WidevineDrmClient widevineDrmClient = new WidevineDrmClient(context);
        widevineDrmClient.setEventListener(new WidevineDrmClient.EventListener() {
            @Override
            public void onError(DrmErrorEvent event) {
                log.d(event.toString());

                if (listener != null) {
                    listener.onFailed(localAssetPath, new Exception("License acquisition failed; DRM client error code: " + event.getType()));
                }
            }

            @Override
            public void onEvent(DrmEvent event) {
                log.d(event.toString());
                switch (event.getType()) {
                    case DrmInfoEvent.TYPE_RIGHTS_INSTALLED:
                        if (listener != null) {
                            listener.onRegistered(localAssetPath);
                        }
                        break;
                }
            }
        });
        widevineDrmClient.acquireLocalAssetRights(localAssetPath, licenseUri);

        return true;
    }

    @Override
    public boolean refreshAsset(String localAssetPath, String assetId, String licenseUri, LocalAssetsManager.AssetRegistrationListener listener) {
        return registerAsset(localAssetPath, assetId, licenseUri, listener);
    }

    @Override
    public boolean unregisterAsset(final String localAssetPath, String assetId, final LocalAssetsManager.AssetRemovalListener listener) {
        WidevineDrmClient widevineDrmClient = new WidevineDrmClient(context);
        widevineDrmClient.setEventListener(new WidevineDrmClient.EventListener() {
            @Override
            public void onError(DrmErrorEvent event) {
                log.d(event.toString());
            }

            @Override
            public void onEvent(DrmEvent event) {
                log.d(event.toString());
                switch (event.getType()) {
                    case DrmInfoEvent.TYPE_RIGHTS_REMOVED:
                        if (listener != null) {
                            listener.onRemoved(localAssetPath);
                        }
                        break;
                }
            }
        });
        widevineDrmClient.removeRights(localAssetPath);
        return true;
    }
}
