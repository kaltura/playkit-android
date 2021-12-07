package com.kaltura.playkit.ads

import androidx.annotation.NonNull

interface PKAdvertising {

    /**
     * App may call it whenever it wants to play an AdBreak
     *
     * *WARNING* For Live Media, if this API is used then after
     * every adPlayback, player will go back to the live edge
     *
     * @param adBreak AdBreak to be played
     */
    fun playAdNow(adBreak: Any?,
                  @NonNull adType: AdType = AdType.AD_URL)
}

