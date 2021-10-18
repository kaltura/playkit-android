package com.kaltura.playkit.ads

// List of waterfalling ads
data class Advertising(val ads: List<AdBreak?>?)// TODO: change it to ADBreak as well

// Where the ad lies (adtype - pre/mid/post)
data class AdBreak(val position: Long, val ads: List<String>)

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
