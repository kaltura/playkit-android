package com.kaltura.playkit.ads

import com.kaltura.playkit.PKEvent
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.Player
import com.kaltura.playkit.PlayerEvent
import com.kaltura.playkit.PlayerEvent.PlayheadUpdated
import com.kaltura.playkit.ads.AdController
import com.kaltura.playkit.ads.Advertising
import com.kaltura.playkit.ads.PKAdvertising
import com.kaltura.playkit.plugins.ads.AdEvent

class PKAdvertisingController: PKAdvertising {

    private val log = PKLog.get(PKAdvertisingController::class.java.simpleName)
    private var player: Player? = null
    private var adController: AdController? = null
    private var advertising: Advertising? = null

    private var midRollTiming: Int = -2
    private var isCustomAdTriggered: Boolean = false

    override fun playAdNow() {
        getPrerollAd()?.let {
            adController?.playAdNow(it)
        }
    }

    override fun playAdNow(vastAdTag: String) {
        TODO("Not yet implemented")
    }

    override fun getCurrentAd() {
        TODO("Not yet implemented")
    }

    fun setPlayer(player: Player) {
        this.player = player
        subscribeToAdEvents()
    }

    fun setAdController(adController: AdController) {
        this.adController = adController
    }

    fun setAdvertising(advertising: Advertising) {
        this.advertising = advertising
        adController?.advertisingConfigured(true)
    }

    private fun subscribeToAdEvents() {
        player?.addListener(this, PlayerEvent.playheadUpdated) {
            log.d("playheadUpdated = ${it.position}")
            if (!isCustomAdTriggered && it.position > 15000L && it.position < 16000L) {
                isCustomAdTriggered = true
                adController?.playAdNow(getMidrollAd())
            }
        }

        player?.addListener(this, AdEvent.started) {
            log.d("started")
        }

        player?.addListener(this, AdEvent.contentResumeRequested) {
            log.d("contentResumeRequested")
        }

        player?.addListener(this, AdEvent.contentPauseRequested) {
            log.d("contentPauseRequested")
        }

        player?.addListener(this, AdEvent.adPlaybackInfoUpdated) {
            log.d("adPlaybackInfoUpdated")
        }

        player?.addListener(this, AdEvent.skippableStateChanged) {
            log.d("skippableStateChanged")
        }

        player?.addListener(this, AdEvent.adRequested) {
            log.d("adRequested")
        }

        player?.addListener(this, AdEvent.playHeadChanged) {
            log.d("playHeadChanged")
        }

        player?.addListener(this, AdEvent.adBreakStarted) {
            log.d("adBreakStarted")
        }

        player?.addListener(this, AdEvent.cuepointsChanged) {
            log.d("cuepointsChanged")
        }

        player?.addListener(this, AdEvent.loaded) {
            log.d("loaded")
        }

        player?.addListener(this, AdEvent.resumed) {
            log.d("resumed")
        }

        player?.addListener(this, AdEvent.paused) {
            log.d("paused")
        }

        player?.addListener(this, AdEvent.skipped) {
            log.d("skipped")
        }

        player?.addListener(this, AdEvent.allAdsCompleted) {
            log.d("allAdsCompleted")
        }

        player?.addListener(this, AdEvent.completed) {
            isCustomAdTriggered = false
            log.d("completed")
        }

        player?.addListener(this, AdEvent.firstQuartile) {
            log.d("firstQuartile")
        }

        player?.addListener(this, AdEvent.midpoint) {
            log.d("midpoint")
        }

        player?.addListener(this, AdEvent.thirdQuartile) {
            log.d("thirdQuartile")
        }

        player?.addListener(this, AdEvent.adBreakEnded) {
            log.d("adBreakEnded")
        }

        player?.addListener(this, AdEvent.adClickedEvent) {
            log.d("adClickedEvent")
        }

        player?.addListener(this, AdEvent.error) {
            log.d("error")
        }

    }

    private fun populateAdTags() {

    }

    private fun getPrerollAd(): String? {
        val prerollAvailable = advertising?.prerollAd?.isNotEmpty() ?: return null

        if (prerollAvailable) {
            return advertising?.prerollAd?.get(0)
        }
        return null
    }

    private fun getMidrollAd(): String? {
        val midrollAvailable = advertising?.midrollAds?.isNotEmpty() ?: return null

        if (midrollAvailable) {
            return advertising?.midrollAds?.get(0)?.ads?.get(0)
        }
        return null
    }

    private fun getPostrollAd(): String? {
        val postrollAvailable = advertising?.postrollAd?.isNotEmpty() ?: return null

        if (postrollAvailable) {
            return advertising?.postrollAd?.get(0)
        }
        return null
    }
}