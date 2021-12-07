package com.kaltura.playkit.ads

import androidx.annotation.Nullable
import com.google.gson.Gson
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.utils.Consts
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Class to map the Advertising object to our internal helper DataStructure
 */
internal class AdvertisingContainer(advertisingConfig: AdvertisingConfig?) {

    private val log = PKLog.get(AdvertisingContainer::class.java.simpleName)
    private var adsConfigMap: MutableMap<Long, AdBreakConfig?>? = null // TODO: Check the condition having 0sec -> 1sec (how video view is getting removed)
    private var cuePointsList: LinkedList<Long>? = null
    private var midrollAdPositionType: AdBreakPositionType = AdBreakPositionType.POSITION
    private var midrollFrequency = Long.MIN_VALUE
    private var playAdsAfterTime = Long.MIN_VALUE
    private var adType: AdType = AdType.AD_URL

    init {
        advertisingConfig?.let {
            parseAdTypes(it)
        }
    }

    /**
     * Parse the Ads from the external Ads' data structure
     */
    private fun parseAdTypes(advertisingConfig: AdvertisingConfig?) {
        log.d("parseAdTypes")

        advertisingConfig?.advertising?.let { adBreaks ->
            val adBreaksList = ArrayList<AdBreakConfig>()
            cuePointsList = LinkedList()

            playAdsAfterTime = if (advertisingConfig.adTimeUnit == AdTimeUnit.SECONDS && (advertisingConfig.playAdsAfterTime != -1L || advertisingConfig.playAdsAfterTime > 0)) {
                advertisingConfig.playAdsAfterTime * Consts.MILLISECONDS_MULTIPLIER
            } else {
                advertisingConfig.playAdsAfterTime
            }

            if (advertisingConfig.adType != null) {
                adType = advertisingConfig.adType
            }

            for (adBreak: AdBreak? in adBreaks) {

                adBreak?.let adBreakLoop@{ singleAdBreak ->

                    // Only one ad can be configured for Every AdBreakPositionType

                    // TODO: Check pre/post is not passed apart from position

                    if ((singleAdBreak.position == 0L || singleAdBreak.position == -1L) &&
                        (singleAdBreak.adBreakPositionType == AdBreakPositionType.EVERY || singleAdBreak.adBreakPositionType == AdBreakPositionType.PERCENTAGE)) {
                        log.w("Preroll or Postroll ad should not be configured with AdBreakPositionType.EVERY or AdBreakPositionType.PERCENTAGE\n" +
                                "Dropping such AdBreak")
                        return@adBreakLoop
                    }

                    if (midrollAdPositionType == AdBreakPositionType.POSITION && adBreak.adBreakPositionType == AdBreakPositionType.EVERY) {
                        midrollAdPositionType = AdBreakPositionType.EVERY
                        midrollFrequency = if (advertisingConfig.adTimeUnit == AdTimeUnit.SECONDS) singleAdBreak.position * Consts.MILLISECONDS_MULTIPLIER else singleAdBreak.position
                    } else if (midrollAdPositionType == AdBreakPositionType.EVERY && adBreak.adBreakPositionType == AdBreakPositionType.EVERY) {
                        log.w("There should not be multiple Midrolls for AdBreakPositionType EVERY.\n" +
                                "Keep One MidRoll ad which will play at the given second.")
                        midrollAdPositionType = AdBreakPositionType.POSITION
                        midrollFrequency = 0L
                        return@adBreakLoop
                    }

                    if (adBreak.adBreakPositionType == AdBreakPositionType.PERCENTAGE) {
                        if (midrollAdPositionType == AdBreakPositionType.EVERY) {
                            log.w("There should not be a combination of PERCENTAGE and EVERY.")
                            return@adBreakLoop
                        }
                        if (singleAdBreak.position <= 0 || singleAdBreak.position >= 100) {
                            log.w("AdBreak having PERCENTAGE type \n " +
                                    "should neither give percentage values less than or equal to 0 nor \n " +
                                    "greater than or equal to 100.")
                            return@adBreakLoop
                        }

                        midrollAdPositionType = AdBreakPositionType.PERCENTAGE
                    }

                    if (advertisingConfig.adTimeUnit == AdTimeUnit.SECONDS &&
                        (singleAdBreak.adBreakPositionType == AdBreakPositionType.POSITION || singleAdBreak.adBreakPositionType == AdBreakPositionType.EVERY)) {

                        singleAdBreak.position = if (singleAdBreak.position > 0) (singleAdBreak.position * Consts.MILLISECONDS_MULTIPLIER) else singleAdBreak.position // Convert to miliseconds
                    }

                    val adPodConfigList = parseAdPodConfig(singleAdBreak)
                    // Create ad break list and mark them ready
                    val adBreakConfig = AdBreakConfig(singleAdBreak.adBreakPositionType, singleAdBreak.position, AdState.READY, adPodConfigList)
                    // TODO: If some one gives midroll equal to duration with postroll

                    // TODO: When I am dropping the every feature. may be 5 second before the media end
                    adBreaksList.add(adBreakConfig)
                }
            }
            sortAdsByPosition(adBreaksList)
        }
    }

