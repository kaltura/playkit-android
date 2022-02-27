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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.kaltura.android.exoplayer2.drm.DrmInitData;
import com.kaltura.android.exoplayer2.drm.DrmSession;
import com.kaltura.android.exoplayer2.drm.DrmSessionEventListener;
import com.kaltura.android.exoplayer2.drm.DrmSessionManager;
import com.kaltura.android.exoplayer2.drm.FrameworkMediaDrm;
import com.kaltura.android.exoplayer2.drm.UnsupportedDrmException;
import com.kaltura.android.exoplayer2.extractor.mp4.PsshAtomUtil;
import com.kaltura.android.exoplayer2.source.MediaSource;
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

import static com.kaltura.playkit.Utils.toBase64;

/**
 * @hide
 */

public class DeferredDrmSessionManager implements DrmSessionManager, DrmSessionEventListener {

    private static final PKLog log = PKLog.get("DeferredDrmSessionManager");

    private String WIDEVINE_SECURITY_LEVEL_1 = "L1";
    private String WIDEVINE_SECURITY_LEVEL_3 = "L3";
    private String SECURITY_LEVEL_PROPERTY = "securityLevel";

    private Handler mainHandler;
    private final DrmCallback drmCallback;
    private DrmSessionListener drmSessionListener;
    private LocalAssetsManager.LocalMediaSource localMediaSource = null;
    private DrmSessionManager drmSessionManager;
    private boolean allowClearLead;
    private boolean forceWidevineL3Playback;

    public DeferredDrmSessionManager(Handler mainHandler, DrmCallback drmCallback, DrmSessionListener drmSessionListener, boolean allowClearLead, boolean forceWidevineL3Playback) {
        this.mainHandler = mainHandler;
        this.drmCallback = drmCallback;
        this.drmSessionListener = drmSessionListener;
        this.allowClearLead = allowClearLead;
        this.forceWidevineL3Playback = forceWidevineL3Playback;
        this.drmSessionManager = getDRMSessionManager(drmCallback);
    }

    public interface DrmSessionListener {
        void onError(PKError error);
    }

    public void setDrmSessionManager(DrmSessionManager drmSessionManager) {
        this.drmSessionManager = drmSessionManager;
    }

    public void setMediaSource(PKMediaSource mediaSource) {
        if (Util.SDK_INT < 18) {
            drmSessionManager = null;
            return;
        }

        if (mediaSource instanceof LocalAssetsManager.LocalMediaSource) {
            localMediaSource = (LocalAssetsManager.LocalMediaSource) mediaSource;
        } else {
            drmCallback.setLicenseUrl(getLicenseUrl(mediaSource));
        }
    }

    public void setLicenseUrl(String license) {
        if (Util.SDK_INT < 18) {
            drmSessionManager = null;
            return;
        }

        if (drmCallback != null) {
            drmCallback.setLicenseUrl(license);
        } else {
            log.d("DrmCallback is null");
        }
    }

    @Nullable
    @Override
    public DrmSession acquireSession(Looper playbackLooper, @Nullable EventDispatcher eventDispatcher, Format format) {
        if (drmSessionManager == null) {
            return null;
        }

        if (localMediaSource != null) {
            byte[] offlineKey;
            DrmInitData.SchemeData schemeData = getWidevineInitData(format.drmInitData);
            try {
                if (schemeData != null) {
                    offlineKey = localMediaSource.getStorage().load(toBase64(schemeData.data));
                    if (drmSessionManager instanceof DefaultDrmSessionManager) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            ((DefaultDrmSessionManager) drmSessionManager).setMode(DefaultDrmSessionManager.MODE_PLAYBACK, offlineKey);
                        }
                    }
                    localMediaSource = null;
                }
            } catch (FileNotFoundException e) {
                PKError error = new PKError(PKPlayerErrorType.DRM_ERROR, "Failed to obtain offline licence from LocalDataStore. Requested key: " + Arrays.toString(schemeData.data) + ", for keysetId not found.", e);
                drmSessionListener.onError(error);
            }
        }

        return drmSessionManager.acquireSession(playbackLooper, eventDispatcher, format);
    }

    private DrmSessionManager getDRMSessionManager(DrmCallback drmCallback) {
        log.d("getDRMSessionManager forceWidevineL3Playback = " + forceWidevineL3Playback);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            DefaultDrmSessionManager.Builder drmSessionManagerBuilder = new DefaultDrmSessionManager.Builder();
            drmSessionManagerBuilder.setMultiSession(true) // key rotation
                    .setPlayClearSamplesWithoutKeys(allowClearLead);

            if (forceWidevineL3Playback) {
                drmSessionManagerBuilder.setUuidAndExoMediaDrmProvider(
                        MediaSupport.WIDEVINE_UUID,
                        uuid -> {
                            try {
                                FrameworkMediaDrm frameworkMediaDrm = FrameworkMediaDrm.newInstance(MediaSupport.WIDEVINE_UUID);
                                frameworkMediaDrm.setPropertyString(SECURITY_LEVEL_PROPERTY, WIDEVINE_SECURITY_LEVEL_3);
                                return frameworkMediaDrm;
                            } catch (UnsupportedDrmException e) {
                                log.e("ForceWidevineL3Playback failed due to " + e.getMessage());
                                return FrameworkMediaDrm.DEFAULT_PROVIDER.acquireExoMediaDrm(MediaSupport.WIDEVINE_UUID);
                            }
                        });
            } else {
                drmSessionManagerBuilder.setUuidAndExoMediaDrmProvider(MediaSupport.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER);
            }
            drmSessionManager = drmSessionManagerBuilder.build(drmCallback);
        } else {
            drmSessionManager = DrmSessionManager.DRM_UNSUPPORTED;
        }

        return drmSessionManager;
    }

    @Override
    public int getCryptoType(@NonNull Format format) {
        if (drmSessionManager != null) {
            return drmSessionManager.getCryptoType(format);
        }
        return C.CRYPTO_TYPE_NONE;
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
    public void onDrmSessionAcquired(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, @DrmSession.State int state) {
        log.d("onDrmSessionAcquired");
    }

    @Override
    public void onDrmKeysLoaded(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
        log.d("onDrmKeysLoaded");
    }

    @Override
    public void onDrmSessionManagerError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, Exception error) {
        log.d("onDrmSessionManagerError");
        PKError pkError = new PKError(PKPlayerErrorType.DRM_ERROR, PKError.Severity.Recoverable, error.getMessage(), error);
        drmSessionListener.onError(pkError);
    }

    @Override
    public void onDrmKeysRestored(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
        log.d("onDrmKeysRestored");
    }

    @Override
    public void onDrmKeysRemoved(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
        log.d("onDrmKeysRemoved");
    }

    @Override
    public void onDrmSessionReleased(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
        log.d("onDrmSessionReleased");
    }

}

