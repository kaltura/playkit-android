package com.kaltura.playkit.ads

import androidx.annotation.Nullable
import com.kaltura.playkit.PKLog
import java.util.*
import kotlin.collections.ArrayList

/**
 * Class to map the Advertising object to our internal helper DataStructure
 */
internal class AdvertisingTree(advertising: Advertising?) {

    private val log = PKLog.get(AdvertisingTree::class.java.simpleName)
    private var adsConfigMap: MutableMap<Long, AdPodConfig?>? = null // TODO: Check the condition having 0sec -> 1sec (how video view is getting removed)
    private var cuePointsQueue: LinkedList<Long>? = null

    init {
        advertising?.let {
            parseAdTypes(it)
        }
    }

    /**
     * Parse the Ads from the external Ads' data structure
     */
    private fun parseAdTypes(advertising: Advertising) {
        advertising.ads?.let { adPods ->
            val adPodsList = ArrayList<AdPodConfig>()
            cuePointsQueue = LinkedList()
            for (adPod: AdPod? in adPods) {
                adPod?.let {
                    adPodsList.add(AdPodConfig(it.position, AdState.READY, setAdUrlConfig(it.ads)))
                }
            }
            sortAdsByPosition(adPodsList)
        }
    }

    /**
     * Sorting ads by position: App can pass the AdPod in any sequence
     * Here we are arranging the ads in Pre(0)/Mid(n)/Post(-1) adroll order
     * Here Mid(n) n denotes the time/percentage
     */
    private fun sortAdsByPosition(adPodsList: ArrayList<AdPodConfig>) {
        if (adPodsList.isNotEmpty()) {
            adPodsList.sortWith(compareBy { it.adPosition })
            prepareAdsMapAndQueue(adPodsList)
            movePostRollAdToLast()
        }
    }

    /**
     * After the Ads sorting, create a map with position and the relevant AdPodConfig
     * Prepare a CuePoints Queue. Queue is being monitored on the controller level
     * to understand the current and upcoming cuepoint
     */
    private fun prepareAdsMapAndQueue(adPodConfigList: ArrayList<AdPodConfig>) {
        if (adPodConfigList.isNotEmpty()) {
            adsConfigMap = mutableMapOf()
            for (adPodConfig: AdPodConfig in adPodConfigList) {
                adsConfigMap?.put(adPodConfig.adPosition, adPodConfig)
                cuePointsQueue?.add(adPodConfig.adPosition)
            }
        }
    }

    /**
     * After the sorting -1 will be on the top,
     * so remove it and put it at the last (Postroll)
     */
    private fun movePostRollAdToLast() {
        cuePointsQueue?.let {
            if (it.first == -1L) {
                it.remove(-1)
            }
            it.addLast(-1)
        }
    }

    /**
     * AdPod contains the list of Ads (including waterfalling ads)
     * Mark all the ads Ready.
     */
    private fun setAdUrlConfig(ads: List<String>): List<Ad> {
        val adUrls = mutableListOf<Ad>()
        for (url: String in ads) {
            adUrls.add(Ad(AdState.READY, url))
        }
        return adUrls
    }

    /**
     * Getter for CuePoints queue
     */
    @Nullable
    fun getCuePointsQueue(): LinkedList<Long>? {
        return cuePointsQueue
    }

    /**
     * Get MidRoll ads if there is any
     */
    @Nullable
    fun getAdsConfigMap(): MutableMap<Long, AdPodConfig?>? {
        return adsConfigMap
    }
}

// Ad Pod Config
internal data class AdPodConfig(val adPosition: Long, var adPodState: AdState, val adList: List<Ad>?)
// Each ad with list of waterfalling ads
internal data class Ad(var adState: AdState, val ad: String)

// Ad's State
enum class AdState {
    READY, // name it READY
    PLAYING,
    PLAYED,
    ERROR
}






