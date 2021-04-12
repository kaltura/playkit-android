package com.kaltura.playkit

import androidx.annotation.NonNull
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSource

data class PKRequestConfiguration @JvmOverloads constructor(@NonNull var crossProtocolRedirectEnabled: Boolean = false,
                                                            @NonNull var readTimeoutMs: Int = DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                                                            @NonNull var connectTimeoutMs: Int = DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
