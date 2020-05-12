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

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.drm.DefaultDrmSessionEventListener;
import com.kaltura.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.kaltura.android.exoplayer2.drm.DrmInitData;
import com.kaltura.android.exoplayer2.drm.DrmSession;
import com.kaltura.android.exoplayer2.drm.DrmSessionManager;
import com.kaltura.android.exoplayer2.drm.ExoMediaCrypto;
import com.kaltura.android.exoplayer2.drm.FrameworkMediaDrm;
import com.kaltura.android.exoplayer2.extractor.mp4.PsshAtomUtil;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.player.PKPlayerErrorType;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.kaltura.playkit.Utils.toBase64;

/**
 * @hide
 */

public class DeferredDrmSessionManager implements DrmSessionManager<ExoMediaCrypto>, DefaultDrmSessionEventListener {

    private static final PKLog log = PKLog.get("DeferredDrmSessionManager");

    private Handler mainHandler;
    private final DrmCallback drmCallback;
    private DrmSessionListener drmSessionListener;
    private LocalAssetsManager.LocalMediaSource localMediaSource = null;
    private DrmSessionManager drmSessionManager;
    private boolean allowClearLead;

    public DeferredDrmSessionManager(Handler mainHandler, DrmCallback drmCallback, DrmSessionListener drmSessionListener, boolean allowClearLead) {
        this.mainHandler = mainHandler;
        this.drmCallback = drmCallback;
        this.drmSessionListener = drmSessionListener;
        drmSessionManager = DrmSessionManager.getDummyDrmSessionManager();
        this.allowClearLead = allowClearLead;
    }

    public interface DrmSessionListener {
        void onError(PKError error);
    }

    public void setMediaSource(PKMediaSource mediaSource) {
        if (Util.SDK_INT < 18) {
            drmSessionManager = null;
            return;
        }

        drmSessionManager = new DefaultDrmSessionManager.Builder()
                //.setUuidAndExoMediaDrmProvider(MediaSupport.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                //.setMultiSession(true)
                .setPlayClearSamplesWithoutKeys(allowClearLead)
                .build(drmCallback);

        if (mediaSource instanceof LocalAssetsManager.LocalMediaSource) {
            localMediaSource = (LocalAssetsManager.LocalMediaSource) mediaSource;
        } else {
            drmCallback.setLicenseUrl(getLicenseUrl(mediaSource));
        }

        if (mainHandler != null) {
            if (drmSessionManager instanceof DefaultDrmSessionManager) {
                ((DefaultDrmSessionManager) drmSessionManager).addListener(mainHandler, this);
            }
        }
    }

    @Override
    public boolean canAcquireSession(DrmInitData drmInitData) {
        return drmSessionManager != null && drmSessionManager.canAcquireSession(drmInitData);
    }

    @Nullable
    @Override
    public DrmSession<ExoMediaCrypto> acquirePlaceholderSession(Looper playbackLooper, int trackType) {
        if (drmSessionManager != null) {
            return drmSessionManager.acquirePlaceholderSession(playbackLooper, trackType);
        }
        return null;
    }

    @Override
    public DrmSession<ExoMediaCrypto> acquireSession(Looper playbackLooper, DrmInitData drmInitData) {
        if (drmSessionManager == null) {
            return null;
        }

        if (localMediaSource != null) {
            byte[] offlineKey;
            DrmInitData.SchemeData schemeData = getWidevineInitData(drmInitData);
            try {
                if (schemeData != null) {
                    offlineKey = localMediaSource.getStorage().load(toBase64(schemeData.data));
                    if (drmSessionManager instanceof DefaultDrmSessionManager) {
                        ((DefaultDrmSessionManager) drmSessionManager).setMode(DefaultDrmSessionManager.MODE_PLAYBACK, offlineKey);
                    }
                    localMediaSource = null;
                }
            } catch (FileNotFoundException e) {
                PKError error = new PKError(PKPlayerErrorType.DRM_ERROR, "Failed to obtain offline licence from LocalDataStore. Requested key: " + Arrays.toString(schemeData.data) + ", for keysetId not found.", e);
                drmSessionListener.onError(error);
            }
        }

        return drmSessionManager.acquireSession(playbackLooper, drmInitData);
    }

    @Nullable
    @Override
    public Class<? extends ExoMediaCrypto> getExoMediaCryptoType(DrmInitData drmInitData) {
        if (drmSessionManager != null) {
            return drmSessionManager.getExoMediaCryptoType(drmInitData);
        }
        return null;
    }

    @Override
    public void prepare() {
        if (drmSessionManager != null) {
            drmSessionManager.prepare();
        }
    }

    @Override
    public void release() {
        if (drmSessionManager != null) {
            drmSessionManager.release();
        }
    }

    private DrmInitData.SchemeData getWidevineInitData(DrmInitData drmInitData) {
        if (drmInitData == null) {
            log.e("No PSSH in media");
            return null;
        }

        DrmInitData.SchemeData schemeData = null;
        for (int i = 0 ; i < drmInitData.schemeDataCount ; i++) {
            if (drmInitData.get(i) != null && drmInitData.get(i).matches(MediaSupport.WIDEVINE_UUID)) {
                schemeData = drmInitData.get(i);
            }
        }

        if (schemeData == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Prior to L the Widevine CDM required data to be extracted from the PSSH atom.
            byte[] psshData = PsshAtomUtil.parseSchemeSpecificData(schemeData.data, MediaSupport.WIDEVINE_UUID);
            if (psshData == null) {
                log.w("Extraction failed. schemeData isn't a Widevine PSSH atom, so leave it unchanged.");
            } else {
                schemeData = new DrmInitData.SchemeData(MediaSupport.WIDEVINE_UUID, schemeData.mimeType, psshData);
            }
        }
        return schemeData;
    }

    private String getLicenseUrl(PKMediaSource mediaSource) {
        String licenseUrl = null;

        if (mediaSource.hasDrmParams()) {
            List<PKDrmParams> drmData = mediaSource.getDrmData();
            for (PKDrmParams pkDrmParam : drmData) {
                // selecting WidevineCENC as default right now
                if (PKDrmParams.Scheme.WidevineCENC == pkDrmParam.getScheme()) {
                    licenseUrl = pkDrmParam.getLicenseUri();
                    break;
                }
            }
        }
        return licenseUrl;
    }

    @Override
    public void onDrmKeysLoaded() {
        log.d("onDrmKeysLoaded");
    }

    @Override
    public void onDrmSessionManagerError(Exception e) {
        log.d("onDrmSessionManagerError");
        PKError error = new PKError(PKPlayerErrorType.DRM_ERROR, e.getMessage(), e);
        drmSessionListener.onError(error);
    }

    @Override
    public void onDrmKeysRestored() {
        log.d("onDrmKeysRestored");
    }

    @Override
    public void onDrmKeysRemoved() {
        log.d("onDrmKeysRemoved");
    }

    @Override
    public void onDrmSessionAcquired() {
        log.d("onDrmSessionAcquired");
    }

    @Override
    public void onDrmSessionReleased() {
        log.d("onDrmSessionReleased");
    }

}

