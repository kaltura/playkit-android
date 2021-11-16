package com.kaltura.playkit.ads

import androidx.annotation.Nullable
import com.kaltura.playkit.*
import com.kaltura.playkit.plugins.ads.AdEvent
import java.util.*

/**
 * Controller to handle the Custom Ad playback
 */
class PKAdvertisingController: PKAdvertising, IMAEventsListener {

    private val log = PKLog.get(PKAdvertisingController::class.java.simpleName)
    private var player: Player? = null
    private var messageBus: MessageBus? = null
    private var adController: AdController? = null
    private var advertisingConfig: AdvertisingConfig? = null
    private var advertisingTree: AdvertisingTree? = null
    private var cuePointsList: LinkedList<Long>? = null
    private var adsConfigMap: MutableMap<Long, AdBreakConfig?>? = null

    private val DEFAULT_AD_INDEX: Int = Int.MIN_VALUE
    private val PREROLL_AD_INDEX: Int = 0
    private var POSTROLL_AD_INDEX: Int = 0

    private var currentAdBreakIndexPosition: Int = DEFAULT_AD_INDEX // For each ad Break 0, 15, 30, -1
    //private var currentAdPodIndexPosition: Int = 0 // For each ad pod 1 of 2, 2 of 2 so monitor this value
    private var nextAdBreakIndexForMonitoring: Int = DEFAULT_AD_INDEX // For Next ad Break 0, 15, 30, -1
    private var adPlaybackTriggered: Boolean = false
    private var isPlayerSeeking: Boolean = false

    /**
     * Set the AdController from PlayerLoader level
     * Need to inform IMAPlugin that Advertising is configured
     * before onUpdateMedia call
     */
    fun setAdController(adController: AdController) {
        this.adController = adController
    }

    /**
     * Set the actual advertising config object
     * and map it with our internal Advertising tree
     */
    fun setAdvertising(advertisingConfig: AdvertisingConfig) {
        this.advertisingConfig = advertisingConfig
        adController?.setAdvertisingConfig(true, this)
        advertisingTree = AdvertisingTree(advertisingConfig)
        cuePointsList = advertisingTree?.getCuePointsList()
        log.d("cuePointsList $cuePointsList")
        adsConfigMap = advertisingTree?.getAdsConfigMap()
    }

    fun setPlayer(player: Player, messageBus: MessageBus) {
        this.player = player
        this.messageBus = messageBus
        subscribeToPlayerEvents()
    }

    /**
     * After the Player prepare, starting point
     * to play the Advertising
     */
    fun playAdvertising() {
        if (hasPostRoll()) {
            POSTROLL_AD_INDEX = cuePointsList?.size?.minus(1)!!
        }

        if (hasPreRoll()) {
            val preRollAdUrl = getAdFromAdConfigMap(PREROLL_AD_INDEX)
            if (preRollAdUrl != null) {
                playAd(preRollAdUrl)
            }
        } else if (midRollAdsCount() > 0){
            cuePointsList?.let {
                if (it.isNotEmpty()) {
                    // Update next Ad index for monitoring
                    nextAdBreakIndexForMonitoring = 0
                }
            }
        }
    }

    override fun playAdNow(vastAdTag: List<AdBreak>) {
        TODO("Not yet implemented")
    }

    override fun getCurrentAd() {
        TODO("Not yet implemented")
    }

    //  15230  15800/ 1000 => 15 * 1000 => 15000 // TOD0: Check what will happen if app passed 15100 and 15500