    /**
     * Parse Each AdBreak. AdBreak may contain list of ad pods
     * Mark all the ad pods Ready.
     */
    internal fun parseAdPodConfig(singleAdBreak: AdBreak): List<AdPodConfig> {
        log.d("parseAdPodConfig")
        val adPodConfigList = mutableListOf<AdPodConfig>()
        for (adPod: List<String>? in singleAdBreak.ads) {
            val adsList = parseEachAdUrl(adPod)
            val hasWaterFalling = adsList.size > 1
            val adPodConfig = AdPodConfig(AdState.READY, adsList, hasWaterFalling)
            adPodConfigList.add(adPodConfig)
        }
        return adPodConfigList
    }

    internal fun parseAdBreakGSON(singleAdBreak: String): AdBreak? {
        log.d("parseAdBreakGSON")
        try {
            val adBreak = Gson().fromJson<AdBreak>(singleAdBreak, AdBreak::class.java)
            if (adBreak != null) {
                return adBreak
            } else {
                log.e("Malformed AdBreak Json")
            }
        } catch (e: Exception) {
            log.e("Malformed AdBreak Json Exception: ${e.message}")
        }
        return null
    }

    /**
     * AdPod may contain the list of Ads (including waterfalling ads)
     * Mark all the ads Ready.
     */
    private fun parseEachAdUrl(ads: List<String>?): List<Ad> {
        log.d("parseEachAdUrl")
        val adUrls = mutableListOf<Ad>()
        if (ads != null) {
            for (url: String in ads) {
                adUrls.add(Ad(AdState.READY, url))
            }
        }
        return adUrls
    }

    /**
     * Sorting ads by position: App can pass the AdBreaks in any sequence
     * Here we are arranging the ads in Pre(0)/Mid(n)/Post(-1) adroll order
     * Here Mid(n) n denotes the time/percentage
     */
    private fun sortAdsByPosition(adBreaksList: ArrayList<AdBreakConfig>) {
        log.d("sortAdsByPosition")
        if (adBreaksList.isNotEmpty()) {
            adBreaksList.sortWith(compareBy { it.adPosition })
            prepareAdsMapAndList(adBreaksList)
            movePostRollAdToLastInList()
        }
    }

    /**
     * After the Ads sorting, create a map with position and the relevant AdBreakConfig
     * Prepare a CuePoints List. List is being monitored on the controller level
     * to understand the current and upcoming cuepoint
     */
    private fun prepareAdsMapAndList(adBreakConfigList: ArrayList<AdBreakConfig>) {
        log.d("prepareAdsMapAndList")
        if (adBreakConfigList.isNotEmpty()) {
            adsConfigMap = hashMapOf()
            for (adBreakConfig: AdBreakConfig in adBreakConfigList) {
                adsConfigMap?.put(adBreakConfig.adPosition, adBreakConfig)
                cuePointsList?.add(adBreakConfig.adPosition)
            }
        }
    }

    /**
     * After the sorting -1 will be on the top,
     * so remove it and put it at the last (Postroll)
     */
    private fun movePostRollAdToLastInList() {
        log.d("movePostRollAdToLastInList")
        cuePointsList?.let {
            if (it.first == -1L) {
                it.remove(-1)
                it.addLast(-1)
            }
        }
    }

