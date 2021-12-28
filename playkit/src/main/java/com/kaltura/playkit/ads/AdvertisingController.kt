package com.kaltura.playkit.ads

import com.kaltura.playkit.PKController

interface AdvertisingController: PKController {
    // APIs for internal use (AdLayout)
    fun setAdvertisingConfig(
        isConfigured: Boolean,
        adType: AdType,
        imaEventsListener: IMAEventsListener?
    )

    fun advertisingPlayAdNow(adTag: String?)

    fun advertisingSetCuePoints(cuePoints: List<Long?>?)

    fun advertisingSetAdInfo(pkAdvertisingAdInfo: PKAdvertisingAdInfo?)

    fun advertisingPreparePlayer()
}