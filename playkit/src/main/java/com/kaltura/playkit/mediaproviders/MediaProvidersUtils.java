package com.kaltura.playkit.mediaproviders;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.api.base.model.KalturaDrmPlaybackPluginData;

import java.util.ArrayList;
import java.util.List;

import static com.kaltura.playkit.PKDrmParams.Scheme.FairPlay;
import static com.kaltura.playkit.PKDrmParams.Scheme.PlayReadyCENC;
import static com.kaltura.playkit.PKDrmParams.Scheme.WidevineCENC;
import static com.kaltura.playkit.PKDrmParams.Scheme.WidevineClassic;

/**
 * Created by gilad.nadav on 24/12/2017.
 */

public class MediaProvidersUtils {

    public static boolean isFairPlaySource(PKMediaSource pkMediaSource, List<KalturaDrmPlaybackPluginData> drmData) {
        if (drmData.size() == 1 && pkMediaSource.getMediaFormat() == PKMediaFormat.hls) {
            PKDrmParams.Scheme drmScheme = getScheme(drmData.get(0).getScheme());
            if (drmScheme == FairPlay) {
                return true;
            }
        }
        return false;
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
            case "drm.WIDEVINE_CENC":
                return WidevineCENC;
            case "drm.PLAYREADY_CENC":
                return PlayReadyCENC;
            case "widevine.WIDEVINE":
                return WidevineClassic;
            case "fairplay.FAIRPLAY":
                return FairPlay;
            default:
                return null;
        }
    }
}
