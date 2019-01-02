package com.kaltura.playkit;

import android.net.Uri;

import com.google.android.exoplayer2.offline.StreamKey;
import com.google.android.exoplayer2.upstream.DataSource;

import java.util.List;

public interface ExoCacheProvider extends PKCacheProvider {
    DataSource.Factory buildDataSourceFactory();
    List<StreamKey> getOfflineStreamKeys(Uri uri);
}
