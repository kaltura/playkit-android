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

package com.kaltura.playkit.addon.cast;

import com.google.android.gms.cast.AdBreakInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.gson.GsonBuilder;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.utils.Consts;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by itanbarpeled on 27/12/2016.
 */

public class CastAdInfoParser implements RemoteMediaClient.ParseAdsInfoCallback {


    private static final String AD_JSON_OBJECT_NAME = "adsInfo";
    private static final PKLog log = PKLog.get("CastAdInfoParser");



    @Override
    public boolean parseIsPlayingAdFromMediaStatus(MediaStatus mediaStatus) {

        JSONObject customData = mediaStatus.getCustomData();
        AdsInfoData adsInfoData = getAdsInfoData(customData);

        boolean isPlayingAd = adsInfoData != null ? adsInfoData.getIsPlayingAd() : false;
        log.v("parseIsPlayingAdFromMediaStatus. isPlayingAd = " + isPlayingAd);

        return isPlayingAd;

    }


    @Override
    public List<AdBreakInfo> parseAdBreaksFromMediaStatus(MediaStatus mediaStatus) {


        List<AdBreakInfo> adBreakInfoList = new ArrayList<>();
        AdBreakInfo adBreakInfo;

        JSONObject customData = mediaStatus.getCustomData();
        AdsInfoData adsInfoData = getAdsInfoData(customData);

        if (adsInfoData == null) {
            return adBreakInfoList;
        }


        for (long adPosition : adsInfoData.getAdsBreakInfo()) {

            long adPositionInMs = adPosition * Consts.MILLISECONDS_MULTIPLIER;
            adBreakInfo = new AdBreakInfo.Builder(adPositionInMs).build();
            adBreakInfoList.add(adBreakInfo);

            log.v("parseAdBreaksFromMediaStatus. adPositionInMs = " + adPositionInMs);
        }


        return adBreakInfoList;

    }



    private AdsInfoData getAdsInfoData(JSONObject customData) {

        if (customData == null || !customData.has(AD_JSON_OBJECT_NAME)) {

            return null;

        } else {

            try {

                JSONObject adsInfoObject = customData.getJSONObject(AD_JSON_OBJECT_NAME);
                return new GsonBuilder().create().fromJson(adsInfoObject.toString(), AdsInfoData.class);

            } catch (JSONException e) {

                log.e(e.getMessage());
                return null;

            }
        }

    }



}
