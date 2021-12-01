package com.kaltura.playkit.ads

interface PKAdvertising {

    /**
     * App may call it whenever it wants to play an AdBreak
     * //TODO: Add more details here
     * @param adBreak AdBreak to be played
     */
    fun playAdNow(adBreak: AdBreak?)

}