package com.kaltura.playkit.ads

import androidx.annotation.NonNull

// Collection of classes and enums required for Advertising

/**
 * Advertising configuration
 * Pass the list of AdBreaks
 *
 * @param advertising: List of AdBreaks
 * @param adTimeUnit: AdBreak position in Seconds or Milliseconds
 * @param adType: If it is AdUrl or VAST response
 * @param playAdsAfterTime: Play ads only from a specific time
 */
data class AdvertisingConfig(@NonNull val advertising: List<AdBreak?>?,
                             @NonNull val adTimeUnit: AdTimeUnit = AdTimeUnit.SECONDS,
                             @NonNull val adType: AdType = AdType.AD_URL,
                             @NonNull val playAdsAfterTime: Long = Long.MIN_VALUE)

/**
 * AdBreak: Pre, Mid, Post
 * Each AdBreak may contain List of AdPod
 * Each AdPod may contain a list of Ads (List of Ads is being used to do waterfalling)
 *
 * For PlayAdNow API, if app is passing AdBreak then position is irrelevant.
 */
data class AdBreak(@NonNull var adBreakPositionType: AdBreakPositionType = AdBreakPositionType.POSITION,
                   @NonNull var position: Long,
                   @NonNull val ads: List<List<String>>)

// Ad Break Config
data class AdBreakConfig(val adBreakPositionType: AdBreakPositionType,
                         var adPosition: Long,
                         var adBreakState: AdState,
                         val adPodList: List<AdPodConfig>?)

// Ad list contains waterfalling ads as well.
data class AdPodConfig(var adPodState: AdState,
                       val adList: List<Ad>?,
                       val hasWaterFalling: Boolean = false)

// Single Ad
data class Ad(var adState: AdState,
              val ad: String)

/**
 * AdInfo for the Advertising Controller
 */
data class PKAdvertisingAdInfo(var adPodTimeOffset: Long,
                               var podIndex: Int,
                               var podCount: Int)

/**
 * For Preroll and Postroll, always configure POSITION or PERCENTAGE (0% = Preroll, 100% = Postroll)
 * For Midroll, POSITION, PERCENTAGE or EVERY can be configured.
 *
 * PERCENTAGE and EVERY can not be mixed in one configuration (Only one can be configured at a time)
 * For EVERY: Only one Midroll ad should be configured (Because the adbreak will be played every X seconds)
 */
enum class AdBreakPositionType {
    POSITION, // Play AdBreak at this specific second
    PERCENTAGE, // Play AdBreak at nth percentage (Position percentage of the media length)
    EVERY // Play AdBreak at every n seconds (60 means on every 1 min ad will be played)
}

/**
 * AdBreak time value can be passed as SECONDS (10 means 10 seconds) OR MILISECONDS (10000 means 10 seconds)
 */
enum class AdTimeUnit {
    SECONDS,
    MILISECONDS
}

/**
 * Passing Ad type
 * It could be Ad's VAST Url or Ad's VAST XML Response
 */
enum class AdType {
    AD_URL,
    AD_RESPONSE
}

/**
 * Ad's State
 */
enum class AdState {
    READY,
    PLAYING,
    PLAYED,
    ERROR
}

/**
 * Adroll type
 * 
 * AdLayout may contain different AdBreaks
 * Each AdBreak will contain at least 1 or more than 1 AdPod
 * Ead AdPod will contain ad least 1 Ad
 */

internal enum class AdRollType {
    ADBREAK,
    ADPOD,
    AD
}
