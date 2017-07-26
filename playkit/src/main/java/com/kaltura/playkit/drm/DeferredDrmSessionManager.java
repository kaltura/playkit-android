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

import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.mp4.PsshAtomUtil;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.utils.EventLogger;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import static com.kaltura.playkit.Utils.toBase64;

/**
 * @hide
 */

public class DeferredDrmSessionManager implements DrmSessionManager<FrameworkMediaCrypto> {

    private static final PKLog log = PKLog.get("DeferredDrmSessionManager");

    private DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;

    private Handler mainHandler;
    private EventLogger eventLogger;
    private HttpDataSource.Factory dataSourceFactory;
    private LocalAssetsManager.LocalMediaSource localMediaSource = null;
    private boolean modeSet = false;

    public DeferredDrmSessionManager(Handler mainHandler, EventLogger eventLogger, HttpDataSource.Factory factory) {
        this.mainHandler = mainHandler;
        this.eventLogger = eventLogger;

        this.dataSourceFactory = factory;
    }

    public void setMediaSource(PKMediaSource mediaSource) {
        if (Util.SDK_INT < 18) {
            drmSessionManager = null;
            return;
        }

        try {
            String licenseUrl = getLicenseUrl(mediaSource);
            drmSessionManager = DefaultDrmSessionManager.newWidevineInstance(new HttpMediaDrmCallback(licenseUrl, dataSourceFactory), null, mainHandler, eventLogger);
            if (mediaSource instanceof LocalAssetsManager.LocalMediaSource) {
                localMediaSource = (LocalAssetsManager.LocalMediaSource) mediaSource;
            }
        } catch (UnsupportedDrmException exception) {
            log.w("This device doesn't support widevine modular " + exception.getMessage());
        }
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
    public DrmSession<FrameworkMediaCrypto> acquireSession(Looper playbackLooper, DrmInitData drmInitData) {
        if (drmSessionManager == null) {
            return null;
        }
        log.e("acqure session");

        if (localMediaSource != null && !modeSet) {
            log.e("acqure session inner");
            byte[] offlineKey;
            try {
                byte[] initData =  getWidevineInitData(drmInitData).data;
                if(initData != null) {
                    offlineKey = localMediaSource.getStorage().load(toBase64(initData));
                    drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, offlineKey);
                    modeSet = true;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }
        return new SessionWrapper(playbackLooper, drmInitData, drmSessionManager);
    }

    @Override
    public void releaseSession(DrmSession drmSession) {
        localMediaSource = null;
        modeSet = false;
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


        DrmInitData.SchemeData schemeData = drmInitData.get(MediaSupport.WIDEVINE_UUID);
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
    public boolean requiresSecureDecoderComponent(String mimeType) {
        return realDrmSession.requiresSecureDecoderComponent(mimeType);
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
