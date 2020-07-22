package com.kaltura.playkit;

import android.net.Uri;

import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.playkit.prefetch.PKCacheProvider;


public interface ExoCacheProvider extends PKCacheProvider {
    DataSource.Factory buildDataSourceFactory();
    Cache getCache();
}