    private fun subscribeToPlayerEvents() {
        player?.addListener(this, PlayerEvent.playheadUpdated) { event ->
            cuePointsList?.let { list ->
                if (!isPlayerSeeking && event.position >= list[nextAdBreakIndexForMonitoring] && list[nextAdBreakIndexForMonitoring] != list.last && !adPlaybackTriggered) {
                    log.d("playheadUpdated ${event.position} & nextAdIndexForMonitoring is $nextAdBreakIndexForMonitoring & nextAdForMonitoring ad position is = ${cuePointsList?.get(nextAdBreakIndexForMonitoring)}")
                    log.d("nextAdForMonitoring ad position is = $list")
                    // TODO: handle situation of player.pause or content_pause_requested
                    // (because there is a delay while loading the ad
                    adPlaybackTriggered = true
                    getAdFromAdConfigMap(nextAdBreakIndexForMonitoring)?.let { adUrl ->
                        playAd(adUrl)
                    }
                }
            }
        }

        player?.addListener(this, PlayerEvent.seeking) {
            log.d("Player seeking for player position = ${player?.currentPosition} - currentPosition ${it.currentPosition} - targetPosition ${it.targetPosition}" )
            isPlayerSeeking = true
        }

        player?.addListener(this, PlayerEvent.seeked) {
            isPlayerSeeking = false
            log.d("Player seeked for position = ${player?.currentPosition}" )
            if (midRollAdsCount() > 0) {
                val lastAdPosition = getImmediateLastAdPosition(player?.currentPosition)
                if (lastAdPosition > 0) {
                    log.d("Ad found on the left side of ad list")
                    adPlaybackTriggered = true
                    getAdFromAdConfigMap(lastAdPosition)?.let { adUrl ->
                        playAd(adUrl)
                    }
                } else {
                    log.d("No Ad found on the left side of ad list, finding on right side")
                    // Trying to get the immediate Next ad from pod
                    val nextAdPosition = getImmediateNextAdPosition(player?.currentPosition)
                    if (nextAdPosition > 0) {
                        log.d("Ad found on the right side of ad list, update the current and next ad Index")
                        nextAdBreakIndexForMonitoring = nextAdPosition
                    }
                }
            }
        }

        player?.addListener(this, PlayerEvent.ended) {
            log.d("PlayerEvent.ended came = ${player?.currentPosition}" )
            if (hasPostRoll()) {
                getAdFromAdConfigMap(POSTROLL_AD_INDEX)?.let {
                    playAd(it)
                }
            } else {
                currentAdBreakIndexPosition = DEFAULT_AD_INDEX
                nextAdBreakIndexForMonitoring = DEFAULT_AD_INDEX
                adPlaybackTriggered = false
            }
        }

        player?.addListener(this, AdEvent.started) {
            log.d("started")
        }

        player?.addListener(this, AdEvent.contentResumeRequested) {
            log.d("contentResumeRequested ${player?.currentPosition}")
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
            log.d("AdEvent.completed")
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
            log.d("AdEvent.error $it")
        }
    }

    override fun allAdsCompleted() {
        log.w("allAdsCompleted callback")
        adPlaybackTriggered = false
        changeAdState(AdState.PLAYED, AdrollType.AD)
        val adUrl = getAdFromAdConfigMap(currentAdBreakIndexPosition)
        if (adUrl != null) {
            playAd(adUrl)
        } else {
            changeAdState(AdState.PLAYED, AdrollType.ADBREAK)
            playContent()
        }
    }

    override fun contentResumeRequested() {
        log.w("contentResumeRequested callback ${player?.currentPosition}")
        playContent()
//            player?.currentPosition?.let { currentPosition ->
//                if (currentPosition >= 0L) {
//                    cuePointsList?.peek()?.let {
//                        nextAdIndexForMonitoring = it
//                        adPlaybackTriggered = false
//                    }
//                }
//            }
    }

    override fun contentPauseRequested() {
        log.w("contentPauseRequested callback")
    }

    override fun adCompleted() {
        log.w("adCompleted callback")
    }

