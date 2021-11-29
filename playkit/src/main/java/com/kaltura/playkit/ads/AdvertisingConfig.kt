package com.kaltura.playkit.ads

// Collection of classes and enums required for Advertising

/**
 * Advertising configuration
 * Pass the list the AdBreaks
 */
data class AdvertisingConfig(val advertising: List<AdBreak?>?, val adTimeUnit: AdTimeUnit)// TODO: change it to ADBreak as well

/**
 * AdBreak: Pre, Mid, Post
 * Each AdBreak may contain List of AdPod
 * Each AdPod may contain a list of Ads (List of Ads is being used to do waterfalling)
 * @see <a href="Ad Waterfalling">https://github.com/kaltura/kaltura-player-js/blob/master/docs/advertisement-layout-management.md#waterfalling</a>
 */
data class AdBreak(var adBreakPositionType: AdBreakPositionType = AdBreakPositionType.POSITION, var position: Long, val ads: List<List<String>>)

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

// Ad Break Config
internal data class AdBreakConfig(val adBreakPositionType: AdBreakPositionType, var adPosition: Long, var adBreakState: AdState, val adPodList: List<AdPodConfig>?)

// Ad list contains waterfalling ads as well.
internal data class AdPodConfig(var adPodState: AdState, val adList: List<Ad>?)

// Single Ad
internal data class Ad(var adState: AdState, val ad: String)

// Ad's State
internal enum class AdState {
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
