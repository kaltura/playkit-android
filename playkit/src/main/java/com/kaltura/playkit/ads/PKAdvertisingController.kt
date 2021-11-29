package com.kaltura.playkit.ads

import android.text.TextUtils
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
    private var advertisingContainer: AdvertisingContainer? = null

    // List containing the Ads' position (Sorted list, Preroll is moved to 0th position and Postoll, is moved to the last)
    private var cuePointsList: LinkedList<Long>? = null
    // Map containg the actual ads with position as key in the Map (Insertion order of app maintained)
    private var adsConfigMap: MutableMap<Long, AdBreakConfig?>? = null

    private val DEFAULT_AD_INDEX: Int = Int.MIN_VALUE
    private val PREROLL_AD_INDEX: Int = 0
    private var POSTROLL_AD_INDEX: Int = 0

    private var currentAdBreakIndexPosition: Int = DEFAULT_AD_INDEX // For each ad Break 0, 15, 30, -1
    private var nextAdBreakIndexForMonitoring: Int = DEFAULT_AD_INDEX // For Next ad Break 0, 15, 30, -1
    private var adPlaybackTriggered: Boolean = false
    private var isPlayerSeeking: Boolean = false
    private var isPostrollLeftForPlaying = false

    private var midrollAdBreakPositionType: AdBreakPositionType = AdBreakPositionType.POSITION
    private var midrollFrequency = Long.MIN_VALUE

    /**
     * Player configuration from KalturaPlayer
     */
    fun setPlayer(player: Player, messageBus: MessageBus) {
        this.player = player
        this.messageBus = messageBus
        subscribeToPlayerEvents()
    }

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
        advertisingContainer = AdvertisingContainer(advertisingConfig)
        adsConfigMap = advertisingContainer?.getAdsConfigMap()
        checkTypeOfMidrollAdPresent(advertisingContainer?.getMidrollAdBreakPositionType(), 0L)
        cuePointsList = advertisingContainer?.getCuePointsList()
        log.d("cuePointsList $cuePointsList")
    }

    /**
     * After the Player prepare, starting point
     * to play the Advertising
     */
    fun playAdvertising() {
        if (hasPostRoll()) {
            cuePointsList?.let {
                POSTROLL_AD_INDEX = if (it.size > 1) it.size.minus(1) else 0
                nextAdBreakIndexForMonitoring = POSTROLL_AD_INDEX
            }
        }

        if (hasPreRoll()) {
            val preRollAdUrl = getAdFromAdConfigMap(PREROLL_AD_INDEX, false)
            if (preRollAdUrl != null) {
                playAd(preRollAdUrl)
            }
        } else {
            if (midRollAdsCount() > 0) {
                cuePointsList?.let {
                    if (it.isNotEmpty()) {
                        // Update next Ad index for monitoring
                        nextAdBreakIndexForMonitoring = 0
                    }
                }
            }
            // In case if there is no Preroll ad
            // prepare the content player
            prepareContentPlayer()
        }
    }

    override fun playAdNow(vastAdTag: List<AdBreak>) {
        TODO("Not yet implemented")
    }

    override fun getCurrentAd() {
        TODO("Not yet implemented")
    }

    /**
     * Player Events' subscription
     */
    private fun subscribeToPlayerEvents() {
        var adPlayedWithFrequency = 0
        player?.addListener(this, PlayerEvent.playheadUpdated) { event ->
            cuePointsList?.let { list ->

                log.d("nextAdBreakIndexForMonitoring = $nextAdBreakIndexForMonitoring")
                log.d("adPlaybackTriggered = $adPlaybackTriggered")
                log.d("Gourav event.position = ${event.position}")
                log.d("Gourav midrollFrequency = ${midrollFrequency}")
                log.d("Gourav (event.position > 1000L && event.position % midrollFrequency < 999L) = ${(event.position > 1000L && event.position % midrollFrequency < 1000L)}")

                if ((event.position > 1000L && event.position % midrollFrequency < 1000L)) {
                    if (adPlayedWithFrequency < 3) {
                        adPlayedWithFrequency++
                    }
                } else {
                    adPlayedWithFrequency = 0
                }

                if (!isPlayerSeeking && (midrollAdBreakPositionType == AdBreakPositionType.EVERY) && midrollFrequency > Long.MIN_VALUE && adPlayedWithFrequency == 1 && list[nextAdBreakIndexForMonitoring] != list.last && !adPlaybackTriggered) {
                    log.d("playheadUpdated ${event.position} & nextAdIndexForMonitoring is $nextAdBreakIndexForMonitoring & nextAdForMonitoring ad position is = ${cuePointsList?.get(nextAdBreakIndexForMonitoring)}")
                    log.d("nextAdForMonitoring ad position is = $list")
                    // TODO: handle situation of player.pause or content_pause_requested
                    // (because there is a delay while loading the ad
                    getAdFromAdConfigMap(nextAdBreakIndexForMonitoring, true)?.let { adUrl ->
                        playAd(adUrl)
                    }
                    return@let
                }

                if (!isPlayerSeeking && (midrollAdBreakPositionType != AdBreakPositionType.EVERY) && event.position >= list[nextAdBreakIndexForMonitoring] && list[nextAdBreakIndexForMonitoring] != list.last && !adPlaybackTriggered) {
                    log.d("playheadUpdated ${event.position} & nextAdIndexForMonitoring is $nextAdBreakIndexForMonitoring & nextAdForMonitoring ad position is = ${cuePointsList?.get(nextAdBreakIndexForMonitoring)}")
                    log.d("nextAdForMonitoring ad position is = $list")
                    // TODO: handle situation of player.pause or content_pause_requested
                    // (because there is a delay while loading the ad
                    getAdFromAdConfigMap(nextAdBreakIndexForMonitoring, false)?.let { adUrl ->
                        playAd(adUrl)
                    }
                }
            }
        }

        player?.addListener(this, PlayerEvent.seeking) {
            log.d("Player seeking for player position = ${player?.currentPosition} - currentPosition ${it.currentPosition} - targetPosition ${it.targetPosition}" )
            isPlayerSeeking = true
        }

        player?.addListener(this, PlayerEvent.loadedMetadata) {
            log.d("loadedMetadata Player duration is = ${player?.duration}" )
            checkTypeOfMidrollAdPresent(advertisingContainer?.getMidrollAdBreakPositionType(), player?.duration)
        }

        player?.addListener(this, PlayerEvent.seeked) {
            isPlayerSeeking = false
            adPlaybackTriggered = false
            log.d("Player seeked for position = ${player?.currentPosition}" )
            if (midRollAdsCount() > 0) {
                val lastAdPosition = getImmediateLastAdPosition(player?.currentPosition)
                if (lastAdPosition > 0) {
                    log.d("Ad found on the left side of ad list")
                    getAdFromAdConfigMap(lastAdPosition, false)?.let { adUrl ->
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
                if (!adPlaybackTriggered) {
                    playPostrollAdBreak()
                } else {
                    isPostrollLeftForPlaying = true
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

    /**
     * Mapping of ALL_ADS_COMPLETED event from IMAPlugin
     */
    override fun allAdsCompleted() {
        log.w("allAdsCompleted callback")
        adPlaybackTriggered = false
        changeAdState(AdState.PLAYED, AdrollType.AD)
        val adUrl = getAdFromAdConfigMap(currentAdBreakIndexPosition, false)
        if (adUrl != null) {
            playAd(adUrl)
        } else {
            adController?.adControllerPreparePlayer()
            changeAdState(AdState.PLAYED, AdrollType.ADBREAK)
            playContent()
        }

        if (isPostrollLeftForPlaying) {
            playPostrollAdBreak()
        }
        isPostrollLeftForPlaying = false
    }

    /**
     * Mapping of CONTENT_RESUME_REQUESTED event from IMAPlugin
     */
    override fun contentResumeRequested() {
        log.w("contentResumeRequested callback ${player?.currentPosition}")
        playContent()
    }

    /**
     * Mapping of CONTENT_PAUSE_REQUESTED event from IMAPlugin
     */
    override fun contentPauseRequested() {
        log.w("contentPauseRequested callback")
    }

    /**
     * Mapping of AD_COMPLETED event from IMAPlugin
     */
    override fun adCompleted() {
        log.w("adCompleted callback")
    }

    /**
     * Mapping of AD_ERROR event from IMAPlugin
     */
    override fun adError(error: AdEvent.Error) {
        log.e("AdEvent.error callback $error")
        adPlaybackTriggered = false
        if (error.error.errorType != PKAdErrorType.VIDEO_PLAY_ERROR) {
            val ad = getAdFromAdConfigMap(currentAdBreakIndexPosition, false)
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
            playContent()
        }
    }

    /**
     * Gets the next ad from AdsConfigMap using the cuePoints list
     * Set the next ad break position to be monitored as well
     */
    @Nullable
    private fun getAdFromAdConfigMap(adIndex: Int, isTriggeredFromPlayerPosition: Boolean): String? {
        var adUrl: String? = null
        cuePointsList?.let { cuePointsList ->
            if (cuePointsList.isNotEmpty()) {
                val adPosition: Long = cuePointsList[adIndex]
                adsConfigMap?.let { adsMap ->
                    getAdPodConfigMap(adPosition)?.let {
                        if (it.adBreakPositionType == AdBreakPositionType.EVERY) {
                            adUrl = fetchPlayableAdOnFrequency(it, isTriggeredFromPlayerPosition)
                            adUrl?.let {
                                currentAdBreakIndexPosition = adIndex
                            }
                            return adUrl
                        }

                        if ((it.adBreakState == AdState.PLAYING || it.adBreakState == AdState.READY) && it.adBreakPositionType != AdBreakPositionType.EVERY) {
                            adUrl = fetchPlayableAdFromAdsList(it, isTriggeredFromPlayerPosition)
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

    /**
     * Get the specific AdBreakConfig by position
     */
    @Nullable
    private fun getAdPodConfigMap(position: Long?): AdBreakConfig? {
        var adBreakConfig: AdBreakConfig? = null
        advertisingContainer?.let { _ ->
            adsConfigMap?.let { adsMap ->
                if (adsMap.contains(position)) {
                    adBreakConfig = adsMap[position]
                }
            }
        }

        log.d("getAdPodConfigMap AdPodConfig is $adBreakConfig and podState is ${adBreakConfig?.adBreakState}")
        return adBreakConfig
    }

    /**
     * In case if the AdBreakPositionType is EVERY
     */
    @Nullable
    private fun fetchPlayableAdOnFrequency(adBreakConfig: AdBreakConfig?, isTriggeredFromPlayerPosition: Boolean): String? {
        if (midRollAdsCount() > 0) {
            return fetchPlayableAdFromAdsList(adBreakConfig, isTriggeredFromPlayerPosition)
        }
        return null
    }

    /**
     * Check the AdBreakConfig state and get the AdPod accordingly
     */
    @Nullable
    private fun fetchPlayableAdFromAdsList(adBreakConfig: AdBreakConfig?, isTriggeredFromPlayerPosition: Boolean): String? {
        log.e("Gourav fetchPlayableAdFromAdsList AdPodConfig position is ${adBreakConfig}")
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
                    adTagUrl = getAdFromAdPod(adPodList, adBreakConfig.adBreakPositionType, isTriggeredFromPlayerPosition)
                }
            }

            AdState.PLAYED -> {
                if (isTriggeredFromPlayerPosition && adBreakConfig.adBreakPositionType == AdBreakPositionType.EVERY) {
                    log.i("fetchPlayableAdFromAdsList -> I am in Played State only for adBreakPositionType EVERY \n " +
                            "and checking for the PLAYED ad Tag.")
                    adBreakConfig.adPodList?.let { adPodList ->
                        adTagUrl = getAdFromAdPod(adPodList, adBreakConfig.adBreakPositionType, isTriggeredFromPlayerPosition)
                    }
                }
            }
        }

        return adTagUrl
    }

    /**
     * Check the AdPodConfig state and get the Ad accordingly
     */

    @Nullable
    private fun getAdFromAdPod(adPodList: List<AdPodConfig>, adBreakPositionType: AdBreakPositionType, isTriggeredFromPlayerPosition: Boolean): String? {
        var adUrl: String? = null

        for (adPodConfig: AdPodConfig in adPodList) {
            when(adPodConfig.adPodState) {

                AdState.ERROR -> {
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

                AdState.PLAYING, AdState.PLAYED -> {
                    if (adPodConfig.adPodState == AdState.PLAYED)  {
                        if (adBreakPositionType != AdBreakPositionType.EVERY && !isTriggeredFromPlayerPosition) {
                            continue
                        }
                    }

                    log.i("getAdFromAdPod -> I am in Playing State and checking for the next ad Tag.")
                    adPodConfig.adList?.let { adsList ->
                        if(adsList.isNotEmpty()) {
                            for (specificAd: Ad in adsList) {
                                log.w("specificAd State ${specificAd.adState}")
                                log.w("specificAd ${specificAd.ad}")
                                when (specificAd.adState) {
                                    AdState.ERROR -> continue

                                    AdState.PLAYED -> {
                                        if (adBreakPositionType == AdBreakPositionType.EVERY && isTriggeredFromPlayerPosition) {
                                            return specificAd.ad
                                        } else {
                                            continue
                                        }
                                    }

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
        //TODO: Check te case of bg/fg, calling, network on/off
        return adUrl
    }

    /**
     * After each successful or error ad playback,
     * Change the AdBreak, AdPod OR Ad state accordingly
     */
    private fun changeAdState(adState: AdState, adrollType: AdrollType) {
        log.d("changeAdPodState AdState is $adState")
        advertisingContainer?.let { _ ->
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
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Check te Midroll adbreak type
     * Act only if it is EVERY or PERCENTAGE
     * For Every case, get the midroll frequency
     * For PERCENTAGE case, update the Advertising tree data structure
     */
    private fun checkTypeOfMidrollAdPresent(adBreakPositionType: AdBreakPositionType?, playerDuration: Long?) {
        when(adBreakPositionType) {
            AdBreakPositionType.EVERY -> {
                midrollAdBreakPositionType = adBreakPositionType
                midrollFrequency = advertisingContainer?.getMidrollFrequency() ?: Long.MIN_VALUE
            }

            AdBreakPositionType.PERCENTAGE -> {
                playerDuration?.let {
                    if (it > 0) {
                        advertisingContainer?.updatePercentageBasedPosition(playerDuration)
                        adsConfigMap = advertisingContainer?.getAdsConfigMap()
                        cuePointsList = advertisingContainer?.getCuePointsList()
                        log.d("Updated cuePointsList for PERCENTAGE based Midrolls $cuePointsList")
                    }
                }
            }
            else -> return
        }
    }

    /**
     * TODO: USE this method
     */
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

    /**
     * Trigger Postroll ad playback
     */
    private fun playPostrollAdBreak() {
        getAdFromAdConfigMap(POSTROLL_AD_INDEX, false)?.let {
            playAd(it)
        }
    }

    /**
     * Ad Playback
     * Call the play Ad API on IMAPlugin
     */
    private fun playAd(adUrl: String) {
        adPlaybackTriggered = !TextUtils.isEmpty(adUrl)
        player?.pause()
        adController?.playAdNow(adUrl)
    }

    /**
     * Content Playback
     */
    private fun playContent() {
        adPlaybackTriggered = false
        player?.play()
    }

    /**
     * Prepare content player by passing empty ad tag
     * Empty ad tag will trigger preparePlayer inside IMAPlugin
     */
    private fun prepareContentPlayer() {
        playAd("")
    }

    /**
     * Check is preRoll ad is present
     */
    private fun hasPreRoll(): Boolean {
        if (cuePointsList?.first != null) {
            return cuePointsList?.first == 0L
        }
        return false
    }

    /**
     * Check is postRoll ad is present
     */
    private fun hasPostRoll(): Boolean {
        if (cuePointsList?.last != null) {
            return cuePointsList?.last == -1L
        }
        return false
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

    /**
     * Get the just previous ad position
     * Used only if the user seeked the Ad cue point
     * Mimicking the SNAPBACK feature
     */
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

    /**
     * Get the just next ad position
     * Used only if the user seeked the Ad cue point
     * Mimicking the SNAPBACK feature
     */
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
}
