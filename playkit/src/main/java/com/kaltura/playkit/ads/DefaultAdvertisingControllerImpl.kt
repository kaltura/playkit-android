package com.kaltura.playkit.ads

import com.kaltura.playkit.plugins.ads.AdsProvider

class DefaultAdvertisingControllerImpl(private val adsProvider: AdsProvider): AdvertisingController {

    override fun setAdvertisingConfig(
        isConfigured: Boolean,
        adType: AdType,
        imaEventsListener: IMAEventsListener?
    ) {
        adsProvider.setAdvertisingConfig(isConfigured, adType, imaEventsListener)
    }

    override fun advertisingPlayAdNow(adTag: String?) {
        adsProvider.advertisingPlayAdNow(adTag)
    }

    override fun advertisingSetCuePoints(cuePoints: List<Long?>?) {
        adsProvider.advertisingSetCuePoints(cuePoints)
    }

    override fun advertisingSetAdInfo(pkAdvertisingAdInfo: PKAdvertisingAdInfo?) {
        adsProvider.advertisingSetAdInfo(pkAdvertisingAdInfo)
    }

    override fun advertisingPreparePlayer() {
        adsProvider.advertisingPreparePlayer()
    }
}