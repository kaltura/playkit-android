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
 * Created by anton.afanasiev on 18/12/2016.
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
        List<PKDrmParams> drmData = mediaSource.getDrmData();
        String licenseUrl = null;
        if (drmData != null && drmData.size() > 0) {
            licenseUrl = drmData.get(0).getLicenseUri(); // ?? TODO: decide which of the drm items to take
        }
        return licenseUrl;
    }

    @Override
    public DrmSession<T> acquireSession(Looper playbackLooper, DrmInitData drmInitData) {
        if(drmSessionManager == null){
            return null;
        }

        //noinspection unchecked
        return (DrmSession<T>) drmSessionManager.acquireSession(playbackLooper, drmInitData);
    }

    @Override
    public void releaseSession(DrmSession drmSession) {
        if(drmSessionManager == null){
            return;
        }
        //noinspection unchecked
        drmSessionManager.releaseSession(drmSession);
    }
}
