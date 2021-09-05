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
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.drm.DrmInfoEvent;

import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKRequestParams;


/**
 * Created by anton.afanasiev on 13/12/2016.
 */

class WidevineClassicAdapter extends DrmAdapter {

    private static final PKLog log = PKLog.get("WidevineClassicAdapter");

    private final Context context;

    WidevineClassicAdapter(Context context) {
        this.context = context;
    }

    @Override
    public boolean checkAssetStatus(String localAssetPath, String assetId, boolean forceWidevineL3Playback, final LocalAssetsManager.AssetStatusListener listener) {
        WidevineClassicDrm widevineClassicDrm = new WidevineClassicDrm(context);
        WidevineClassicDrm.RightsInfo info = widevineClassicDrm.getRightsInfo(localAssetPath);
        if (listener != null) {
            listener.onStatus(localAssetPath, info.expiryTime, info.availableTime, true);
        }
        return true;
    }

    @Override
    public boolean registerAsset(final String localAssetPath, String assetId, String licenseUri, PKRequestParams.Adapter adapter, boolean forceWidevineL3Playback, final LocalAssetsManager.AssetRegistrationListener listener) {
        WidevineClassicDrm widevineClassicDrm = new WidevineClassicDrm(context);
        widevineClassicDrm.setEventListener(new WidevineClassicDrm.EventListener() {
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
                if (event.getType() == DrmInfoEvent.TYPE_RIGHTS_INSTALLED) {
                    if (listener != null) {
                        listener.onRegistered(localAssetPath);
                    }
                }
            }
        });
        widevineClassicDrm.acquireLocalAssetRights(localAssetPath, licenseUri);

        return true;
    }

    @Override
    public boolean refreshAsset(String localAssetPath, String assetId, String licenseUri, PKRequestParams.Adapter adapter, boolean forceWidevineL3Playback, LocalAssetsManager.AssetRegistrationListener listener) {
        return registerAsset(localAssetPath, assetId, licenseUri, null, forceWidevineL3Playback, listener);
    }

    @Override
    public boolean unregisterAsset(final String localAssetPath, String assetId, boolean forceWidevineL3Playback, final LocalAssetsManager.AssetRemovalListener listener) {
        WidevineClassicDrm widevineClassicDrm = new WidevineClassicDrm(context);
        widevineClassicDrm.setEventListener(new WidevineClassicDrm.EventListener() {
            @Override
            public void onError(DrmErrorEvent event) {
                log.d(event.toString());
            }

            @Override
            public void onEvent(DrmEvent event) {
                log.d(event.toString());
                if (event.getType() == DrmInfoEvent.TYPE_RIGHTS_REMOVED) {
                    if (listener != null) {
                        listener.onRemoved(localAssetPath);
                    }
                }
            }
        });
        widevineClassicDrm.removeRights(localAssetPath);
        return true;
    }
}
