package com.kaltura.playkit.ads

import com.kaltura.playkit.plugins.ads.AdEvent

interface IMAEventsListener {
    fun allAdsCompleted()
    fun contentPauseRequested()
    fun contentResumeRequested()
    fun adError(error: AdEvent.Error)
}
