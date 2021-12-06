package com.kaltura.playkit.ads

interface PKAdvertising {

    /**
     * App may call it whenever it wants to play an AdBreak
     *
     * *WARNING* For Live Media, if this API is used then after
     * every adPlayback, player will go back to the live edge
     *
     * @param adBreak AdBreak to be played
     */
    fun playAdNow(adBreak: AdBreak?)
}

