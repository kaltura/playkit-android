package com.kaltura.playkit.ads

import com.kaltura.playkit.plugins.ads.AdEvent

/**
 * Interface to listen to the callbacks on PKAdvertisingController from
 * AdsProvider(IMAPlugin)
 */
interface IMAEventsListener {

    /**
     * Listen to all ads completed event
     * In Advertising case, it will be called after each Ad playback
     * If Advertising is set then this event will be fired from PKAdvertisingController
     */
    fun allAdsCompleted()

    /**
     * Listen to content pause requested event from AdsProvider(IMAPlugin)
     */
    fun contentPauseRequested()

    /**
     * Listen to content resume requested event from AdsProvider(IMAPlugin)
     */
    fun contentResumeRequested()

    /**
     * Listen to AdError event from AdsProvider(IMAPlugin)
     * It's implementation holds the logic
     */
    fun adError(error: AdEvent.Error)
}
