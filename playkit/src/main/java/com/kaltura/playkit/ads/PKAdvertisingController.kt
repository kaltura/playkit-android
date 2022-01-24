package com.kaltura.playkit.ads

import android.text.TextUtils
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.kaltura.playkit.*
import com.kaltura.playkit.plugins.ads.AdEvent
import com.kaltura.playkit.utils.Consts
import java.util.*

/**
 * Controller to handle the Custom Ad playback
 */
class PKAdvertisingController: PKAdvertising, IMAEventsListener {

    private val log = PKLog.get(PKAdvertisingController::class.java.simpleName)
    private var player: Player? = null
    private var messageBus: MessageBus? = null
    private var mediaConfig: PKMediaConfig? = null
    private var adController: AdvertisingController? = null
    private var advertisingConfig: AdvertisingConfig? = null
    private var advertisingContainer: AdvertisingContainer? = null

    // List containing the Ads' position (Sorted list, Preroll is moved to 0th position and Postoll, is moved to the last)
    private var cuePointsList: LinkedList<Long>? = null
    // Map containing the actual ads with position as key in the Map (Insertion order of app maintained)
    private var adsConfigMap: MutableMap<Long, AdBreakConfig?>? = null

    private var playerStartPosition: Long = 0L
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

    // AdWaterfalling indication
    private var hasWaterFallingAds = false

    // AdType
    private var adType: AdType = AdType.AD_URL

    /**
     * Set the AdController from PlayerLoader level
     * Need to inform IMAPlugin that Advertising is configured
     * before onUpdateMedia call
     */
    fun setAdController(adController: AdvertisingController) {
        log.d("setAdController")
        this.adController = adController
    }

    /**
     * Set the actual advertising config object
     * and map it with our internal Advertising tree
     */
    fun setAdvertising(@Nullable advertisingConfig: AdvertisingConfig?) {
        log.d("setAdvertising")
        advertisingContainer = null
        resetAdvertisingConfig()
        advertisingContainer = AdvertisingContainer()

        if (advertisingConfig != null) {
            initAdvertising(advertisingConfig)
        } else {
            log.d("setAdvertising: AdvertisingConfig is null")
        }
    }

    /**
     * Player configuration from KalturaPlayer
     */
    fun setPlayer(player: Player?, messageBus: MessageBus?, mediaConfig: PKMediaConfig) {
        if (player == null || messageBus == null) {
            log.d("setPlayer: Player or MessageBus is null hence cleaning up the underlying controller resources.")
            release()
            return
        }
        log.d("setPlayer")
        this.player = player
        this.messageBus = messageBus
        this.mediaConfig = mediaConfig
        subscribeToPlayerEvents()
    }

    /**
     * Initialize Advertising Config
     */
    private fun initAdvertising(advertisingConfig: AdvertisingConfig) {
        log.d("initAdvertising")

        this.advertisingConfig = advertisingConfig
        advertisingContainer?.setAdvertisingConfig(this.advertisingConfig)
        adType = advertisingContainer?.getAdType() ?: AdType.AD_URL
        adController?.setAdvertisingConfig(true, adType, this)
        adsConfigMap = advertisingContainer?.getAdsConfigMap()
        checkTypeOfMidrollAdPresent(advertisingContainer?.getMidrollAdBreakPositionType(), 0L)

        cuePointsList = advertisingContainer?.getCuePointsList()
        adController?.advertisingSetCuePoints(cuePointsList)
        playAdsAfterTime = advertisingContainer?.getPlayAdsAfterTime() ?: Long.MIN_VALUE

        if (isAdsListEmpty()) {
            log.d("All Ads are empty hence clearing the underlying resources")
            resetAdvertisingConfig()
            return
        }

        log.d("cuePointsList $cuePointsList")
    }

