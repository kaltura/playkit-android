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

import java.util.List;

/**
 * Created by itanbarpeled on 27/12/2016.
 */

class AdsInfoData {


    private Boolean isPlayingAd;
    private List<Long> adsBreakInfo;


    Boolean getIsPlayingAd() {
        return isPlayingAd;
    }


    List<Long> getAdsBreakInfo() {
        return adsBreakInfo;
    }


}
