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

import android.media.MediaCryptoException;
import android.media.MediaDrmException;
import android.media.NotProvisionedException;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.RequiresApi;

import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.mp4.PsshAtomUtil;
import com.google.android.exoplayer2.util.Util;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.LocalDataStore;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.player.MediaSupport;

import java.io.FileNotFoundException;
import java.util.Map;

import static com.kaltura.playkit.Utils.toBase64;

/**
 * @hide
 */

public class LocalDrmSessionManager<T extends ExoMediaCrypto> implements DrmSessionManager<T>, DrmSession<T> {

    private static final PKLog log = PKLog.get("LocalDrmSessionManager");

    private T mediaCrypto;
    private FrameworkMediaDrm mediaDrm;
    private MediaDrmSession drmSession;
    private LocalDataStore drmStorage;

    private Exception lastError;
    private int state = STATE_CLOSED;


    public LocalDrmSessionManager(LocalAssetsManager.LocalMediaSource mediaSource) {
        this.drmStorage = mediaSource.getStorage();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public DrmSession<T> acquireSession(Looper playbackLooper, DrmInitData drmInitData) {
        if (Util.SDK_INT < 18) {
            return null;
        }

        DrmInitData.SchemeData schemeData = getWidevineInitData(drmInitData);
        if (schemeData == null) {
            log.e("Widevine PSSH not found");
            return null;
        }

        byte[] initData = schemeData.data;

        try {
            mediaDrm = FrameworkMediaDrm.newInstance(MediaSupport.WIDEVINE_UUID);
            drmSession = openSessionWithKeys(initData);
            state = STATE_OPENED_WITH_KEYS;

            //noinspection unchecked
            mediaCrypto = (T) mediaDrm.createMediaCrypto(MediaSupport.WIDEVINE_UUID, drmSession.getId());

        } catch (NotProvisionedException e) {
            throw new WidevineNotSupportedException(e);
        } catch (MediaCryptoException | UnsupportedDrmException e) {
            onError(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            onError(e);
        }

        return this;
    }

    @Override
    public void releaseSession(DrmSession<T> drmSession) {
        mediaCrypto = null;

        if (this.drmSession != null) {
            this.drmSession.close();
            this.drmSession = null;
        }
        state = STATE_CLOSED;
    }


    private DrmInitData.SchemeData getWidevineInitData(DrmInitData drmInitData) {
        if (drmInitData == null) {
            log.e("No PSSH in media");
            return null;
        }

        state = STATE_OPENING;

        DrmInitData.SchemeData schemeData = drmInitData.get(MediaSupport.WIDEVINE_UUID);
        if (schemeData == null) {
            onError(new IllegalStateException("Widevine PSSH not found"));
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

    /**
     * Open drm session with keySetId that was previously saved in {@link LocalDataStore}
     * @param initData - the init data with which we will obtain the proper keySetId.
     * @return - the {@link MediaDrmSession}.
     * @throws MediaDrmException - {@link MediaDrmException}
     * @throws MediaCryptoException - {@link MediaCryptoException}
     * @throws FileNotFoundException - {@link FileNotFoundException}
     */
    private MediaDrmSession openSessionWithKeys(byte[] initData) throws MediaDrmException, MediaCryptoException, FileNotFoundException {
        String key = toBase64(initData);
        byte[] keySetId = drmStorage.load(key);

        MediaDrmSession session = MediaDrmSession.open(mediaDrm);
        session.restoreKeys(keySetId);

        return session;
    }

    private void onError(Exception error) {
        log.e(error.getMessage());
        lastError = error;
        state = STATE_ERROR;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public T getMediaCrypto() {
        return mediaCrypto;
    }

    @Override
    public boolean requiresSecureDecoderComponent(String mimeType) {
        if (state != STATE_OPENED && state != STATE_OPENED_WITH_KEYS) {
            throw new IllegalStateException();
        }
        return mediaCrypto.requiresSecureDecoderComponent(mimeType);
    }

    @Override
    public DrmSessionException getError() {
        return (DrmSessionException) lastError;
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
