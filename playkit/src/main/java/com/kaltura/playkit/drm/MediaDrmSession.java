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

import android.annotation.TargetApi;
import android.media.DeniedByServerException;
import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.os.Build;
import androidx.annotation.NonNull;

import com.kaltura.androidx.media3.common.DrmInitData;
import com.kaltura.androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import com.kaltura.playkit.player.MediaSupport;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by anton.afanasiev on 13/12/2016.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MediaDrmSession {

    private final FrameworkMediaDrm mMediaDrm;
    private byte[] mSessionId;

    private MediaDrmSession(@NonNull FrameworkMediaDrm mediaDrm) {
        mMediaDrm = mediaDrm;
    }

    static MediaDrmSession open(@NonNull FrameworkMediaDrm mediaDrm) throws MediaDrmException {
        MediaDrmSession session = new MediaDrmSession(mediaDrm);
        session.mSessionId = mediaDrm.openSession();
        return session;
    }

    byte[] getId() {
        return mSessionId;
    }

    void close() {
        mMediaDrm.closeSession(mSessionId);
    }

    void restoreKeys(byte[] keySetId) {
        mMediaDrm.restoreKeys(mSessionId, keySetId);
    }


    Map<String, String> queryKeyStatus() {
        return mMediaDrm.queryKeyStatus(mSessionId);
    }

    FrameworkMediaDrm.KeyRequest getOfflineKeyRequest(byte[] initData, String mimeType) {
        try {
            List<DrmInitData.SchemeData> schemeList = Collections.singletonList(new DrmInitData.SchemeData(MediaSupport.WIDEVINE_UUID, mimeType, initData));
            return mMediaDrm.getKeyRequest(mSessionId, schemeList, MediaDrm.KEY_TYPE_OFFLINE, null);
        } catch (Exception e) {
            throw new WidevineNotSupportedException(e);
        }
    }

    byte[] provideKeyResponse(byte[] keyResponse) throws DeniedByServerException {
        try {
            return mMediaDrm.provideKeyResponse(mSessionId, keyResponse);
        } catch (Exception e) {
            throw new WidevineNotSupportedException(e);
        }
    }
}
