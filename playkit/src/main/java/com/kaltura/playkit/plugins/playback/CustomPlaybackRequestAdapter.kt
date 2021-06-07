package com.kaltura.playkit.plugins.playback

import com.kaltura.playkit.PKRequestParams
import com.kaltura.playkit.Player
import com.kaltura.playkit.plugins.playback.PlaybackUtils.Companion.getPKRequestParams

class CustomPlaybackRequestAdapter(applicationName: String?, player: Player) : PKRequestParams.Adapter {

    private var applicationName: String? = null
    private var playSessionId: String? = null
    private var httpHeaders: Map<String?, String?>? = null

     init {
         this.applicationName = applicationName
         updateParams(player)
     }

    fun setHttpHeaders(httpHeaders: Map<String?, String?>?) {
        this.httpHeaders = httpHeaders
    }

    override fun adapt(requestParams: PKRequestParams): PKRequestParams {
        return getPKRequestParams(requestParams, playSessionId, applicationName, httpHeaders)
    }

    override fun updateParams(player: Player) {
        playSessionId = player.sessionId
    }

    override fun getApplicationName(): String? {
        return applicationName
    }
}
