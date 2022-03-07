package com.kaltura.playkit

import androidx.annotation.NonNull
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSource
import com.kaltura.playkit.player.CustomLoadErrorHandlingPolicy

/**
 * Request Configuration for [com.kaltura.playkit.player.ExoPlayerWrapper.getHttpDataSourceFactory]
 */
data class PKRequestConfig @JvmOverloads constructor(@NonNull var crossProtocolRedirectEnabled: Boolean = false,
                                                     @NonNull var readTimeoutMs: Int = DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                                                     @NonNull var connectTimeoutMs: Int = DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                                                     /* Maximum number of times to retry a load in the case of a load error, before propagating the error.*/
                                                     @NonNull var maxRetries: Int = CustomLoadErrorHandlingPolicy.LOADABLE_RETRY_COUNT_UNSET) {
    class Builder {

        private var crossProtocolRedirectEnabled: Boolean = false
        private var readTimeoutMs: Int = DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS
        private var connectTimeoutMs: Int = DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS
        private var maxRetries = CustomLoadErrorHandlingPolicy.LOADABLE_RETRY_COUNT_UNSET

        fun setCrossProtocolRedirectEnabled(crossProtocolRedirectEnabled: Boolean): Builder {
            this.crossProtocolRedirectEnabled = crossProtocolRedirectEnabled
            return this
        }

        fun setReadTimeoutMs(readTimeoutMs: Int): Builder {
            this.readTimeoutMs = readTimeoutMs
            return this
        }

        fun setConnectTimeoutMs(connectTimeoutMs: Int): Builder {
            this.connectTimeoutMs = connectTimeoutMs
            return this
        }

        fun setMaxRetries(maxRetries: Int): Builder {
            this.maxRetries = maxRetries
            return this
        }

        fun build(): PKRequestConfig {
            return PKRequestConfig(crossProtocolRedirectEnabled, readTimeoutMs, connectTimeoutMs, maxRetries)
        }
    }
}
