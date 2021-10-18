package com.kaltura.playkit.ads

import androidx.annotation.Nullable
import com.kaltura.playkit.plugins.ads.AdPositionType
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

internal class AdvertisingTree(advertising: Advertising?) {

    private var prerollAdConfig: AdUrlConfigs? = null
    private var midRollAdConfig: MutableMap<Long, AdUrlConfigs?>? = null
    private var postrollAdConfig: AdUrlConfigs? = null

    private var midRollAdvertisingQueue: Queue<Long>? = null

    init {
        advertising?.let {
            parseAdTypes(it)
        }
    }

    private fun parseAdTypes(advertising: Advertising) {
        advertising.ads?.let { adBreaks ->
            val midRollAdBreaks = ArrayList<MidRollAdConfig>()
            for (adBreak: AdBreak? in adBreaks) {
                adBreak?.let {
                    if (it.position == -1L) {
                        postrollAdConfig = setAdUrlConfig(it.ads)
                    } else if (it.position > 0) {
                        midRollAdvertisingQueue = LinkedList()
                        midRollAdBreaks.add(MidRollAdConfig(it.position, setAdUrlConfig(it.ads)))
                    } else if (it.position == 0L) {
                        prerollAdConfig = setAdUrlConfig(it.ads)
                    } else {
                        //TODO: Handle it
                    }
                }
            }

            if (midRollAdBreaks.isNotEmpty()) {
                midRollAdBreaks.sortWith(compareBy { it.value })
                populateMidRollAds(midRollAdBreaks)
            }
        }
    }

    private fun populateMidRollAds(midRollAdBreaks: ArrayList<MidRollAdConfig>) {
        if (midRollAdBreaks.isNotEmpty()) {
            midRollAdConfig = HashMap()
            for (adConfig: MidRollAdConfig in midRollAdBreaks) {
                midRollAdConfig?.put(adConfig.value, adConfig.adUrlConfigs)
                midRollAdvertisingQueue?.add(adConfig.value)
            }
        }
    }

    private fun setAdUrlConfig(ads: List<String>): AdUrlConfigs {
        val adUrls = mutableListOf<Ad>()
        for (url: String in ads) {
            adUrls.add(Ad(AdState.LOADED, url))
        }
        return AdUrlConfigs(AdState.LOADED, adUrls)
    }

   /* private fun setMidrollAds(ads: List<AdBreak?>): List<MidrollAdConfig> {
        val midRollAdBreak = mutableListOf<MidrollAdConfig>()
        for (adBreak: AdBreak? in ads) {
            adBreak?.let {
                midRollAdBreak.add(MidrollAdConfig(false, it.position, setAdUrlConfig(it.ads)))
            }
        }
        return midRollAdBreak
    }*/

    @Nullable
    fun getMidRollAdvertisingQueue(): Queue<Long>? {
        midRollAdvertisingQueue?.let {
            if (it.isEmpty()) {
                getMidRollAds()?.keys?.forEach { key ->
                    it.add(key)
                }
            }
        }
        return midRollAdvertisingQueue
    }

    @Nullable
    fun getPrerollAds(): AdUrlConfigs? {
        return prerollAdConfig
    }

    @Nullable
    fun getMidRollAds(): MutableMap<Long, AdUrlConfigs?>? {
        return midRollAdConfig
    }

    @Nullable
    fun getPostrollAds(): AdUrlConfigs? {
        return postrollAdConfig
    }
}

internal data class AdUrlConfigs(var adPodState: AdState, val adList: List<Ad>?)
internal data class Ad(var adState: AdState, val ad: String)

internal data class MidRollAdConfig(val value: Long, val adUrlConfigs: AdUrlConfigs?)

enum class AdState {
    LOADED,
    PLAYING,
    PLAYED,
    ERROR
}





