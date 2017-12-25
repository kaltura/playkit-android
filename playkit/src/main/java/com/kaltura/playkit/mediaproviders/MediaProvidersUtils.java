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

package com.kaltura.playkit.mediaproviders;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.api.base.model.KalturaDrmPlaybackPluginData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.kaltura.playkit.PKDrmParams.Scheme.PlayReadyCENC;
import static com.kaltura.playkit.PKDrmParams.Scheme.Unknown;
import static com.kaltura.playkit.PKDrmParams.Scheme.WidevineCENC;
import static com.kaltura.playkit.PKDrmParams.Scheme.WidevineClassic;

/**
 * Created by gilad.nadav on 24/12/2017.
 */

public class MediaProvidersUtils {

    public static boolean isDRMSchemeValid(PKMediaSource pkMediaSource, List<KalturaDrmPlaybackPluginData> drmData) {
        if (drmData == null) {
            return false;
        }

        Iterator<KalturaDrmPlaybackPluginData> drmDataItr = drmData.iterator();
        while(drmDataItr.hasNext()) {
            KalturaDrmPlaybackPluginData drmDataItem = drmDataItr.next();
            if (getScheme(drmDataItem.getScheme()) == Unknown) {
                drmDataItr.remove();
            }
        }
        if (drmData.isEmpty()) {
            return false;
        }
        return true;
    }

    public static void updateDrmParams(PKMediaSource pkMediaSource, List<KalturaDrmPlaybackPluginData> drmData) {
        List<PKDrmParams> drmParams = new ArrayList<>();
        for (KalturaDrmPlaybackPluginData drm : drmData) {
            PKDrmParams.Scheme drmScheme = getScheme(drm.getScheme());
            drmParams.add(new PKDrmParams(drm.getLicenseURL(), drmScheme));
        }
        pkMediaSource.setDrmData(drmParams);
    }

    public static PKDrmParams.Scheme getScheme(String name) {

        switch (name) {
            case "WIDEVINE_CENC":
            case "drm.WIDEVINE_CENC":
                return WidevineCENC;
            case "PLAYREADY_CENC":
            case "drm.PLAYREADY_CENC":
                return PlayReadyCENC;
            case "WIDEVINE":
            case "widevine.WIDEVINE":
                return WidevineClassic;
            default:
                return Unknown;
        }
    }
}
