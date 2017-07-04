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

import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.utils.EventLogger;

import java.util.List;
import java.util.Map;

/**
 * @hide
 */

public class DeferredDrmSessionManager implements DrmSessionManager<FrameworkMediaCrypto> {

    private static final PKLog log = PKLog.get("DeferredDrmSessionManager");

    private DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;

    private Handler mainHandler;
    private EventLogger eventLogger;
    private HttpDataSource.Factory dataSourceFactory;

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
        
        if (mediaSource instanceof LocalAssetsManager.LocalMediaSource) {
            buildLocalDrmSessionManager(mediaSource);
        } else {
            buildDefaultDrmSessionManager(getLicenseUrl(mediaSource));
        }
    }

    private void buildLocalDrmSessionManager(PKMediaSource mediaSource) {
        LocalAssetsManager.LocalMediaSource localMediaSource = (LocalAssetsManager.LocalMediaSource) mediaSource;
        drmSessionManager = new LocalDrmSessionManager<>(localMediaSource);
    }

    private void buildDefaultDrmSessionManager(String licenseUrl) {
        try {
            drmSessionManager = DefaultDrmSessionManager.newWidevineInstance(new HttpMediaDrmCallback(licenseUrl, dataSourceFactory), null, mainHandler, eventLogger);

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
        if (drmSessionManager == null){
            return null;
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
}

class SessionWrapper implements DrmSession<FrameworkMediaCrypto> {

    private DrmSession<FrameworkMediaCrypto> realDrmSession;
    private DrmSessionManager<FrameworkMediaCrypto> realDrmSessionManager;

    SessionWrapper(Looper playbackLooper, DrmInitData drmInitData, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
        this.realDrmSession = drmSessionManager.acquireSession(playbackLooper, drmInitData);;
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
        return null;
    }

    @Override
    public byte[] getOfflineLicenseKeySetId() {
        return new byte[0];
    }
}