    /**
     * After the Player prepare, starting point
     * to play the Advertising
     */
    fun loadAdvertising(@Nullable startPosition: Long?) {
        log.d("loadAdvertising")

        if (isAdsListEmpty()) {
            log.d("All Ads are empty hence clearing the underlying resources")
            resetAdvertisingConfig()
            return
        }

        if (hasPostRoll()) {
            cuePointsList?.let {
                log.d("Config has Postroll")
                POSTROLL_AD_INDEX = if (it.size > 1) it.size.minus(1) else 0
                nextAdBreakIndexForMonitoring = POSTROLL_AD_INDEX
            }
        }

        if (startPosition != null && startPosition > 0L) {
            playerStartPosition = startPosition *  Consts.MILLISECONDS_MULTIPLIER
        }

        if (hasPreRoll()) {
            log.d("Config has Preroll")
            getPlayAdsAfterTimeConfiguration()
            if (isPlayAdsAfterTimeConfigured) {
                if (playAdsAfterTime == -1L) {
                    val preRollAdUrl = getAdFromAdConfigMap(PREROLL_AD_INDEX)
                    // Case when preroll is there then play preroll and then play immediate last ad as per player start position
                    if (preRollAdUrl != null) {
                        playAd(preRollAdUrl)
                        if (playerStartPosition > 0 && midrollAdBreakPositionType != AdBreakPositionType.EVERY) {
                            //Get the immediate last ad and play once preroll completes
                            nextAdBreakIndexForMonitoring = getImmediateLastAdPosition(playerStartPosition)
                        }
                    } else {
                        prepareContentPlayer()
                    }
                } else {
                    if (midRollAdsCount() > 0 && playAdsAfterTime < playerStartPosition) {
                        // If playAdsAfterTime is smaller than playerStartPosition then play the immediate last ad from the map as per startposition
                        // It will skip the preroll
                        nextAdBreakIndexForMonitoring = getImmediateLastAdPosition(playerStartPosition)
                        if (nextAdBreakIndexForMonitoring > 0) {
                            val playAbleAdUrl = getAdFromAdConfigMap(nextAdBreakIndexForMonitoring)
                            if (playAbleAdUrl != null) {
                                playAd(playAbleAdUrl)
                            }
                        } else {
                            prepareContentPlayer()
                        }
                    } else {
                        // If playAdsAfterTime is greater than startPosition then prepare the content player
                        prepareContentPlayer()
                    }
                }
            } else {
                if (playerStartPosition > 0L) {
                    nextAdBreakIndexForMonitoring = getImmediateNextAdPosition(playerStartPosition)
                    prepareContentPlayer()
                } else {
                    val preRollAdUrl = getAdFromAdConfigMap(PREROLL_AD_INDEX)
                    if (preRollAdUrl != null) {
                        playAd(preRollAdUrl)
                    } else {
                        prepareContentPlayer()
                    }
                }
            }
        } else {
            if (midRollAdsCount() > 0) {
                log.d("Config has Midroll/s")
                cuePointsList?.let {
                    if (it.isNotEmpty()) {
                        // Update next Ad index for monitoring
                        if (playerStartPosition > 0L) {
                            nextAdBreakIndexForMonitoring = getImmediateNextAdPosition(playerStartPosition)
                        } else {
                            nextAdBreakIndexForMonitoring = 0
                        }
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
    override fun playAdNow(adBreak: Any?, @NonNull adType: AdType) {
        log.d("playAdNow AdBreak is $adBreak")

        var parsedAdBreak: AdBreak? = null

        adBreak?.let {
            parsedAdBreak = if (it is String) {
                advertisingContainer?.parseAdBreakGSON(it)
            } else if (it is AdBreak) {
                it
            } else {
                log.e("Malformed AdBreak Input. PlayAdNow API either support AdBreak Object or AdBreak JSON. Hence returning.")
                return
            }
        }

        parsedAdBreak?.let parsedAdbreak@ {
            if (advertisingContainer == null) {
                log.d("AdvertisingContainer is null. Hence discarding the AdPlayback")
                return@parsedAdbreak
            }

            player?.let { plyr ->
                if (plyr.currentPosition <= 0) {
                    log.e("PlayAdNow API can be used once the content playback starts.")
                    return@parsedAdbreak
                }
            }

            if (adPlaybackTriggered) {
                log.e("Currently another Ad is either loading or being played, hence discarding PlayAdNow API request.")
                return@parsedAdbreak
            }

            if (it.adBreakPositionType == AdBreakPositionType.EVERY || it.adBreakPositionType == AdBreakPositionType.PERCENTAGE) {
                log.e("For playAdNow, AdBreakPositionType can only be AdBreakPositionType.POSITION. Hence discarding the AdPlayback")
                return@parsedAdbreak
            }

            if (it.position > 0) {
                isPlayAdNowTriggered = true
                val adPodConfig = advertisingContainer?.parseAdPodConfig(it)
                playAdNowAdBreak = AdBreakConfig(it.adBreakPositionType, 0L, AdState.READY, adPodConfig)
                val adUrl = fetchPlayableAdFromAdsList(playAdNowAdBreak, false)
                adUrl?.let { url ->
                    log.d("playAdNow")
                    adController?.setAdvertisingConfig(true, adType, this)
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
        var triggerAdPlaybackCounter = 0

        messageBus?.addListener(this, PlayerEvent.playheadUpdated) { event ->

            if (isAllAdsCompletedFired) {
                log.d("All ads has completed its playback hence returning.")
                return@addListener
            }

            if (isLiveMedia()) {
                log.v("PlayheadUpdated Event. For Live Medias only Preroll ad will be played. Hence dropping other Ads if configured.")
                return@addListener
            }

            cuePointsList?.let { list ->

                if (list.isEmpty() || nextAdBreakIndexForMonitoring == DEFAULT_AD_INDEX) {
                    log.v("playheadUpdated: Ads are empty, dropping ad playback.")
                    return@addListener
                }

                if (adPlaybackTriggered) {
                    log.d("playheadUpdated: Ad is playing or being loaded, dropping ad playback.")
                    return@addListener
                }

                if (hasOnlyPreRoll()) {
                    log.v("playheadUpdated: Only Preroll ad is available hence returning from here.")
                    return@addListener
                }

                if (!isAllAdsCompleted) {
                    log.v("nextAdBreakIndexForMonitoring = $nextAdBreakIndexForMonitoring")
                    log.v("adPlaybackTriggered = $adPlaybackTriggered")
                    log.v("event.position = ${event.position}")
                    log.v("midrollFrequency = ${midrollFrequency}")
                    log.v("midrollAdBreakPositionType = ${midrollAdBreakPositionType}")
                }

                if (!isPlayAdsAfterTimeConfigured && playerStartPosition > 0 && event.position < playerStartPosition) {
                    return@addListener
                }

                if (nextAdBreakIndexForMonitoring < 0 || nextAdBreakIndexForMonitoring >= list.size) {
                    log.d("playheadUpdated: Invalid nextAdBreakIndexForMonitoring")
                    return@addListener
                }

                if (midrollAdBreakPositionType == AdBreakPositionType.EVERY) {
                    if (midrollFrequency > Long.MIN_VALUE &&
                        event.position > Consts.MILLISECONDS_MULTIPLIER &&
                        ((event.position % midrollFrequency) < Consts.MILLISECONDS_MULTIPLIER)) {

                        triggerAdPlaybackCounter++
                    } else {
                        triggerAdPlaybackCounter = 0
                    }
                } else {
                    if (list[nextAdBreakIndexForMonitoring] > 0 &&
                        event.position >= list[nextAdBreakIndexForMonitoring] &&
                        (event.position > Consts.MILLISECONDS_MULTIPLIER &&
                                (event.position % list[nextAdBreakIndexForMonitoring]) < Consts.MILLISECONDS_MULTIPLIER)) {

                        triggerAdPlaybackCounter++
                    } else {
                        triggerAdPlaybackCounter = 0
                    }
                }

                if (!isPlayerSeeking &&
                    !adPlaybackTriggered &&
                    triggerAdPlaybackCounter == 1) {

                    if (hasPostRoll() && list[nextAdBreakIndexForMonitoring] == list.last) {
                        log.d("PlayheadUpdated: postroll position")
                        return@addListener
                    }
                    log.d("playheadUpdated ${event.position} & nextAdIndexForMonitoring is $nextAdBreakIndexForMonitoring & nextAdForMonitoring ad position is = ${list[nextAdBreakIndexForMonitoring]}")
                    log.d("nextAdForMonitoring ad position is = $list")

                    getAdFromAdConfigMap(nextAdBreakIndexForMonitoring)?.let { adUrl ->
                        playAd(adUrl)
                    }
                }
            }
        }

        messageBus?.addListener(this, PlayerEvent.seeking) {
            log.d("Player seeking for player position = ${player?.currentPosition} - currentPosition ${it.currentPosition} - targetPosition ${it.targetPosition}" )
            isPlayerSeeking = true
            if (isLiveMedia()) {
                log.v("Seeking Event. For Live Medias only Preroll ad will be played. Hence dropping other Ads if configured.")
                return@addListener
            }
        }

        messageBus?.addListener(this, PlayerEvent.loadedMetadata) {
            log.d("loadedMetadata Player duration is = ${player?.duration}" )
            if (isLiveMedia()) {
                log.v("Loaded MetaData Event. For Live Medias only Preroll ad will be played. Hence dropping other Ads if configured.")
                return@addListener
            }
            checkTypeOfMidrollAdPresent(advertisingContainer?.getMidrollAdBreakPositionType(), player?.duration)
        }

        messageBus?.addListener(this, PlayerEvent.seeked) {
            isPlayerSeeking = false

            if (isLiveMedia()) {
                log.v("Seeked Event. For Live Medias only Preroll ad will be played. Hence dropping other Ads if configured.")
                return@addListener
            }

            if (isAllAdsCompletedFired) {
                log.d("Player seeked to position = ${player?.currentPosition} but All ads has completed its playback hence returning.")
                return@addListener
            }

            val seekedPosition: Long = player?.currentPosition ?: return@addListener

            cuePointsList?.let { list ->

                if (list.isEmpty() || nextAdBreakIndexForMonitoring == DEFAULT_AD_INDEX) {
                    log.d("seeked: Ads are empty, dropping ad playback.")
                    return@addListener
                }

                if (nextAdBreakIndexForMonitoring < 0 || nextAdBreakIndexForMonitoring >= list.size) {
                    log.d("seeked: Invalid nextAdBreakIndexForMonitoring")
                    return@addListener
                }

                if (adPlaybackTriggered) {
                    log.d("seeked: Ad is playing or being loaded, dropping ad playback.")
                    return@addListener
                }

                // In case, if playAdsAfterTime is not configured and user seeked before the start Position then don't play the Ads.
                // On the other hand if playAdsAfterTime is configured then let the ad play in the normal fashion
                if (!isPlayAdsAfterTimeConfigured && playerStartPosition > 0 && seekedPosition < playerStartPosition) {
                    return@addListener
                }

                adPlaybackTriggered = false
                log.d("Player seeked to position = ${seekedPosition}")
                if (midrollAdBreakPositionType == AdBreakPositionType.EVERY &&
                    midrollFrequency > Long.MIN_VALUE &&
                    seekedPosition > midrollFrequency &&
                    ((seekedPosition % midrollFrequency) == 0L || (seekedPosition % midrollFrequency) < midrollFrequency)) {

                    getAdFromAdConfigMap(nextAdBreakIndexForMonitoring)?.let { adUrl ->
                        log.d("Midroll is EVERY(Frequency based) hence playing the last Ad.")
                        playAd(adUrl)
                    }

                    return@addListener
                }

                if (midRollAdsCount() > 0) {
                    val lastAdPosition = getImmediateLastAdPosition(seekedPosition)
                    if (lastAdPosition > 0 || (lastAdPosition == 0 && !hasPreRoll())) {
                        log.d("Ad found on the left side of ad list")
                        getAdFromAdConfigMap(lastAdPosition)?.let { adUrl ->
                            playAd(adUrl)
                        }
                    } else {
                        log.d("No Ad found on the left side of ad list, finding on right side")
                        // Trying to get the immediate Next ad from pod
                        val nextAdPosition = getImmediateNextAdPosition(seekedPosition)
                        if ((hasPreRoll() && nextAdPosition > 0) || (!hasPreRoll() && nextAdPosition >= 0)) {
                            log.d("Ad found on the right side of ad list, update the current and next ad Index")
                            nextAdBreakIndexForMonitoring = nextAdPosition
                        } else if (seekedPosition == 0L && nextAdPosition == -1) {
                            // Is seeked back till 0 then update the next index to monitor
                            if (hasPreRoll() && midRollAdsCount() > 0) {
                                nextAdBreakIndexForMonitoring = 1
                            } else if (midRollAdsCount() > 0) {
                                nextAdBreakIndexForMonitoring = 0
                            }
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
                // Resetting the index position for the replay case after the player event ended
                currentAdBreakIndexPosition = DEFAULT_AD_INDEX
                nextAdBreakIndexForMonitoring = DEFAULT_AD_INDEX
                adPlaybackTriggered = false
            }
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
            } else if (midrollAdBreakPositionType == AdBreakPositionType.EVERY) {
                if (hasPreRoll() && midRollAdsCount() > 0 && hasPostRoll()) {
                    nextAdBreakIndexForMonitoring = 1
                } else if (midRollAdsCount() > 0 && hasPostRoll()) {
                    nextAdBreakIndexForMonitoring = 0
                }
            }
            log.d("playAdsAfterTime = $playAdsAfterTime and nextAdPosition is $nextAdPosition")
            isPlayAdsAfterTimeConfigured = true
            return
        }
        isPlayAdsAfterTimeConfigured = false
        log.d("isPlayAdsAfterTimeConfigured : $isPlayAdsAfterTimeConfigured")
    }

    /**
     * Mapping of ALL_ADS_COMPLETED event from IMAPlugin
     */
    override fun allAdsCompleted() {
        log.d("allAdsCompleted callback and currentAdBreakIndexPosition is $currentAdBreakIndexPosition")
        adPlaybackTriggered = false
        if (isPlayAdNowTriggered) {
            handlePlayAdNowPlayback(AdEvent.allAdsCompleted, null)
            return
        }
        changeAdState(AdState.PLAYED, AdRollType.AD)
        val adUrl = getAdFromAdConfigMap(currentAdBreakIndexPosition)
        if (adUrl != null) {
            playAd(adUrl)
            return
        } else {
            changeAdState(AdState.PLAYED, AdRollType.ADBREAK)
            if (isPlayAdsAfterTimeConfigured &&
                playerStartPosition > 0L &&
                hasPreRoll() &&
                midRollAdsCount() > 0 &&
                currentAdBreakIndexPosition == 0 &&
                nextAdBreakIndexForMonitoring > DEFAULT_AD_INDEX) {

                // Special case, to mimic IMA behaviour so if playAdsAfterTime = -1 and startPosition > 0
                // Then after pre-roll playback, immediate last ad wrt startPosition will be played
                val adUrlForAdsAfterTimeAfterPreroll = getAdFromAdConfigMap(nextAdBreakIndexForMonitoring)
                if (adUrlForAdsAfterTimeAfterPreroll != null) {
                    playAd(adUrlForAdsAfterTimeAfterPreroll)
                    return
                }
            }
            playContent()
        }

        if (isPostrollLeftForPlaying) {
            playPostrollAdBreak()
        }
        isPostrollLeftForPlaying = false
    }

    override fun contentPauseRequested() {
        adPlaybackTriggered = true
    }

    override fun contentResumeRequested() {
        adPlaybackTriggered = false
    }

    /**
     * Mapping of AD_ERROR event from IMAPlugin
     */
    override fun adError(error: AdEvent.Error) {
        log.w("AdEvent.error callback $error")
        adPlaybackTriggered = false
        if (isPlayAdNowTriggered && error.error.errorType != PKAdErrorType.VIDEO_PLAY_ERROR) {
            handlePlayAdNowPlayback(AdEvent.adBreakFetchError, error)
            return
        }
        if (error.error.errorType != PKAdErrorType.VIDEO_PLAY_ERROR) {
            val ad = getAdFromAdConfigMap(currentAdBreakIndexPosition)
            if (ad.isNullOrEmpty()) {
                log.d("Ad is completely error $error")
                handleErrorEvent(true, getCurrentAdBreakConfig(), error)
                changeAdState(AdState.ERROR, AdRollType.ADBREAK)
                playContent()
            } else {
                log.d("Playing next waterfalling ad")
                handleErrorEvent(false, getCurrentAdBreakConfig(), error)
                changeAdState(AdState.ERROR, AdRollType.ADPOD)
                playAd(ad)
            }
        } else {
            handleErrorEvent(null, getCurrentAdBreakConfig(), error)
            log.d("PKAdErrorType.VIDEO_PLAY_ERROR currentAdIndexPosition = $currentAdBreakIndexPosition")
            cuePointsList?.let { cueList ->
                if (currentAdBreakIndexPosition != DEFAULT_AD_INDEX) {
                    val adPosition: Long = cueList[currentAdBreakIndexPosition]
                    if (currentAdBreakIndexPosition < cueList.size - 1 && adPosition != -1L) {
                        // Update next Ad index for monitoring
                        nextAdBreakIndexForMonitoring = currentAdBreakIndexPosition + 1
                        log.d("nextAdIndexForMonitoring is $nextAdBreakIndexForMonitoring")
                    }
                }
            }
            playContent()
        }
    }

    /**
     * Handle Error event for AdvertisingConfig
     * Send AD_WATERFALLING, AD_WATERFALLING_FAILED and ERROR event to the client apps.
     */
    private fun handleErrorEvent(isAllAdsFailed: Boolean?, adBreakConfig: AdBreakConfig?, error: AdEvent.Error?) {
        log.e("isAdWaterFallingOccurred $hasWaterFallingAds")
        isAllAdsFailed?.let {
            if (hasWaterFallingAds) {
                if (it) {
                    messageBus?.post(
                        AdEvent.AdWaterFallingFailed(adBreakConfig)
                    )
                    // When the all ads in the waterfalling list fail then with `AdWaterFallingFailed`, we need to fire
                    // AdEvent.error as well so that Analytics behaves as usual
                    error?.let { err ->
                        messageBus?.post(err)
                    }
                    log.e("Firing AdEvent.error after AdWaterFallingFailed event")
                } else {
                    messageBus?.post(
                        AdEvent.AdWaterFalling(adBreakConfig)
                    )
                    log.d("Firing AdWaterFalling event")
                }
            } else {
                error?.let { err ->
                    messageBus?.post(err)
                }
                log.e("Firing AdError because there was no AdWaterFalling")
            }
            hasWaterFallingAds = false
            return
        }
        // else Fire AdError
        error?.let { err ->
            log.d("Firing AdError $err")
            messageBus?.post(err)
        }
        hasWaterFallingAds = false
    }

    @Nullable
    private fun getCurrentAdBreakConfig(): AdBreakConfig? {
        log.d("getCurrentAdBreakConfig")
        if (currentAdBreakIndexPosition > DEFAULT_AD_INDEX) {
            cuePointsList?.let { cuePointsList ->
                if (cuePointsList.isNotEmpty()) {
                    val adPosition: Long = cuePointsList[currentAdBreakIndexPosition]
                    adsConfigMap?.let { adsMap ->
                        return if (adsMap.isEmpty()) {
                            null
                        } else {
                            adsMap[adPosition]
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Handle the PlayAdNow AdBreak/AdPod/Waterfalling playback
     */
    private fun handlePlayAdNowPlayback(adEventType: AdEvent.Type, error: AdEvent.Error?) {
        log.d("handlePlayAdNowPlayback ${adEventType.name}")
        if (adEventType == AdEvent.allAdsCompleted) {
            changeAdBreakState(playAdNowAdBreak, AdRollType.AD, AdState.PLAYED)
            val adUrl = fetchPlayableAdFromAdsList(playAdNowAdBreak, false)
            if (adUrl != null) {
                playAd(adUrl)
            } else {
                changeAdBreakState(playAdNowAdBreak, AdRollType.ADBREAK, AdState.PLAYED)
                isPlayAdNowTriggered = false
                playAdNowAdBreak = null
                playContent()
            }
        } else if (adEventType == AdEvent.adBreakFetchError) {
            val adUrl = fetchPlayableAdFromAdsList(playAdNowAdBreak, false)
            if (adUrl.isNullOrEmpty()) {
                log.d("PlayAdNow Ad is completely errored")
                handleErrorEvent(true, playAdNowAdBreak, error)
                changeAdBreakState(playAdNowAdBreak , AdRollType.ADBREAK, AdState.ERROR)
                isPlayAdNowTriggered = false
                playAdNowAdBreak = null
                playContent()
            } else {
                log.d("Playing next waterfalling ad")
                handleErrorEvent(false, playAdNowAdBreak, error)
                changeAdBreakState(playAdNowAdBreak, AdRollType.ADPOD, AdState.ERROR)
                playAd(adUrl)
            }
        } else {
            handleErrorEvent(null, playAdNowAdBreak, error)
            isPlayAdNowTriggered = false
            playAdNowAdBreak = null
            playContent()
        }
    }

    /**
     * Gets the next ad from AdsConfigMap using the cuePoints list
     * Set the next ad break position to be monitored as well
     */
    @Nullable
    private fun getAdFromAdConfigMap(adIndex: Int): String? {
        log.d("getAdFromAdConfigMap")

        if (isAllAdsCompletedFired) {
            log.d("All ads have completed its playback. Hence returning null from here.")
            return null
        }

        var adUrl: String? = null
        cuePointsList?.let { cuePointsList ->

            if (adIndex == cuePointsList.size || adIndex == DEFAULT_AD_INDEX) {
                currentAdBreakIndexPosition = DEFAULT_AD_INDEX
                nextAdBreakIndexForMonitoring = DEFAULT_AD_INDEX
                return null
            }

            if (cuePointsList.isNotEmpty()) {
                val adPosition: Long = cuePointsList[adIndex]
                adsConfigMap?.let { adsMap ->
                    getAdPodConfigMap(adPosition)?.let {
                        if (it.adBreakPositionType == AdBreakPositionType.EVERY) {
                            // For EVERY based midrolls always send 'isTriggeredFromPlayerPosition' true
                            adUrl = fetchPlayableAdOnFrequency(it, true)
                            adUrl?.let {
                                currentAdBreakIndexPosition = adIndex
                            }
                            return adUrl
                        }

                        if ((it.adBreakState == AdState.PLAYING || it.adBreakState == AdState.READY) && it.adBreakPositionType != AdBreakPositionType.EVERY) {
                            adUrl = fetchPlayableAdFromAdsList(it, false)
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
        hasWaterFallingAds = false

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
                    // We are not resetting the AdBreak, only the internal adPod or ad are being reset.
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
            hasWaterFallingAds = adPodConfig.hasWaterFalling

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
                        // Move AdState.PLAYED to outside on top level
                        continue
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
                                            // ONLY in case of EVERY if there is an Ad with PLAYED state return this ad.
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
        return adUrl
    }

    /**
     * After each successful or error ad playback,
     * Change the AdBreak, AdPod OR Ad state accordingly
     */
    private fun changeAdState(adState: AdState, adRollType: AdRollType) {
        log.d("changeAdPodState AdState is $adState and AdrollType is $adRollType")
        advertisingContainer?.let advertisingContainer@ { _ ->
            cuePointsList?.let { cuePointsList ->
                if (cuePointsList.isNotEmpty()) {
                    adsConfigMap?.let { adsMap ->
                        if (currentAdBreakIndexPosition != DEFAULT_AD_INDEX) {
                            val adPosition: Long = cuePointsList[currentAdBreakIndexPosition]
                            val adBreakConfig: AdBreakConfig? = adsMap[adPosition]
                            changeAdBreakState(adBreakConfig, adRollType, adState)
                            if (!isAllAdsCompleted && adPosition == -1L && adState == AdState.PLAYED && adRollType == AdRollType.ADBREAK) {
                                log.d("It's PostRoll and it is played completely, firing allAdsCompleted from here.")
                                fireAllAdsCompleteEvent()
                                return@advertisingContainer
                            } else {
                                checkAllAdsArePlayed()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Change the internal state of AdBreak, AdPod or Ad
     *
     * *NOTE*: For EVERY based midrolls AdPod State will be changed to READY
     * This is important because for the next EVERY based midroll, we will pick those AdPods
     * again and mark it Played/Errored accordingly [This will only be set when the all the AdPods
     * of the AdBreak are done with playback]
     *
     */
    private fun changeAdBreakState(adBreakConfig: AdBreakConfig?, adRollType: AdRollType, adState: AdState) {
        log.d("changeAdBreakState AdBreakConfig: $adBreakConfig")
        adBreakConfig?.let { adBreak ->
            log.d("AdState is changed for AdPod position ${adBreak.adPosition}")
            if (adRollType == AdRollType.ADBREAK) {
                adBreak.adBreakState = adState
            }

            adBreak.adPodList?.forEach {
                if (adRollType == AdRollType.ADBREAK && it.adPodState == AdState.PLAYING) {
                    it.adPodState = adState
                }

                var isAdPodCompletelyErrored = 0
                it.adList?.forEach { ad ->
                    if (adRollType == AdRollType.AD && ad.adState == AdState.PLAYING) {
                        if (ad.adState != AdState.ERROR) {
                            it.adPodState = adState
                        }
                        ad.adState = adState
                    }

                    if (adBreak.adBreakPositionType == AdBreakPositionType.EVERY &&
                        adRollType == AdRollType.AD &&
                        adState == AdState.PLAYED &&
                        ad.adState == AdState.PLAYED &&
                        it.adPodState == AdState.PLAYING) {

                        // NOTE: Continuation of method Javadoc:
                        // Because there will be ads which are already in PLAYED state
                        // but even though those were played due to EVERY.
                        // Check it and mark the AdPod state to PLAYED
                        it.adPodState = AdState.PLAYED
                    }

                    if (ad.adState != AdState.ERROR) {
                        isAdPodCompletelyErrored++
                    }
                }

                if (isAdPodCompletelyErrored == 0) {
                    it.adPodState = AdState.ERROR
                } else if (adBreak.adBreakPositionType == AdBreakPositionType.EVERY &&
                    adState == AdState.PLAYED &&
                    adRollType == AdRollType.ADBREAK) {

                    // NOTE: Continuation of method Javadoc:
                    // If All the ads of AdBreak completed then mark AdPod to READY
                    it.adPodState = AdState.READY
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
                val updatedCuePoints: List<Long>? = advertisingContainer?.getEveryBasedCuePointsList(playerDuration, midrollFrequency)
                updatedCuePoints?.let {
                    adController?.advertisingSetCuePoints(it)
                    log.d("Updated cuePointsList for EVERY based Midrolls $it")
                }
            }

            AdBreakPositionType.PERCENTAGE -> {
                playerDuration?.let {
                    if (it > 0) {
                        advertisingContainer?.updatePercentageBasedPosition(playerDuration)
                        adsConfigMap = advertisingContainer?.getAdsConfigMap()
                        cuePointsList = advertisingContainer?.getCuePointsList()
                        adController?.advertisingSetCuePoints(cuePointsList)
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

        adController?.setAdvertisingConfig(false, adType, null)

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
        isAllAdsCompletedFired = false

        midrollAdBreakPositionType = AdBreakPositionType.POSITION
        midrollFrequency = Long.MIN_VALUE

        isPlayAdNowTriggered = false
        playAdNowAdBreak = null

        isPlayAdsAfterTimeConfigured = false
        playAdsAfterTime = Long.MIN_VALUE

        hasWaterFallingAds = false
    }

    /**
     * Releasing the underlying resources
     */
    fun release() {
        advertisingContainer = null // Don't change the position of this
        resetAdvertisingConfig()
        destroyConfigResources()
        adController = null // Don't change the position of this
        log.d("Advertising Controller resources have been released completely")
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
        this.mediaConfig = null
    }

    /**
     * Check if all the ads are completely played
     */
    private fun checkAllAdsArePlayed(): Boolean {
        if (isAllAdsCompleted) {
            log.d("isAllAdsCompleted: $isAllAdsCompleted")
            fireAllAdsCompleteEvent()
            return true
        }

        log.d("checkAllAdsArePlayed")
        if (!hasPreRoll() && midRollAdsCount() <= 0 && !hasPostRoll()) {
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

    /**
     * Firing AllAdsCompleted event
     */
    private fun fireAllAdsCompleteEvent() {
        if (isAllAdsCompletedFired) {
            log.d("AllAdsCompleted event as already been fired.")
            return
        }
        log.d("fireAllAdsCompleteEvent")
        isAllAdsCompletedFired = true
        messageBus?.post(AdEvent(AdEvent.Type.ALL_ADS_COMPLETED))
        playContent()

        resetAdvertisingConfig()
    }
    /**
     * Trigger Postroll ad playback
     */
    private fun playPostrollAdBreak() {
        log.d("playPostrollAdBreak")
        midrollAdBreakPositionType = AdBreakPositionType.POSITION
        midrollFrequency = Long.MIN_VALUE
        getAdFromAdConfigMap(POSTROLL_AD_INDEX)?.let {
            playAd(it)
        }
    }

    /**
     * Check if the Ads config is empty
     */
    private fun isAdsListEmpty(): Boolean {
        log.d("isAdsListEmpty")
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
     * Create AdInfo object for IMAPlugin
     */
    private fun getAdInfo(): PKAdvertisingAdInfo? {
        log.d("createAdInfoForAdvertisingConfig")
        if (isPlayAdNowTriggered) {
            log.d("PlayAdNow ad is there, hence no need of AdInfo object")
            return null
        }

        if (currentAdBreakIndexPosition == Int.MIN_VALUE) {
            log.d("currentAdBreakIndexPosition is not valid")
            return null
        }

        var pkAdvertisingAdInfo: PKAdvertisingAdInfo? = null

        var adPodTimeOffset = 0L
        var podIndex = 0
        var podCount = 0

        adsConfigMap?.let { adsMap ->
            cuePointsList?.let { cuePoints ->
                adPodTimeOffset = cuePoints[currentAdBreakIndexPosition]
                podIndex = currentAdBreakIndexPosition + 1
                podCount = cuePoints.size
            }

            pkAdvertisingAdInfo = PKAdvertisingAdInfo(adPodTimeOffset, podIndex, podCount)
        }

        return pkAdvertisingAdInfo
    }

    /**
     * Ad Playback
     * Call the play Ad API on IMAPlugin
     */
    private fun playAd(adTag: String) {
        log.d("playAd AdUrl is $adTag")
        adPlaybackTriggered = !TextUtils.isEmpty(adTag)

        // If Ad is empty, it means the content will be loaded using IMAPlugin
        if (!TextUtils.isEmpty(adTag)) {
            player?.let {
                adController?.advertisingSetAdInfo(getAdInfo())
                if (it.isPlaying) {
                    it.pause()
                }
            }
        }
        adController?.advertisingPlayAdNow(adTag)
    }

    /**
     * Content Playback
     */
    private fun playContent() {
        log.d("playContent")
        adPlaybackTriggered = false
        player?.let {
            adController?.advertisingPreparePlayer()
            if (!it.isPlaying) {
                it.play()
            }
        }

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
     * Checks if the media is Live
     * @return isLive or not
     */
    private fun isLiveMedia(): Boolean {
        player?.let {
            if (it.isLive) {
                return true
            }
            getMediaEntry()?.let { pkMediaEntry ->
                return pkMediaEntry.mediaType != PKMediaEntry.MediaEntryType.Vod
            }
        }
        return false
    }

    /**
     * Get the PKMediaEntry from PKMediaConfig
     */
    private fun getMediaEntry(): PKMediaEntry? {
        return mediaConfig?.let { pkMediaConfig ->
            pkMediaConfig.mediaEntry?.let { pkMediaEntry ->
                pkMediaEntry
            }
        }
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
        log.v("MidRollAdsCount is $midrollAdsCount")
        return midrollAdsCount
    }

    /**
     * Checks if it has only the pre roll ad
     */
    private fun hasOnlyPreRoll(): Boolean {
        if (hasPreRoll() && !hasPostRoll() && midRollAdsCount() <= 0) {
            return true
        }
        return false
    }

    /**
     * Get the just previous ad position
     * Used only if the user seeked the Ad cue point
     * Mimicking the SNAPBACK feature
     */
    private fun getImmediateLastAdPosition(position: Long?): Int {
        log.d("getImmediateLastAdPosition")

        if (position == null || cuePointsList == null || cuePointsList.isNullOrEmpty()) {
            log.d("Error in getImmediateLastAdPosition returning DEFAULT_AD_POSITION")
            return DEFAULT_AD_INDEX
        }

        var adPosition = -1

        if (cuePointsList?.size == 1) {
            adPosition = 0
            return adPosition
        }

        cuePointsList?.let {
            if (position > 0 && it.isNotEmpty() && it.size > 1) {
                var lowerIndex: Int = if (it.first == 0L) 1 else 0
                var upperIndex: Int = if (it.last == -1L) it.size -2 else (it.size - 1)

                while (lowerIndex <= upperIndex) {
                    val midIndex = lowerIndex + (upperIndex - lowerIndex) / 2

                    if (it[midIndex] == position) {
                        adPosition = midIndex
                        break
                    } else if (it[midIndex] < position) {
                        adPosition = midIndex
                        lowerIndex = midIndex + 1
                    } else if (it[midIndex] > position) {
                        upperIndex = midIndex - 1
                    }
                }
            } else if (it.size == 1) {
                adPosition = 0
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
    private fun getImmediateNextAdPosition(position: Long?): Int {
        log.d("getImmediateNextAdPosition")

        if (position == null || cuePointsList == null || cuePointsList.isNullOrEmpty()) {
            log.d("Error in getImmediateNextAdPosition returning DEFAULT_AD_POSITION")
            return DEFAULT_AD_INDEX
        }

        var adPosition = -1

        if (cuePointsList?.size == 1) {
            adPosition = 0
            return adPosition
        }

        cuePointsList?.let {
            if (position > 0 && it.isNotEmpty() && it.size > 1) {
                var lowerIndex: Int = if (it.first == 0L) 1 else 0
                var upperIndex: Int = if (it.last == -1L) it.size -2 else (it.size - 1)

                while (lowerIndex <= upperIndex) {
                    val midIndex = lowerIndex + (upperIndex - lowerIndex) / 2

                    if (it[midIndex] == position) {
                        adPosition = midIndex
                        break
                    } else if (it[midIndex] < position) {
                        lowerIndex = midIndex + 1
                    } else if (it[midIndex] > position) {
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

