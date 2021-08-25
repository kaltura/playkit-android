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
                                                     /* Minimum number of times to retry a load in the case of a load error, before propagating the error.*/
                                                     @NonNull var minLoadableRetryCount: Int = CustomLoadErrorHandlingPolicy.MIN_LOADABLE_RETRY_COUNT)
