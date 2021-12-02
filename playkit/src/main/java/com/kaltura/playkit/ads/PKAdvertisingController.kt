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

    private var DEFAULT_AD_INDEX: Int = Int.MIN_VALUE
    private var PREROLL_AD_INDEX: Int = 0
    private var POSTROLL_AD_INDEX: Int = 0

    private var currentAdBreakIndexPosition: Int = DEFAULT_AD_INDEX // For each ad Break 0, 15, 30, -1
    private var nextAdBreakIndexForMonitoring: Int = DEFAULT_AD_INDEX // For Next ad Break 0, 15, 30, -1
    private var adPlaybackTriggered: Boolean = false
    private var isPlayerSeeking: Boolean = false
    private var isPostrollLeftForPlaying: Boolean = false
    private var isAllAdsCompleted: Boolean = false
    private var isAllAdsCompletedFired: Boolean = false

    private var midrollAdBreakPositionType: AdBreakPositionType = AdBreakPositionType.POSITION
    private var midrollFrequency = Long.MIN_VALUE

    // PlayAdNow Setup
    private var isPlayAdNowTriggered = false
    private var playAdNowAdBreak: AdBreakConfig? = null

    // PlayAdsAfterTime Setup
    private var isPlayAdsAfterTimeConfigured = false
    private var playAdsAfterTime: Long = Long.MIN_VALUE

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
    fun setAdvertising(advertisingConfig: AdvertisingConfig?) {
        log.d("setAdvertising")
        resetAdvertisingConfig()
        if (advertisingConfig != null) {
            initAdvertising(advertisingConfig)
        } else {
            log.d("setAdvertising: AdvertisingConfig is null hence cleaning up existing objects.")
            destroyConfigResources()
        }
    }

    /**
     * Player configuration from KalturaPlayer
     */
    fun setPlayer(player: Player?, messageBus: MessageBus?) {
        if (player == null || messageBus == null) {
            log.d("setPlayer: Player or MessageBus is null hence cleaning up the underlying controller resources.")
            resetAdvertisingConfig()
            destroyConfigResources()
            return
        }
        log.d("setPlayer")
        this.player = player
        this.messageBus = messageBus
        subscribeToPlayerEvents()
    }

    /**
     * Initialize Advertising Config
     */
    private fun initAdvertising(advertisingConfig: AdvertisingConfig) {
        log.d("initAdvertising")

        this.advertisingConfig = advertisingConfig
        adController?.setAdvertisingConfig(true, this)
        advertisingContainer = AdvertisingContainer(this.advertisingConfig)
        adsConfigMap = advertisingContainer?.getAdsConfigMap()
        checkTypeOfMidrollAdPresent(advertisingContainer?.getMidrollAdBreakPositionType(), 0L)

        cuePointsList = advertisingContainer?.getCuePointsList()
        playAdsAfterTime = advertisingContainer?.getPlayAdsAfterTime() ?: Long.MIN_VALUE

        if (isAdsListEmpty()) {
            log.d("All Ads are empty hence clearing the underlying resources")
            resetAdvertisingConfig()
            destroyConfigResources()
            return
        }

        log.d("cuePointsList $cuePointsList")
    }

    /**
     * After the Player prepare, starting point
     * to play the Advertising
     */
    fun playAdvertising() {
        log.d("playAdvertising")

        if (isAdsListEmpty()) {
            log.d("All Ads are empty hence clearing the underlying resources")
            resetAdvertisingConfig()
            destroyConfigResources()
            return
        }

        if (hasPostRoll()) {
            cuePointsList?.let {
                log.d("Config has Postroll")
                POSTROLL_AD_INDEX = if (it.size > 1) it.size.minus(1) else 0
                nextAdBreakIndexForMonitoring = POSTROLL_AD_INDEX
            }
        }

        if (hasPreRoll()) {
            log.d("Config has Preroll")
            getPlayAdsAfterTimeConfiguration()
            if (isPlayAdsAfterTimeConfigured) {
                if (playAdsAfterTime == -1L) {
                    val preRollAdUrl = getAdFromAdConfigMap(PREROLL_AD_INDEX, false)
                    if (preRollAdUrl != null) {
                        playAd(preRollAdUrl)
                    }
                }
            } else {
                val preRollAdUrl = getAdFromAdConfigMap(PREROLL_AD_INDEX, false)
                if (preRollAdUrl != null) {
                    playAd(preRollAdUrl)
                }
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
            getPlayAdsAfterTimeConfiguration()
            // In case if there is no Preroll ad
            // prepare the content player
            prepareContentPlayer()
        }
    }

    /**
     * App may call it whenever it wants to play an AdBreak
     */
    override fun playAdNow(adBreak: AdBreak?) {
        log.d("playAdNow AdBreak is $adBreak")
        adBreak?.let adbreak@ {
            if (it.adBreakPositionType == AdBreakPositionType.EVERY || it.adBreakPositionType == AdBreakPositionType.PERCENTAGE) {
                log.e("For playAdNow, AdBreakPositionType can only be AdBreakPositionType.POSITION. Hence discarding the AdPlayback")
                return@adbreak
            }

            if (advertisingContainer == null) {
                log.d("AdvertisingContainer is null. Hence discarding the AdPlayback")
                return@adbreak
            }

            player?.let { plyr ->
                if (plyr.currentPosition <= 0) {
                    log.e("PlayAdNow API can be used once the content playback starts.")
                    return@adbreak
                }
            }

            if (adPlaybackTriggered) {
                log.e("Currently another Ad is either loading or being played, hence discarding PlayAdNow API request.")
                return@adbreak
            }

            if (it.position > 0) {
                isPlayAdNowTriggered = true
                val adPodConfig = advertisingContainer?.parseAdPodConfig(it)
                playAdNowAdBreak = AdBreakConfig(it.adBreakPositionType, it.position, AdState.READY, adPodConfig)
                val adUrl = fetchPlayableAdFromAdsList(playAdNowAdBreak, false)
                adUrl?.let { url ->
                    playAd(url)
                }
            } else {
                log.d("PlayAdNow is not a replacement of Pre-roll or Postroll AdPlayback. Hence discarding. \n " +
                        "AdBreak Position should be greater than zero.")
            }
        }
    }

    /**
     * Player Events' subscription
     */
    private fun subscribeToPlayerEvents() {
        log.d("subscribeToPlayerEvents")

        var adPlayedWithFrequency = 0
        messageBus?.addListener(this, PlayerEvent.playheadUpdated) { event ->
            cuePointsList?.let { list ->

                if (list.isEmpty()) {
                    log.d("Ads are empty, dropping ad playback.")
                    return@addListener
                }

                if (adPlaybackTriggered) {
                    log.d("Ad is playing or being loaded, dropping ad playback.")
                    return@addListener
                }

                if (!isAllAdsCompleted) {
                    log.d("nextAdBreakIndexForMonitoring = $nextAdBreakIndexForMonitoring")
                    log.d("adPlaybackTriggered = $adPlaybackTriggered")
                    log.d("event.position = ${event.position}")
                    log.d("midrollFrequency = ${midrollFrequency}")
                    log.d("(event.position > 1000L && event.position % midrollFrequency < 999L) = ${(event.position > 1000L && event.position % midrollFrequency < 1000L)}")
                }

                if ((event.position > 1000L && (event.position % midrollFrequency) < 1000L)) {
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
                    // because there is a delay while loading the ad
                    getAdFromAdConfigMap(nextAdBreakIndexForMonitoring, true)?.let { adUrl ->
                        playAd(adUrl)
                    }
                    return@let
                }

                if (!isPlayerSeeking && (midrollAdBreakPositionType != AdBreakPositionType.EVERY) && event.position >= list[nextAdBreakIndexForMonitoring] && list[nextAdBreakIndexForMonitoring] != list.last && !adPlaybackTriggered) {
                    log.d("playheadUpdated ${event.position} & nextAdIndexForMonitoring is $nextAdBreakIndexForMonitoring & nextAdForMonitoring ad position is = ${list[nextAdBreakIndexForMonitoring]}")
                    log.d("nextAdForMonitoring ad position is = $list")
                    // TODO: handle situation of player.pause or content_pause_requested
                    // because there is a delay while loading the ad
                    if (isPlayAdsAfterTimeConfigured && ((playAdsAfterTime == -1L && list[nextAdBreakIndexForMonitoring] < (event.position - 999L)) || event.position < playAdsAfterTime)) {
                        log.d("Discarding ad playback from playheadUpdated. \n" +
                                "Player position is = ${event.position} \n" +
                                "But configured isPlayAdsAfterTimeConfigured = $playAdsAfterTime is greater or equal to ${event.position}.")
                        nextAdBreakIndexForMonitoring += 1
                        return@addListener
                    }
                    getAdFromAdConfigMap(nextAdBreakIndexForMonitoring, false)?.let { adUrl ->
                        playAd(adUrl)
                    }
                }
            }
        }

        messageBus?.addListener(this, PlayerEvent.seeking) {
            log.d("Player seeking for player position = ${player?.currentPosition} - currentPosition ${it.currentPosition} - targetPosition ${it.targetPosition}" )
            isPlayerSeeking = true
        }

        messageBus?.addListener(this, PlayerEvent.loadedMetadata) {
            log.d("loadedMetadata Player duration is = ${player?.duration}" )
            checkTypeOfMidrollAdPresent(advertisingContainer?.getMidrollAdBreakPositionType(), player?.duration)
        }

        messageBus?.addListener(this, PlayerEvent.seeked) {
            if (isAllAdsCompleted) {
                log.d("Player seeked to position = ${player?.currentPosition} but All ads has completed its playback hence returning.")
                return@addListener
            }

            val seekedPosition: Long = player?.currentPosition ?: return@addListener

            cuePointsList?.let { list ->

                if (list.isEmpty()) {
                    log.d("Ads are empty, dropping ad playback.")
                    return@addListener
                }

                if (adPlaybackTriggered) {
                    log.d("Ad is playing or being loaded, dropping ad playback.")
                    return@addListener
                }

                isPlayerSeeking = false
                adPlaybackTriggered = false
                log.d("Player seeked to position = ${seekedPosition}")
                if (midrollAdBreakPositionType == AdBreakPositionType.EVERY && midrollFrequency > Long.MIN_VALUE) {
                    log.d("Because Midroll is EVERY(Frequency based) hence will not play immediate last. Will play the upcoming ad.")
                    return@addListener
                }
                if (midRollAdsCount() > 0) {
                    val lastAdPosition = getImmediateLastAdPosition(seekedPosition)
                    if (lastAdPosition > 0 || (lastAdPosition == 0 && !hasPreRoll())) {
                        log.d("Ad found on the left side of ad list")
                        getAdFromAdConfigMap(lastAdPosition, false)?.let { adUrl ->
                            playAd(adUrl)
                        }
                    } else {
                        log.d("No Ad found on the left side of ad list, finding on right side")
                        // Trying to get the immediate Next ad from pod
                        val nextAdPosition = getImmediateNextAdPosition(seekedPosition)
                        if (nextAdPosition > 0) {
                            log.d("Ad found on the right side of ad list, update the current and next ad Index")
                            nextAdBreakIndexForMonitoring = nextAdPosition
                        }
                    }
                }
            }
        }

        messageBus?.addListener(this, PlayerEvent.ended) {
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

        messageBus?.addListener(this, AdEvent.started) {
            log.d("started")
        }

        messageBus?.addListener(this, AdEvent.contentResumeRequested) {
            log.d("contentResumeRequested ${player?.currentPosition}")
        }

        messageBus?.addListener(this, AdEvent.contentPauseRequested) {
            log.d("contentPauseRequested")
        }

        messageBus?.addListener(this, AdEvent.adPlaybackInfoUpdated) {
            log.d("adPlaybackInfoUpdated")
        }

        messageBus?.addListener(this, AdEvent.skippableStateChanged) {
            log.d("skippableStateChanged")
        }

        messageBus?.addListener(this, AdEvent.adRequested) {
            log.d("adRequested")
        }

        messageBus?.addListener(this, AdEvent.playHeadChanged) {
            log.d("playHeadChanged")
        }

        messageBus?.addListener(this, AdEvent.adBreakStarted) {
            log.d("adBreakStarted")
        }

        messageBus?.addListener(this, AdEvent.cuepointsChanged) {
            log.d("cuepointsChanged")
        }

        messageBus?.addListener(this, AdEvent.loaded) {
            log.d("loaded")
        }

        messageBus?.addListener(this, AdEvent.resumed) {
            log.d("resumed")
        }

        messageBus?.addListener(this, AdEvent.paused) {
            log.d("paused")
        }

        messageBus?.addListener(this, AdEvent.skipped) {
            log.d("skipped")
        }

        messageBus?.addListener(this, AdEvent.allAdsCompleted) {
            log.d("allAdsCompleted")
        }

        messageBus?.addListener(this, AdEvent.completed) {
            //  isCustomAdTriggered = false
            log.d("AdEvent.completed")
        }

        messageBus?.addListener(this, AdEvent.firstQuartile) {
            log.d("firstQuartile")
        }

        messageBus?.addListener(this, AdEvent.midpoint) {
            log.d("midpoint")
        }

        messageBus?.addListener(this, AdEvent.thirdQuartile) {
            log.d("thirdQuartile")
        }

        messageBus?.addListener(this, AdEvent.adBreakEnded) {
            log.d("adBreakEnded")
        }

        messageBus?.addListener(this, AdEvent.adClickedEvent) {
            log.d("adClickedEvent")
        }

        messageBus?.addListener(this, AdEvent.error) {
            log.d("AdEvent.error $it")
        }
    }

    /**
     * Check of the PlayAdsAfter time is configured by the app
     */
    private fun getPlayAdsAfterTimeConfiguration() {
        if (playAdsAfterTime == -1L || playAdsAfterTime > 0L) {
            val nextAdPosition = getImmediateNextAdPosition(playAdsAfterTime)
            if (nextAdPosition > 0) {
                nextAdBreakIndexForMonitoring = nextAdPosition
            }
            log.d("playAdsAfterTime = ${playAdsAfterTime} and nextAdPosition is $nextAdPosition")
            isPlayAdsAfterTimeConfigured = true
            return
        }
        isPlayAdsAfterTimeConfigured = false
    }

    /**
     * Mapping of ALL_ADS_COMPLETED event from IMAPlugin
     */
    override fun allAdsCompleted() {
        log.d("allAdsCompleted callback")
        adPlaybackTriggered = false
        if (isPlayAdNowTriggered) {
            handlePlayAdNowPlayback(AdEvent.allAdsCompleted)
            return
        }
        changeAdState(AdState.PLAYED, AdrollType.AD)
        val adUrl = getAdFromAdConfigMap(currentAdBreakIndexPosition, false)
        if (adUrl != null) {
            playAd(adUrl)
        } else {
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
        log.d("contentResumeRequested callback and PlayerPosition is${player?.currentPosition}")
        //playContent() //TODO: Check it because after commenting it video frame between ad playback is gone
    }

    /**
     * Mapping of CONTENT_PAUSE_REQUESTED event from IMAPlugin
     */
    override fun contentPauseRequested() {
        log.d("contentPauseRequested callback")
    }

    /**
     * Mapping of AD_COMPLETED event from IMAPlugin
     */
    override fun adCompleted() {
        log.d("adCompleted callback")
    }

    /**
     * Mapping of AD_ERROR event from IMAPlugin
     */
    override fun adError(error: AdEvent.Error) {
        log.w("AdEvent.error callback $error")
        adPlaybackTriggered = false
        if (isPlayAdNowTriggered && error.error.errorType != PKAdErrorType.VIDEO_PLAY_ERROR) {
            handlePlayAdNowPlayback(AdEvent.adBreakFetchError)
            return
        }
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

    private fun handlePlayAdNowPlayback(adEventType: AdEvent.Type) {
        if (adEventType == AdEvent.allAdsCompleted) {
            changeAdBreakState(playAdNowAdBreak, AdrollType.AD, AdState.PLAYED)
            val adUrl = fetchPlayableAdFromAdsList(playAdNowAdBreak, false)
            if (adUrl != null) {
                playAd(adUrl)
            } else {
                changeAdBreakState(playAdNowAdBreak, AdrollType.ADBREAK, AdState.PLAYED)
                isPlayAdNowTriggered = false
                playAdNowAdBreak = null
                playContent()
            }
        } else if (adEventType == AdEvent.adBreakFetchError) {
            val adUrl = fetchPlayableAdFromAdsList(playAdNowAdBreak, false)
            if (adUrl.isNullOrEmpty()) {
                log.d("PlayAdNow Ad is completely errored")
                changeAdBreakState(playAdNowAdBreak , AdrollType.ADBREAK, AdState.ERROR)
                playContent()
            } else {
                log.d("Playing next waterfalling ad")
                changeAdBreakState(playAdNowAdBreak, AdrollType.ADPOD, AdState.ERROR)
                playAd(adUrl)
            }
        } else {
            playContent()
        }
    }

    /**
     * Gets the next ad from AdsConfigMap using the cuePoints list
     * Set the next ad break position to be monitored as well
     */
    @Nullable
    private fun getAdFromAdConfigMap(adIndex: Int, isTriggeredFromPlayerPosition: Boolean): String? {
        log.d("getAdFromAdConfigMap")

        if (isAllAdsCompleted) {
            log.d("All ads have completed its playback. Hence returning null from here.")
            return null
        }

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
        log.d("getAdPodConfigMap")

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
        log.d("fetchPlayableAdOnFrequency")
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
        log.d("fetchPlayableAdFromAdsList AdBreakConfig is $adBreakConfig")
        var adTagUrl: String? = null

        when (adBreakConfig?.adBreakState) {
            AdState.READY -> {
                log.d("fetchPlayableAdFromAdsList -> I am in ready State and getting the first ad Tag.")
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
                log.d("fetchPlayableAdFromAdsList -> I am in Playing State and checking for the next ad Tag.")
                adBreakConfig.adPodList?.let { adPodList ->
                    adTagUrl = getAdFromAdPod(adPodList, adBreakConfig.adBreakPositionType, isTriggeredFromPlayerPosition)
                }
            }

            AdState.PLAYED -> {
                if (isTriggeredFromPlayerPosition && adBreakConfig.adBreakPositionType == AdBreakPositionType.EVERY) {
                    log.d("fetchPlayableAdFromAdsList -> I am in Played State only for adBreakPositionType EVERY \n " +
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
        log.d("getAdFromAdPod")

        val adUrl: String? = null
        for (adPodConfig: AdPodConfig in adPodList) {
            when(adPodConfig.adPodState) {

                AdState.ERROR -> {
                    continue
                }

                AdState.READY -> {
                    log.d("getAdFromAdPod -> I am in ready State and getting the first ad Tag.")
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

                    log.d("getAdFromAdPod -> I am in Playing State and checking for the next ad Tag.")
                    adPodConfig.adList?.let { adsList ->
                        if(adsList.isNotEmpty()) {
                            for (specificAd: Ad in adsList) {
                                log.d("specificAd State ${specificAd.adState}")
                                log.d("specificAd ${specificAd.ad}")
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
        log.d("changeAdPodState AdState is $adState and AdrollType is $adrollType")
        advertisingContainer?.let { _ ->
            cuePointsList?.let { cuePointsList ->
                if (cuePointsList.isNotEmpty()) {
                    adsConfigMap?.let { adsMap ->
                        if (currentAdBreakIndexPosition != DEFAULT_AD_INDEX) {
                            val adPosition: Long = cuePointsList[currentAdBreakIndexPosition]
                            val adBreakConfig: AdBreakConfig? = adsMap[adPosition]
                            changeAdBreakState(adBreakConfig, adrollType, adState)
                        }
                    }
                }
            }
        }
        checkAllAdsArePlayed()
    }

    /**
     * Change the internal state of AdBreak, AdPod or Ad
     */
    private fun changeAdBreakState(adBreakConfig: AdBreakConfig?, adrollType: AdrollType, adState: AdState) {
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

    /**
     * Check te Midroll adbreak type
     * Act only if it is EVERY or PERCENTAGE
     * For Every case, get the midroll frequency
     * For PERCENTAGE case, update the Advertising tree data structure
     */
    private fun checkTypeOfMidrollAdPresent(adBreakPositionType: AdBreakPositionType?, playerDuration: Long?) {
        log.d("checkTypeOfMidrollAdPresent")

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
     * Resets the Advertising Config
     * */
    private fun resetAdvertisingConfig() {
        log.d("resetAdvertisingConfig")
        advertisingConfig = null
        advertisingContainer = null
        cuePointsList = null
        adsConfigMap = null
        DEFAULT_AD_INDEX = Int.MIN_VALUE
        PREROLL_AD_INDEX = 0
        POSTROLL_AD_INDEX = 0
        currentAdBreakIndexPosition = DEFAULT_AD_INDEX
        nextAdBreakIndexForMonitoring = DEFAULT_AD_INDEX
        adPlaybackTriggered = false
        isPlayerSeeking = false
        isPostrollLeftForPlaying = false
        isAllAdsCompleted = false

        midrollAdBreakPositionType = AdBreakPositionType.POSITION
        midrollFrequency = Long.MIN_VALUE

        isPlayAdNowTriggered = false
        playAdNowAdBreak = null

        isPlayAdsAfterTimeConfigured = false
        playAdsAfterTime = Long.MIN_VALUE
    }

    /**
     * Releasing the underlying resources
     */
    fun release() {
        log.d("release")
        resetAdvertisingConfig()
        destroyConfigResources()
    }

    /**
     * Destroy the Advertising Config in case if
     * config is null
     */
    private fun destroyConfigResources() {
        log.d("destroyConfigResources")
        this.player = null
        this.messageBus?.removeListeners(this)
        this.messageBus = null
        adController?.setAdvertisingConfig(false, null)
    }

    /**
     * Check if all the ads are completely played
     */
    private fun checkAllAdsArePlayed(): Boolean {
        if (isAllAdsCompleted) {
            fireAllAdsCompleteEvent()
            return true
        }

        log.d("checkAllAdsArePlayed")
        if (!hasPreRoll() && midRollAdsCount() <=0 && !hasPostRoll()) {
            isAllAdsCompleted = true
        }

        adsConfigMap?.let map@ { adsMap ->
            var unplayedAdCount = 0
            adsMap.forEach { (adBreakTime, adBreak) ->
                adBreak?.let {
                    if (midrollAdBreakPositionType == AdBreakPositionType.EVERY && midrollFrequency > Long.MIN_VALUE) {
                        isAllAdsCompleted = false
                        return@map
                    }

                    if ((adBreakTime >= 0L || adBreakTime == -1L) &&
                        (it.adBreakState == AdState.READY || it.adBreakState == AdState.PLAYING) &&
                        it.adBreakPositionType != AdBreakPositionType.EVERY) {

                        unplayedAdCount++
                    }
                }
            }
            isAllAdsCompleted = (unplayedAdCount <= 0)
            log.d("Unplayed AdCount is $unplayedAdCount")
        }

        if (isAllAdsCompleted) {
            fireAllAdsCompleteEvent()
        }

        log.d("isAllAdsCompleted $isAllAdsCompleted")
        return isAllAdsCompleted
    }

    private fun fireAllAdsCompleteEvent() {
        if (isAllAdsCompletedFired) {
            log.d("AllAdsCompleted event as already been fired.")
            return
        }
        log.d("fireAllAdsCompleteEvent")
        isAllAdsCompletedFired = true
        messageBus?.post(AdEvent(AdEvent.Type.ALL_ADS_COMPLETED))
    }
    /**
     * Trigger Postroll ad playback
     */
    private fun playPostrollAdBreak() {
        log.d("playPostrollAdBreak")
        midrollAdBreakPositionType = AdBreakPositionType.POSITION
        midrollFrequency = Long.MIN_VALUE
        getAdFromAdConfigMap(POSTROLL_AD_INDEX, false)?.let {
            playAd(it)
        }
    }

    /**
     * Check if the Ads config is empty
     */
    private fun isAdsListEmpty(): Boolean {
        if (adController == null || adsConfigMap == null) {
            log.d("AdController or AdsConfigMap is null. hence discarding ad playback")
            return true
        }

        adsConfigMap?.let {
            if (it.isEmpty()) {
                return true
            }
        }

        cuePointsList?.let {
            if (it.isEmpty()) {
                return true
            }
        }

        return false
    }

    /**
     * Ad Playback
     * Call the play Ad API on IMAPlugin
     */
    private fun playAd(adUrl: String) {
        log.d("playAd AdUrl is $adUrl")
        adPlaybackTriggered = !TextUtils.isEmpty(adUrl)
        player?.pause()
        adController?.playAdNow(adUrl)
    }

    /**
     * Content Playback
     */
    private fun playContent() {
        log.d("playContent")
        adPlaybackTriggered = false
        adController?.adControllerPreparePlayer()
        player?.play()
    }

    /**
     * Prepare content player by passing empty ad tag
     * Empty ad tag will trigger preparePlayer inside IMAPlugin
     */
    private fun prepareContentPlayer() {
        log.d("prepareContentPlayer")
        playAd("")
    }

    /**
     * Check if preRoll ad is present
     */
    private fun hasPreRoll(): Boolean {
        if (cuePointsList?.first != null) {
            return cuePointsList?.first == 0L
        }
        return false
    }

    /**
     * Check if postRoll ad is present
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
        var midrollAdsCount = 0
        cuePointsList?.let {
            if (hasPreRoll() && hasPostRoll()) {
                midrollAdsCount = it.size.minus(2)
            } else if (hasPreRoll() || hasPostRoll()) {
                midrollAdsCount = it.size.minus(1)
            } else {
                midrollAdsCount = it.size
            }
        }
        log.d("MidRollAdsCount is $midrollAdsCount")
        return midrollAdsCount
    }

    /**
     * Get the just previous ad position
     * Used only if the user seeked the Ad cue point
     * Mimicking the SNAPBACK feature
     */
    private fun getImmediateLastAdPosition(seekPosition: Long?): Int {
        log.d("getImmediateLastAdPosition")

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
        log.d("getImmediateNextAdPosition")

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