    override fun adError(error: AdEvent.Error) {
        log.e("AdEvent.error callback $error")
        adPlaybackTriggered = false
        if (error.error.errorType != PKAdErrorType.VIDEO_PLAY_ERROR) {
            val ad = getAdFromAdConfigMap(currentAdBreakIndexPosition)
            if (ad.isNullOrEmpty()) {
                log.d("Ad is completely errored $error")
                changeAdState(AdState.ERROR, AdrollType.ADBREAK)
                playContent()
            } else {
                log.d("Playing next waterfalling ad")
                changeAdState(AdState.ERROR, AdrollType.ADPOD)
                playAd(ad)
            }
        } else {
            log.d("PKAdErrorType.VIDEO_PLAY_ERROR currentAdIndexPosition = $currentAdBreakIndexPosition")
            cuePointsList?.let { cueList ->
                val adPosition: Long = cueList[currentAdBreakIndexPosition]
                if (currentAdBreakIndexPosition < cueList.size - 1 && adPosition != -1L) {
                    // Update next Ad index for monitoring
                    nextAdBreakIndexForMonitoring = currentAdBreakIndexPosition + 1
                    log.d("nextAdIndexForMonitoring is $nextAdBreakIndexForMonitoring")
                }
            }
        }
    }

    @Nullable
    private fun getAdFromAdConfigMap(adIndex: Int): String? {
        var adUrl: String? = null
        cuePointsList?.let { cuePointsList ->
            if (cuePointsList.isNotEmpty()) {
                val adPosition: Long = cuePointsList[adIndex]
                adsConfigMap?.let { adsMap ->
                    getAdPodConfigMap(adPosition)?.let {
                        if (it.adBreakState == AdState.PLAYING || it.adBreakState == AdState.READY) {
                            adUrl = fetchPlayableAdFromAdsList(it)
                            adUrl?.let {
                                currentAdBreakIndexPosition = adIndex
                                log.d("currentAdIndexPosition is ${currentAdBreakIndexPosition}")
                                if (currentAdBreakIndexPosition < cuePointsList.size - 1 && adPosition != -1L) {
                                    // Update next Ad index for monitoring
                                    nextAdBreakIndexForMonitoring = currentAdBreakIndexPosition + 1
                                    log.d("nextAdIndexForMonitoring is $nextAdBreakIndexForMonitoring")
                                }
                            }
                        }
                    }
                }
            }
        }

        return adUrl
    }

    @Nullable
    private fun getAdPodConfigMap(position: Long?): AdBreakConfig? {
        var adBreakConfig: AdBreakConfig? = null
        advertisingTree?.let { _ ->
            adsConfigMap?.let { adsMap ->
                if (adsMap.contains(position)) {
                    adBreakConfig = adsMap[position]
                }
            }
        }

        log.d("getAdPodConfigMap AdPodConfig is $adBreakConfig and podState is ${adBreakConfig?.adBreakState}")
        return adBreakConfig
    }

    @Nullable
    private fun fetchPlayableAdFromAdsList(adBreakConfig: AdBreakConfig?): String? {
        log.d("fetchPlayableAdFromAdsList AdPodConfig position is ${adBreakConfig?.adPosition}")
        var adTagUrl: String? = null

        when (adBreakConfig?.adBreakState) {
            AdState.READY -> {
                log.i("fetchPlayableAdFromAdsList -> I am in ready State and getting the first ad Tag.")
                adBreakConfig.adPodList?.let { adPodList ->
                    adBreakConfig.adBreakState = AdState.PLAYING
                    if (adPodList.isNotEmpty()) {
                        val adsList = adPodList[0].adList
                        adsList?.let { ads ->
                            if (ads.isNotEmpty()) {
                                ads[0].adState = AdState.PLAYING
                                adPodList[0].adPodState = AdState.PLAYING
                                adTagUrl = ads[0].ad
                            }
                        }

                    }
                }
            }

            AdState.PLAYING -> {
                log.i("fetchPlayableAdFromAdsList -> I am in Playing State and checking for the next ad Tag.")
                adBreakConfig.adPodList?.let { adPodList ->
                    adTagUrl = getAdFromAdPod(adPodList)
//                    for (adPodConfig: AdPodConfig in adPodList) {
//                        log.w("specificAd State ${specificAd.adState}")
//                        log.w("specificAd ${specificAd.ad}")
//                        if (specificAd.adState == AdState.PLAYING || specificAd.adState == AdState.ERROR) {
//                            specificAd.adState = AdState.ERROR
//                        } else {
//                            adTagUrl = specificAd.ad
//                            specificAd.adState = AdState.PLAYING
//                            break
//                        }
//                    }
                }
            }
        }

        return adTagUrl
    }

