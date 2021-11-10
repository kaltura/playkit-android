package com.kaltura.playkit.ads

import androidx.annotation.Nullable
import com.kaltura.playkit.PKLog
import java.util.*
import kotlin.collections.ArrayList

/**
 * Class to map the Advertising object to our internal helper DataStructure
 */
internal class AdvertisingTree(advertisingConfig: AdvertisingConfig?) {

    private val log = PKLog.get(AdvertisingTree::class.java.simpleName)
    private var adsConfigMap: MutableMap<Long, AdBreakConfig?>? = null // TODO: Check the condition having 0sec -> 1sec (how video view is getting removed)
    private var cuePointsList: LinkedList<Long>? = null

    init {
        advertisingConfig?.let {
            parseAdTypes(it)
        }
    }

    /**
     * Parse the Ads from the external Ads' data structure
     */
    private fun parseAdTypes(advertisingConfig: AdvertisingConfig) {
        advertisingConfig.ads?.let { adBreaks ->
            val adBreaksList = ArrayList<AdBreakConfig>()
            cuePointsList = LinkedList()
            for (adBreak: AdBreak? in adBreaks) {
                adBreak?.let { singleAdBreak ->
                    val adPodConfigList = parseAdPodConfig(singleAdBreak)
                    // Create ad break list and mark them ready
                    val adBreakConfig = AdBreakConfig(singleAdBreak.position, AdState.READY, adPodConfigList)
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
    private fun parseAdPodConfig(singleAdBreak: AdBreak): List<AdPodConfig> {
        val adPodConfigList = mutableListOf<AdPodConfig>()
        for (adPod: List<String>? in singleAdBreak.ads) {
            val adsList = parseEachAdUrl(adPod)
            val adPodConfig = AdPodConfig(AdState.READY, adsList)
            adPodConfigList.add(adPodConfig)
        }
        return adPodConfigList
    }

    /**
     * AdPod may contain the list of Ads (including waterfalling ads)
     * Mark all the ads Ready.
     */
    private fun parseEachAdUrl(ads: List<String>?): List<Ad> {
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
        if (adBreakConfigList.isNotEmpty()) {
            adsConfigMap = mutableMapOf()
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
        cuePointsList?.let {
            if (it.first == -1L) {
                it.remove(-1)
            }
            it.addLast(-1)
        }
    }

    /**
     * Getter for CuePoints list
     */
    @Nullable
    fun getCuePointsList(): LinkedList<Long>? {
        return cuePointsList
    }

    /**
     * Get MidRoll ads if there is any
     */
    @Nullable
    fun getAdsConfigMap(): MutableMap<Long, AdBreakConfig?>? {
        return adsConfigMap
    }
}

// Ad Break Config
internal data class AdBreakConfig(val adPosition: Long, var adBreakState: AdState, val adPodList: List<AdPodConfig>?)

// Ad list contains waterfalling ads as well.
internal data class AdPodConfig(var adPodState: AdState, val adList: List<Ad>?)

// Single Ad
internal data class Ad(var adState: AdState, val ad: String)

// Ad's State
enum class AdState {
    READY,
    PLAYING,
    PLAYED,
    ERROR
}






