package com.kaltura.playkit.ads

import com.kaltura.playkit.plugins.ads.AdEvent

interface IMAEventsListener {
    fun allAdsCompleted()
    fun contentResumeRequested()
    fun contentPauseRequested()
    fun adCompleted()
    fun adError(error: AdEvent.Error)
}