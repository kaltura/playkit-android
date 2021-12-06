package com.kaltura.playkit.ads

import androidx.annotation.NonNull

// Collection of classes and enums required for Advertising

/**
 * Advertising configuration
 * Pass the list of AdBreaks
 * @param advertising: List of AdBreaks
 * @param adTimeUnit: AdBreak position in Seconds or Milliseconds
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
 * @see <a href="Ad Waterfalling">https://github.com/kaltura/kaltura-player-js/blob/master/docs/advertisement-layout-management.md#waterfalling</a>
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

data class PKAdvertisingAdInfo(var adDescription: String,
                               var adTitle: String,
                               var adPodTimeOffset: Long,
                               var podIndex: Int,
                               var podCount: Int)

/**
 * For Preroll and Postroll, always configure POSITION
 * For Midroll, PERCENTAGE or EVERY can be configured
 * PERCENTAGE and EVERY can not be mixed in one configuration (Only one can be configured at a time)
 * For EVERY: Only one Midroll ad should be configured (Because the adbreak will be played every X seconds)
 */
enum class AdBreakPositionType {
    POSITION, // Play AdBreak at this specific second
    PERCENTAGE, // Play AdBreak at nth percentage (Position percentage of the media length)
    EVERY // Play AdBreak at every n seconds (60 means on every 1 min ad will be played)
}

enum class AdTimeUnit {
    SECONDS,
    MILISECONDS
}

enum class AdType {
    AD_URL,
    AD_RESPONSE
}

// Ad's State
enum class AdState {
    READY,
    PLAYING,
    PLAYED,
    ERROR
}

// Adroll type
internal enum class AdrollType {
    ADBREAK,
    ADPOD,
    AD
}
