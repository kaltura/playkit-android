package com.kaltura.playkit.ads

// List of waterfalling ads
data class AdvertisingConfig(val ads: List<AdBreak?>?)// TODO: change it to ADBreak as well

// Where the ad lies (adtype - pre/mid/post)

/**
 *                                                 -> Ad 1 -> List of Ads (Ad Waterfalling may occur here)
 *  AdBreak -> Position, AdPod -> List Of Ads --->|
 *                                                 -> Ad 2 -> List Of Ads  (Ad Waterfalling may occur here)
 *
 */
data class AdBreak(val position: Long, val ads: List<List<String>>)
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
