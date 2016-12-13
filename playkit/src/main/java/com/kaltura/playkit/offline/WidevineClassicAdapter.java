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

public class WidevineClassicAdapter extends DrmAdapter {

    private static final PKLog log = PKLog.get("WidevineClassicAdapter");

    private final Context mContext;

    @Override
    public DRMScheme getScheme() {
        return DRMScheme.WidevineClassic;
    }

    WidevineClassicAdapter(@NonNull Context context) {
        mContext = context;
    }

    @Override
    public boolean checkAssetStatus(@NonNull String localPath, @Nullable final LocalAssetsManager.AssetStatusListener listener) {
        WidevineDrmClient widevineDrmClient = new WidevineDrmClient(mContext);
        WidevineDrmClient.RightsInfo info = widevineDrmClient.getRightsInfo(localPath);
        if (listener != null) {
            listener.onStatus(localPath, info.expiryTime, info.availableTime);
        }
        return true;
    }

    @Override
    public boolean registerAsset(@NonNull final String localPath, @NonNull String licenseUri, @Nullable final LocalAssetsManager.AssetRegistrationListener listener) {
        WidevineDrmClient widevineDrmClient = new WidevineDrmClient(mContext);
        widevineDrmClient.setEventListener(new WidevineDrmClient.EventListener() {
            @Override
            public void onError(DrmErrorEvent event) {
                log.d(event.toString());

                if (listener != null) {
                    listener.onFailed(localPath, new Exception("License acquisition failed; DRM client error code: " + event.getType()));
                }
            }

            @Override
            public void onEvent(DrmEvent event) {
                log.d(event.toString());
                switch (event.getType()) {
                    case DrmInfoEvent.TYPE_RIGHTS_INSTALLED:
                        if (listener != null) {
                            listener.onRegistered(localPath);
                        }
                        break;
                }
            }
        });
        widevineDrmClient.acquireLocalAssetRights(localPath, licenseUri);

        return true;
    }

    @Override
    public boolean refreshAsset(@NonNull String localPath, @NonNull String licenseUri, @Nullable LocalAssetsManager.AssetRegistrationListener listener) {
        return registerAsset(localPath, licenseUri, listener);
    }

    @Override
    public boolean unregisterAsset(@NonNull final String localPath, final LocalAssetsManager.AssetRemovalListener listener) {
        WidevineDrmClient widevineDrmClient = new WidevineDrmClient(mContext);
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
                            listener.onRemoved(localPath);
                        }
                        break;
                }
            }
        });
        widevineDrmClient.removeRights(localPath);
        return true;
    }
}