    @Nullable
    private fun getAdFromAdPod(adPodList: List<AdPodConfig>): String? {
        var adUrl: String? = null

        for (adPodConfig: AdPodConfig in adPodList) {
            when(adPodConfig.adPodState) {

                AdState.ERROR, AdState.PLAYED -> {
                    continue
                }

                AdState.READY -> {
                    log.i("getAdFromAdPod -> I am in ready State and getting the first ad Tag.")
                    adPodConfig.adList?.let {
                        if(it.isNotEmpty()) {
                            it[0].adState = AdState.PLAYING
                            adPodConfig.adPodState = AdState.PLAYING
                            return it[0].ad
                        }
                    }
                }

                AdState.PLAYING -> {
                    log.i("getAdFromAdPod -> I am in Playing State and checking for the next ad Tag.")
                    adPodConfig.adList?.let { adsList ->
                        if(adsList.isNotEmpty()) {
                            for (specificAd: Ad in adsList) {
                                log.w("specificAd State ${specificAd.adState}")
                                log.w("specificAd ${specificAd.ad}")
                                when (specificAd.adState) {
                                    AdState.ERROR, AdState.PLAYED -> continue

                                    AdState.READY -> {
                                        adPodConfig.adPodState = AdState.PLAYING
                                        specificAd.adState = AdState.PLAYING
                                        return specificAd.ad
                                    }

                                    AdState.PLAYING -> {
                                        specificAd.adState = AdState.ERROR
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return adUrl
    }

    private fun changeAdState(adState: AdState, adrollType: AdrollType) {
        log.d("changeAdPodState AdState is $adState")
        advertisingTree?.let { _ ->
            cuePointsList?.let { cuePointsList ->
                if (cuePointsList.isNotEmpty()) {
                    adsConfigMap?.let { adsMap ->
                        if (currentAdBreakIndexPosition != DEFAULT_AD_INDEX) {
                            val adPosition: Long = cuePointsList[currentAdBreakIndexPosition]
                            val adBreakConfig: AdBreakConfig? = adsMap[adPosition]
                            adBreakConfig?.let { adBreak ->
                                log.d("AdState is changed for AdPod position ${adBreak.adPosition}")
                                //TODO: Change internal ad index state which eventually was played after waterfalling
                                if (adrollType == AdrollType.ADBREAK) {
                                    adBreak.adBreakState = adState
                                }

                                adBreak.adPodList?.forEach {
                                    if (adrollType == AdrollType.ADBREAK && it.adPodState == AdState.PLAYING) {
                                        it.adPodState = adState
                                    }
                                    var isAdPodCompletelyErrored = 0
                                    it.adList?.forEach { ad ->
                                        if (adrollType == AdrollType.AD && ad.adState == AdState.PLAYING) {
                                            if (ad.adState != AdState.ERROR) {
                                                it.adPodState = adState
                                            }
                                            ad.adState = adState
                                        }
                                        if (ad.adState != AdState.ERROR) {
                                            isAdPodCompletelyErrored++
                                        }
                                    }
                                    if (isAdPodCompletelyErrored == 0) {
                                        it.adPodState = AdState.ERROR
                                    }
                                }
                                // cuePointsList.remove(adPosition)
                                // currentAdIndexPosition = DEFAULT_AD_INDEX
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Ad Playback
     * Call the play Ad API on IMAPlugin
     */
    private fun playAd(adUrl: String) {
        player?.pause()
        adController?.playAdNow(adUrl)
    }

    /**
     * Content Playback
     */
    private fun playContent() {
        player?.play()
    }

    /**
     * Check is preRoll ad is present
     */
    private fun hasPreRoll(): Boolean {
        return cuePointsList?.first == 0L
    }

    /**
     * Check is postRoll ad is present
     */
    private fun hasPostRoll(): Boolean {
        return cuePointsList?.last == -1L
    }

    /**
     * Get the number of midRolls,
     * if no midRoll is present count will be zero.
     */
    private fun midRollAdsCount(): Int {
        cuePointsList?.let {
            return if (hasPreRoll() && hasPostRoll()) {
                it.size.minus(2)
            } else if (hasPreRoll() || hasPostRoll()) {
                it.size.minus(1)
            } else {
                it.size
            }
        }
        return 0
    }

    private fun getImmediateLastAdPosition(seekPosition: Long?): Int {
        if (seekPosition == null || cuePointsList == null || cuePointsList.isNullOrEmpty()) {
            log.d("Error in getImmediateLastAdPosition returning DEFAULT_AD_POSITION")
            return DEFAULT_AD_INDEX
        }

        var adPosition = -1

        cuePointsList?.let {
            if (seekPosition > 0 && it.isNotEmpty() && it.size > 1) {
                var lowerIndex: Int = if (it.first == 0L) 1 else 0
                var upperIndex: Int = if (it.last == -1L) it.size -2 else (it.size - 1)

                while (lowerIndex <= upperIndex) {
                    val midIndex = lowerIndex + (upperIndex - lowerIndex) / 2

                    if (it[midIndex] == seekPosition) {
                        adPosition = midIndex
                        break
                    } else if (it[midIndex] < seekPosition) {
                        adPosition = midIndex
                        lowerIndex = midIndex + 1
                    } else if (it[midIndex] > seekPosition) {
                        upperIndex = midIndex - 1
                    }
                }
            }
        }

        log.d("Immediate Last Ad Position ${adPosition}")
        return adPosition
    }

    private fun getImmediateNextAdPosition(seekPosition: Long?): Int {
        if (seekPosition == null || cuePointsList == null || cuePointsList.isNullOrEmpty()) {
            log.d("Error in getImmediateNextAdPosition returning DEFAULT_AD_POSITION")
            return DEFAULT_AD_INDEX
        }

        var adPosition = -1

        cuePointsList?.let {
            if (seekPosition > 0 && it.isNotEmpty() && it.size > 1) {
                var lowerIndex: Int = if (it.first == 0L) 1 else 0
                var upperIndex: Int = if (it.last == -1L) it.size -2 else (it.size - 1)

                while (lowerIndex <= upperIndex) {
                    val midIndex = lowerIndex + (upperIndex - lowerIndex) / 2

                    if (it[midIndex] == seekPosition) {
                        adPosition = midIndex
                        break
                    } else if (it[midIndex] < seekPosition) {
                        lowerIndex = midIndex + 1
                    } else if (it[midIndex] > seekPosition) {
                        adPosition = midIndex
                        upperIndex = midIndex - 1
                    }
                }
            }
        }

        log.d("Immediate Next Ad Position ${adPosition}")
        return adPosition
    }

    private fun checkAllAdsArePlayed(): Boolean {
        var isAllAdsPlayed = true
        if (hasPreRoll() && midRollAdsCount() <=0 && !hasPostRoll()) {
            return true
        }

        adsConfigMap?.let {
            it.forEach { (key, value) ->
                if (key > 0L && (value?.adBreakState != AdState.PLAYED || value.adBreakState != AdState.ERROR)) {
                    isAllAdsPlayed = false
                }
            }
        }
        return isAllAdsPlayed
    }
}