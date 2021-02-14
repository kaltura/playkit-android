package com.kaltura.playkit.player

import com.kaltura.playkit.utils.Consts

data class LlLiveConfiguration(var targetOffsetMs: Long = Consts.TIME_UNSET, var minOffsetMs: Long = Consts.TIME_UNSET,
                               var maxOffsetMs: Long = Consts.TIME_UNSET, var minPlaybackSpeed: Float = Consts.RATE_UNSET,
                               var maxPlaybackSpeed: Float = Consts.RATE_UNSET)