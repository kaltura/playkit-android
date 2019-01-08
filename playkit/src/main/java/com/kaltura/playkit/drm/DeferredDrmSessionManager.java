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

import com.google.android.kexoplayer2.drm.DefaultDrmSessionEventListener;
import com.google.android.kexoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.kexoplayer2.drm.DrmInitData;
import com.google.android.kexoplayer2.drm.DrmSession;
import com.google.android.kexoplayer2.drm.DrmSessionManager;
import com.google.android.kexoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.kexoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.kexoplayer2.drm.UnsupportedDrmException;
import com.google.android.kexoplayer2.extractor.mp4.PsshAtomUtil;
import com.google.android.kexoplayer2.upstream.HttpDataSource;
import com.google.android.kexoplayer2.util.Util;
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

public class DeferredDrmSessionManager implements DrmSessionManager<FrameworkMediaCrypto>, DefaultDrmSessionEventListener {

    private static final PKLog log = PKLog.get("DeferredDrmSessionManager");

    private Handler mainHandler;
    private DrmSessionListener drmSessionListener;
    private HttpDataSource.Factory dataSourceFactory;
    private LocalAssetsManager.LocalMediaSource localMediaSource = null;
    private DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;

    public interface DrmSessionListener {
        void onError(PKError error);
    }

    public DeferredDrmSessionManager(Handler mainHandler, HttpDataSource.Factory factory, DrmSessionListener drmSessionListener) {
        this.mainHandler = mainHandler;
        this.dataSourceFactory = factory;
        this.drmSessionListener = drmSessionListener;
    }

    public void setMediaSource(PKMediaSource mediaSource) {
        if (Util.SDK_INT < 18) {
            drmSessionManager = null;
            return;
        }

        try {
            String licenseUrl = null;
            if (mediaSource instanceof LocalAssetsManager.LocalMediaSource) {
                localMediaSource = (LocalAssetsManager.LocalMediaSource) mediaSource;
            } else {
                licenseUrl = getLicenseUrl(mediaSource);
            }
            drmSessionManager = DefaultDrmSessionManager.newWidevineInstance(new HttpMediaDrmCallback(licenseUrl, dataSourceFactory), null);
            if (mainHandler != null) {
                drmSessionManager.addListener(mainHandler, this);
            }
        } catch (UnsupportedDrmException exception) {

            PKError error = new PKError(PKPlayerErrorType.DRM_ERROR, "This device doesn't support widevine modular", exception);
            drmSessionListener.onError(error);
        }
    }

    @Override
    public boolean canAcquireSession(DrmInitData drmInitData) {
        return drmSessionManager != null && drmSessionManager.canAcquireSession(drmInitData);
    }

    @Override
    public DrmSession<FrameworkMediaCrypto> acquireSession(Looper playbackLooper, DrmInitData drmInitData) {
        if (drmSessionManager == null) {
            return null;
        }

        if (localMediaSource != null) {
            byte[] offlineKey;
            DrmInitData.SchemeData schemeData = getWidevineInitData(drmInitData);
            try {
                if (schemeData != null) {
                    offlineKey = localMediaSource.getStorage().load(toBase64(schemeData.data));
                    drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, offlineKey);
                    localMediaSource = null;
                }
            } catch (FileNotFoundException e) {
                PKError error = new PKError(PKPlayerErrorType.DRM_ERROR, "Failed to obtain offline licence from LocalDataStore. Requested key: " + Arrays.toString(schemeData.data) + ", for keysetId not found.", e);
                drmSessionListener.onError(error);
            }
        }

        return new SessionWrapper(playbackLooper, drmInitData, drmSessionManager);
    }

    @Override
    public void releaseSession(DrmSession drmSession) {
        if (drmSession instanceof SessionWrapper) {
            ((SessionWrapper) drmSession).release();
        } else {
            throw new IllegalStateException("Can't release unknown session");
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

}


class SessionWrapper implements DrmSession<FrameworkMediaCrypto> {

    private DrmSession<FrameworkMediaCrypto> realDrmSession;
    private DrmSessionManager<FrameworkMediaCrypto> realDrmSessionManager;

    SessionWrapper(Looper playbackLooper, DrmInitData drmInitData, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
        this.realDrmSession = drmSessionManager.acquireSession(playbackLooper, drmInitData);
        this.realDrmSessionManager = drmSessionManager;
    }

    void release() {
        realDrmSessionManager.releaseSession(realDrmSession);
        realDrmSessionManager = null;
        realDrmSession = null;
    }

    @Override
    public int getState() {
        return realDrmSession.getState();
    }

    @Override
    public FrameworkMediaCrypto getMediaCrypto() {
        return realDrmSession.getMediaCrypto();
    }

    @Override
    public DrmSessionException getError() {
        return realDrmSession.getError();
    }


    @Override
    public Map<String, String> queryKeyStatus() {
        return realDrmSession.queryKeyStatus();
    }

    @Override
    public byte[] getOfflineLicenseKeySetId() {
        return realDrmSession.getOfflineLicenseKeySetId();
    }
}
