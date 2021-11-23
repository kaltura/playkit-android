package com.kaltura.playkit.ads

// List of waterfalling ads
data class AdvertisingConfig(val ads: List<AdBreak?>?)// TODO: change it to ADBreak as well

// Where the ad lies (adtype - pre/mid/post)

/**
 *                                                 -> Ad 1 -> List of Ads (Ad Waterfalling may occur here)
 *  AdBreak -> Position, AdPod -> List Of Ads --->|
 *                                                 -> Ad 2 -> List Of Ads  (Ad Waterfalling may occur here)1, 2, 3
 *
 *                                                 2 adpods 1, 2
 *                                                  2ads
 *
 */
data class AdBreak(var adBreakPositionType: AdBreakPositionType, var position: Long, val ads: List<List<String>>) {
    init {
        if (adBreakPositionType == null) {
            adBreakPositionType = AdBreakPositionType.POSITION
        }

        if (adBreakPositionType == AdBreakPositionType.POSITION || adBreakPositionType == AdBreakPositionType.EVERY) {
            position = if (position > 0) (position * 1000) else position // Convert to miliseconds
        }
    }
}

enum class AdBreakPositionType {
    POSITION, // Play AdBreak at this specific second
    PERCENTAGE, // Play AdBreak at nth percentage (Position percentage of the media length)
    EVERY // Play AdBreak at every n seconds (60 means on every 1 min ad will be played)
}
//{
//    constructor(ads: List<String>): this(Long.MIN_VALUE, ads)
//    constructor(ad: String): this(Long.MIN_VALUE, listOf(ad))
//}
//TODO: try to add single Ad



// data class AdBreak(val positionPercentage: Float, val ads: List<String>)

// For each ad pod, how many waterfalling ads are supplied
//data class AdUrl(val url: List<String>)

// For each adType, how many ad pods are there
// data class Ads(val adUrls: List<AdUrl>, val isBumper: Boolean)

// For each ad pod, how many waterfalling ads are supplied
//data class AdUrl(val url: List<String>)

/**
 * No of PODS -3 (Pre/Mid/Post)
 * Each POD has no of ads internally (1 of 3, 2 of 3, 3 of 3)
 */
