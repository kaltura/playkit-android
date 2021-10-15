package com.kaltura.playkit.ads

interface PKAdvertising {

    fun playAdNow(vastAdTag: String)

    fun playAdNow()

    fun getCurrentAd()

}