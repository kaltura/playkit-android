package com.kaltura.playkit.ads

import androidx.annotation.Nullable
import com.kaltura.playkit.MessageBus
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.Player
import com.kaltura.playkit.PlayerEvent
import com.kaltura.playkit.plugins.ads.AdEvent
import com.kaltura.playkit.plugins.ads.AdPositionType
import java.util.*

class PKAdvertisingController: PKAdvertising {

    private val log = PKLog.get(PKAdvertisingController::class.java.simpleName)
    private var player: Player? = null
    private var messageBus: MessageBus? = null
    private var adController: AdController? = null
    private var advertising: Advertising? = null
    private var advertisingTree: AdvertisingTree? = null
    private var midRollAdsQueue: Queue<Long>? = null

    private var currentAdPositionType: AdPositionType? = null
    private var nextMidRollPositionToMonitor: Long = 0L
    private var adPlaybackTriggered: Boolean = false

    override fun playAdNow() {
        getAdFromQueue(AdPositionType.PRE_ROLL, 0)?.let {
            adController?.playAdNow(it)
        }
    }

    override fun playAdNow(vastAdTag: String) {
        TODO("Not yet implemented")
    }

    override fun getCurrentAd() {
        TODO("Not yet implemented")
    }

    fun setPlayer(player: Player, messageBus: MessageBus) {
        this.player = player
        this.messageBus = messageBus
        subscribeToAdEvents()
    }

    fun setAdController(adController: AdController) {
        this.adController = adController
    }

    fun setAdvertising(advertising: Advertising) {
        this.advertising = advertising
        adController?.advertisingConfigured(true)
        advertisingTree = AdvertisingTree(advertising)
        midRollAdsQueue = advertisingTree?.getMidRollAdvertisingQueue()
    }

    private fun subscribeToAdEvents() {
        player?.addListener(this, PlayerEvent.playheadUpdated) {
            log.d("playheadUpdated ${it.position} & nextMidRollPositionToMonitor = $nextMidRollPositionToMonitor")
            if (it.position >= nextMidRollPositionToMonitor && !adPlaybackTriggered) {
                adPlaybackTriggered = true;
                getAdFromQueue(AdPositionType.MID_ROLL, nextMidRollPositionToMonitor)?.let { adUrl ->
                    adController?.playAdNow(adUrl)
                }
            }
        }

        player?.addListener(this, PlayerEvent.ended) {
            getAdFromQueue(AdPositionType.POST_ROLL, 0)?.let {
                adController?.playAdNow(it)
            }
        }

        player?.addListener(this, AdEvent.started) {
            log.d("started")
        }

        player?.addListener(this, AdEvent.contentResumeRequested) {
            log.d("contentResumeRequested ${player?.currentPosition}")
            player?.currentPosition?.let { currentPosition ->
                if (currentPosition >= 0L) {
                    midRollAdsQueue?.poll()?.let {
                        nextMidRollPositionToMonitor = it
                        currentAdPositionType = AdPositionType.MID_ROLL
                        adPlaybackTriggered = false
                    }
                }
            }
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
            //  isCustomAdTriggered = false
            log.d("completed")
            currentAdPositionType?.let {
                setAdPodStatePlayed(it, nextMidRollPositionToMonitor)
            }
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
            log.d("error ${it}")
            currentAdPositionType?.let {
                getAdFromQueue(it, 0)?.let { adUrl ->
                    adController?.playAdNow(adUrl)
                }
            }

        }

    }

    @Nullable
    private fun getAdFromQueue(adPositionType: AdPositionType, position: Long): String? {
        currentAdPositionType = adPositionType
        var pickedAdUrl: String? = null

        advertisingTree?.let queue@{ queue ->
            when (adPositionType) {
                AdPositionType.PRE_ROLL -> {
                    queue.getPrerollAds()?.let adUrlConfig@{ adUrlConfigs ->
                        pickedAdUrl = getAdFromAdUrlConfig(adUrlConfigs)
                    }
                }

                AdPositionType.MID_ROLL -> {
                    queue.getMidRollAds()?.let { midRollAdMap ->
                        if (midRollAdMap.containsKey(position)) {
                            val adUrlConfig = midRollAdMap[position]
                            adUrlConfig?.let {
                                pickedAdUrl = getAdFromAdUrlConfig(it)
                            }
                        }
                    }
                }

                AdPositionType.POST_ROLL -> {
                    queue.getPostrollAds()?.let adUrlConfig@{ adUrlConfigs ->
                        pickedAdUrl = getAdFromAdUrlConfig(adUrlConfigs)
                    }
                }

                AdPositionType.UNKNOWN -> {
                    pickedAdUrl = null
                }
            }

        }
        log.d("getAdFromQueue $adPositionType and pickedAdUrl is $pickedAdUrl")
        return pickedAdUrl
    }

    private fun getAdFromAdUrlConfig(adUrlConfigs: AdUrlConfigs): String? {
        var adTagUrl: String? = null

        when (adUrlConfigs.adPodState) {
            AdState.LOADED -> {
                log.d("I am in Loaded State and getting the first ad Tag.")
                adUrlConfigs.adList?.let { adUrlList ->
                    adUrlConfigs.adPodState = AdState.PLAYING
                    if (adUrlList.isNotEmpty()) {
                        adTagUrl = adUrlList[0].ad
                        adUrlList[0].adState = AdState.PLAYING
                    }
                }
            }

            AdState.PLAYING -> {
                log.d("I am in Playing State and checking for the next ad Tag.")
                adUrlConfigs.adList?.let { adUrlList ->
                    for (specificAd: Ad in adUrlList) {
                        if (specificAd.adState == AdState.PLAYING || specificAd.adState == AdState.ERROR) {
                            specificAd.adState = AdState.ERROR
                        } else {
                            adTagUrl = specificAd.ad
                            specificAd.adState = AdState.PLAYING
                        }
                    }
                }
            }
        }

        return adTagUrl
    }

    private fun setAdPodStatePlayed(adPositionType: AdPositionType, position: Long) {
        log.d("setAdPodStatePlayed $adPositionType and position is $position")

        when(adPositionType) {
            AdPositionType.PRE_ROLL -> {
                advertisingTree?.getPrerollAds()?.let {
                    it.adPodState = AdState.PLAYED
                }
            }

            AdPositionType.MID_ROLL -> {
                advertisingTree?.getMidRollAds()?.let { midRollAdMap ->
                    if (midRollAdMap.containsKey(position)) {
                        val adUrlConfigs = midRollAdMap[position]
                        adUrlConfigs?.adPodState = AdState.PLAYED
                    }
                }
            }

            AdPositionType.POST_ROLL -> {
                advertisingTree?.getPostrollAds()?.let {
                    it.adPodState = AdState.PLAYED
                }
            }
        }
    }

    /* private fun getPrerollAd(): String? {
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
     }*/
}