    /**
     * Used only if AdBreakPositionType is PERCENTAGE
     * Remove and Add the Map's Adbreak Position as per the player duration (Replace not allowed for key in Map)
     * Replace the List's Adbreak Position as per the player duration
     */
    fun updatePercentageBasedPosition(playerDuration: Long?) {
        log.d("updatePercentageBasedPosition PlayerDuration is $playerDuration")
        playerDuration?.let {
            if (it <= 0) {
                return
            }
        }

        adsConfigMap?.let { adsMap ->
            val iterator = adsMap.keys.iterator()
            var tempMapForUpdatedConfigs: HashMap<Long, AdBreakConfig?>? = HashMap(adsMap.size)
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val config = adsMap[entry]

                if (config?.adBreakPositionType == AdBreakPositionType.PERCENTAGE) {
                    playerDuration?.let {
                        // Copy of adconfig object
                        val newAdBreakConfig = config.copy()
                        // Remove the actual adconfig from map
                        iterator.remove()

                        // Update the copied object with updated position for percentage
                        val oldAdPosition = newAdBreakConfig.adPosition
                        val updatedPosition = playerDuration.times(oldAdPosition).div(100) // Ex: 23456
                        val updatedRoundedOfPositionMs = (updatedPosition.div(Consts.MILLISECONDS_MULTIPLIER)) * Consts.MILLISECONDS_MULTIPLIER // It will be changed to 23000
                        newAdBreakConfig.adPosition = updatedRoundedOfPositionMs

                        // Put back again the object in the temp map (Because Iterator doesn't have put method
                        // hence to avoidConcurrentModificationException, need to use temp map and putall the values after the iteration
                        // Don't use ConcurrentHashmap as it can be overkilling
                        tempMapForUpdatedConfigs?.put(newAdBreakConfig.adPosition, newAdBreakConfig)
                        cuePointsList?.forEachIndexed { index, adPosition ->
                            if (adPosition == oldAdPosition) {
                                cuePointsList?.set(index, updatedRoundedOfPositionMs)
                            }
                        }
                    }
                }
            }
            tempMapForUpdatedConfigs?.let {
                if (it.isNotEmpty()) {
                    adsMap.putAll(it)
                }
                it.clear()
            }
            tempMapForUpdatedConfigs = null
        }
        sortCuePointsList()
    }

    /**
     * Sort the cue points list again
     * Move the postroll to the last
     */
    private fun sortCuePointsList() {
        log.d("sortCuePointsList")
        cuePointsList?.sort()
        movePostRollAdToLastInList()
    }

    fun getEveryBasedCuePointsList(playerDuration: Long?, frequency: Long): List<Long>? {
        log.d("getEveryBasedCuePointsList PlayerDuration is $playerDuration")
        playerDuration?.let {
            if (it <= 0) {
                return null
            }
        }

        if (frequency > Long.MIN_VALUE) {
            val updatedCuePointsList = mutableListOf<Long>()

            playerDuration?.let { duration ->
                val updatedRoundedOfDurationMs =
                    (duration.div(Consts.MILLISECONDS_MULTIPLIER)) * Consts.MILLISECONDS_MULTIPLIER
                val factor = updatedRoundedOfDurationMs / frequency
                for (factorValue in 1..factor) {
                    updatedCuePointsList.add(frequency * factorValue)
                }
            }

            log.d("getEveryBasedCuePointsList ${updatedCuePointsList}")

            if (updatedCuePointsList.isNotEmpty()) {
                if (cuePointsList?.first == 0L) {
                    updatedCuePointsList.add(0, 0)
                }

                if (cuePointsList?.last == -1L) {
                    updatedCuePointsList.add(-1)
                }
            }

            log.d(" final updatedCuePointsList = ${updatedCuePointsList}")
            return updatedCuePointsList
        }

        return null
    }

    /**
     * Get Midroll ad break position type (POSITION, PERCENTAGE, EVERY)
     */
    fun getMidrollAdBreakPositionType(): AdBreakPositionType {
        log.d("getMidrollAdBreakPositionType")
        return midrollAdPositionType
    }

    /**
     * If Midroll ad break position type is EVERY
     * Then at what frequency ad will be played
     */
    fun getMidrollFrequency(): Long {
        log.d("getMidrollFrequency")
        return midrollFrequency
    }

    /**
     * Getter for CuePoints list
     */
    @Nullable
    fun getCuePointsList(): LinkedList<Long>? {
        log.d("getCuePointsList")
        return cuePointsList
    }

    /**
     * Get MidRoll ads if there is any
     */
    @Nullable
    fun getAdsConfigMap(): MutableMap<Long, AdBreakConfig?>? {
        log.d("getAdsConfigMap")
        return adsConfigMap
    }

    /**
     * Get PlayAdsAfterTime value from AdvertisingConfig
     */
    fun getPlayAdsAfterTime(): Long {
        return playAdsAfterTime
    }

    fun getAdType(): AdType {
        return adType
    }
}

