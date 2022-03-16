package com.kaltura.playkit.ads

import com.kaltura.playkit.PKController

/**
 * Interface helping PKAdvertisingController to interact with AdsProvider(IMAPlugin)
 */
interface AdvertisingController: PKController {

    /**
     * Set if Advertising is configured
     */
    fun setAdvertisingConfig(isConfigured: Boolean, adType: AdType, imaEventsListener: IMAEventsListener?)

    /**
     * Send ad for the playback to AdsProvider(IMAPlugin)
     */
    fun advertisingPlayAdNow(adTag: String?)

    /**
     * Set/Update CuePoints
     */
    fun advertisingSetCuePoints(cuePoints: List<Long?>?)

    /**
     * Set AdInfo on the AdsProvider(IMAPlugin)
     */
    fun advertisingSetAdInfo(pkAdvertisingAdInfo: PKAdvertisingAdInfo?)

    /**
     * Prepare the Content Player
     */
    fun advertisingPreparePlayer()
}
