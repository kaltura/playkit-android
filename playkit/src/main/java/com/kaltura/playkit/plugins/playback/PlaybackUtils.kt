package com.kaltura.playkit.plugins.playback

import com.kaltura.playkit.PKRequestParams
import com.kaltura.playkit.PlayKitManager
import com.kaltura.playkit.Utils

class PlaybackUtils {

    companion object {

        @JvmStatic
        fun getPKRequestParams(requestParams: PKRequestParams,
                               playSessionId: String?, applicationName: String?,
                               httpHeaders: Map<String?, String?>?): PKRequestParams {
            val url = requestParams.url

            url?.let {
                it.path?.let { path ->
                    if (path.contains("/playManifest/")) {
                        var alt = url.buildUpon()
                                .appendQueryParameter("clientTag", PlayKitManager.CLIENT_TAG)
                                .appendQueryParameter("playSessionId", playSessionId).build()
                        if (!applicationName.isNullOrEmpty()) {
                            alt = alt.buildUpon().appendQueryParameter("referrer", Utils.toBase64(applicationName.toByteArray())).build()
                        }
                        val lastPathSegment = requestParams.url.lastPathSegment
                        if (!lastPathSegment.isNullOrEmpty() && lastPathSegment.endsWith(".wvm")) {
                            // in old android device it will not play wvc if url is not ended in wvm
                            alt = alt.buildUpon().appendQueryParameter("name", lastPathSegment).build()
                        }
                        setCustomHeaders(requestParams, httpHeaders)
                        return PKRequestParams(alt, requestParams.headers)
                    }
                }
            }

            setCustomHeaders(requestParams, httpHeaders)
            return requestParams
        }

        private fun setCustomHeaders(requestParams: PKRequestParams, httpHeaders: Map<String?, String?>?) {
            httpHeaders?.let { header ->
                if (header.isNotEmpty()) {
                    header.forEach { (key, value) ->
                        key?.let { requestKey ->
                            value?.let { requestValue ->
                                requestParams.headers[requestKey] = requestValue
                            }
                        }
                    }
                }
            }
        }
    }
}
