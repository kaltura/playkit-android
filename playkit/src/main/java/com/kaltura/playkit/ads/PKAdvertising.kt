package com.kaltura.playkit.ads

interface PKAdvertising {

    fun playAdNow(vastAdTag: List<AdBreak>)

    fun getCurrentAd()

}