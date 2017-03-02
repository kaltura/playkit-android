package com.kaltura.playkit.drm;

import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.utils.EventLogger;

import java.util.List;

/**
 * @hide
 */

public class DeferredDrmSessionManager<T extends ExoMediaCrypto> implements DrmSessionManager<T> {

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
            buildStreamingDrmSessionManager(getLicenseUrl(mediaSource));
        }
    }

    private void buildLocalDrmSessionManager(PKMediaSource mediaSource) {
        LocalAssetsManager.LocalMediaSource localMediaSource = (LocalAssetsManager.LocalMediaSource) mediaSource;
        drmSessionManager = new LocalDrmSessionManager<>(localMediaSource);
    }

    private void buildStreamingDrmSessionManager(String licenseUrl) {
        try {
            drmSessionManager = StreamingDrmSessionManager.newWidevineInstance(new HttpMediaDrmCallback(licenseUrl, dataSourceFactory), null, mainHandler, eventLogger);

        } catch (UnsupportedDrmException exception) {
            log.w("This device doesn't support widevine modular " + exception.getMessage());
        }
    }

    private String getLicenseUrl(PKMediaSource mediaSource) {
        String licenseUrl = null;

        if (mediaSource.hasDrmParams()) {
            List<PKDrmParams> drmData = mediaSource.getDrmData();
            for (PKDrmParams pkDrmParam : drmData) {
                // selecting widevine_cenc as default right now
                if (PKDrmParams.Scheme.widevine_cenc == pkDrmParam.getScheme()) {
                    licenseUrl = pkDrmParam.getLicenseUri();
                    break;
                }
            }
        }
        return licenseUrl;
    }

    @Override
    public DrmSession<T> acquireSession(Looper playbackLooper, DrmInitData drmInitData) {
        if (drmSessionManager == null){
            return null;
        }

        return new SessionWrapper<>(playbackLooper, drmInitData, (DrmSessionManager<T>) drmSessionManager);
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

class SessionWrapper<T extends ExoMediaCrypto> implements DrmSession<T> {

    private DrmSession<T> realDrmSession;
    private DrmSessionManager<T> realDrmSessionManager;

    SessionWrapper(Looper playbackLooper, DrmInitData drmInitData, DrmSessionManager<T> drmSessionManager) {
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
    public T getMediaCrypto() {
        return realDrmSession.getMediaCrypto();
    }

    @Override
    public boolean requiresSecureDecoderComponent(String mimeType) {
        return realDrmSession.requiresSecureDecoderComponent(mimeType);
    }

    @Override
    public Exception getError() {
        return realDrmSession.getError();
    }
}
