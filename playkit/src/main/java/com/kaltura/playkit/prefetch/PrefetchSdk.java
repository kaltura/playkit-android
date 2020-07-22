package com.kaltura.playkit.prefetch;

import android.content.Context;
import android.net.Uri;

import com.kaltura.android.exoplayer2.database.DatabaseProvider;
import com.kaltura.android.exoplayer2.database.ExoDatabaseProvider;
import com.kaltura.android.exoplayer2.offline.ActionFileUpgradeUtil;
import com.kaltura.android.exoplayer2.offline.DefaultDownloadIndex;
import com.kaltura.android.exoplayer2.offline.DefaultDownloaderFactory;
import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.kaltura.android.exoplayer2.offline.StreamKey;
import com.kaltura.android.exoplayer2.scheduler.Requirements;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.FileDataSource;
import com.kaltura.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.android.exoplayer2.upstream.cache.CacheDataSource;
import com.kaltura.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.kaltura.android.exoplayer2.upstream.cache.SimpleCache;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.ExoCacheProvider;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PrefetchSdk {

    private static final PKLog log = PKLog.get("PrefetchSdk");

    private static PrefetchSdk shared;
    private DatabaseProvider databaseProvider;
    private final String userAgent;
    private final Context context;
    private DownloadManager downloadManager;
    private DownloadTracker downloadTracker;
    private File downloadDirectory;
    private Cache downloadCache;

    private static final String DOWNLOAD_ACTION_FILE = "actions";
    private static final String DOWNLOAD_TRACKER_ACTION_FILE = "tracked_actions";
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
    private static final int MAX_SIMULTANEOUS_DOWNLOADS = 2;


    private PrefetchSdk(Context context) {
        this.context = context.getApplicationContext();

        userAgent = Util.getUserAgent(this.context, "PrefetchSDK");

        initDownloadManager();
    }

    /** Returns a {@link DataSource.Factory}. */
    public DataSource.Factory buildDataSourceFactory() {
        DefaultDataSourceFactory upstreamFactory =
                new DefaultDataSourceFactory(context, buildHttpDataSourceFactory());
        return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache());
    }

    /** Returns a {@link HttpDataSource.Factory}. */
    private HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSourceFactory(userAgent);
    }

    DownloadManager getDownloadManager() {
        return downloadManager;
    }

    private synchronized void initDownloadManager() {
        if (downloadManager == null) {
            DefaultDownloadIndex downloadIndex = new DefaultDownloadIndex(getDatabaseProvider());
            upgradeActionFile(
                    DOWNLOAD_ACTION_FILE, downloadIndex, /* addNewDownloadsAsCompleted= */ false);
            upgradeActionFile(
                    DOWNLOAD_TRACKER_ACTION_FILE, downloadIndex, /* addNewDownloadsAsCompleted= */ true);
            DownloaderConstructorHelper downloaderConstructorHelper =
                    new DownloaderConstructorHelper(getDownloadCache(), buildHttpDataSourceFactory());
            downloadManager =
                    new DownloadManager(
                            context, downloadIndex, new DefaultDownloaderFactory(downloaderConstructorHelper));
            downloadTracker =
                    new DownloadTracker(/* context= */ context, buildDataSourceFactory(), downloadManager);
            downloadManager.addListener(new DownloadManager.Listener() {
                @Override
                public void onInitialized(DownloadManager downloadManager) {

                }

                @Override
                public void onDownloadsPausedChanged(DownloadManager downloadManager, boolean downloadsPaused) {

                }

                @Override
                public void onDownloadChanged(DownloadManager downloadManager, Download download) {

                }

                @Override
                public void onDownloadRemoved(DownloadManager downloadManager, Download download) {

                }

                @Override
                public void onIdle(DownloadManager downloadManager) {

                }

                @Override
                public void onRequirementsStateChanged(DownloadManager downloadManager, Requirements requirements, int notMetRequirements) {

                }

                @Override
                public void onWaitingForRequirementsChanged(DownloadManager downloadManager, boolean waitingForRequirements) {

                }
            });
        }
    }

    private void upgradeActionFile(
            String fileName, DefaultDownloadIndex downloadIndex, boolean addNewDownloadsAsCompleted) {
        try {
            ActionFileUpgradeUtil.upgradeAndDelete(
                    new File(getDownloadDirectory(), fileName),
                    /* downloadIdProvider= */ null,
                    downloadIndex,
                    /* deleteOnFailure= */ true,
                    addNewDownloadsAsCompleted);
        } catch (IOException e) {
            log.e("Failed to upgrade action file: " + fileName, e);
        }
    }

    private DatabaseProvider getDatabaseProvider() {
        if (databaseProvider == null) {
            databaseProvider = new ExoDatabaseProvider(context);
        }
        return databaseProvider;
    }

    protected static CacheDataSourceFactory buildReadOnlyCacheDataSource(
            DataSource.Factory upstreamFactory, Cache cache) {
        return new CacheDataSourceFactory(
                cache,
                upstreamFactory,
                new FileDataSource.Factory(),
                /* cacheWriteDataSinkFactory= */ null,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                /* eventListener= */ null);
    }

    protected synchronized Cache getDownloadCache() {
        if (downloadCache == null) {
            File downloadContentDirectory = new File(getDownloadDirectory(), DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache =
                    new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(), getDatabaseProvider());
        }
        return downloadCache;
    }

    private File getDownloadDirectory() {
        if (downloadDirectory == null) {
            downloadDirectory = context.getExternalFilesDir(null);
            if (downloadDirectory == null) {
                downloadDirectory = context.getFilesDir();
            }
        }
        return downloadDirectory;
    }

    public static PrefetchSdk shared(Context context) {
        if (shared == null) {
            synchronized (PrefetchSdk.class) {
                if (shared == null) {
                    shared = new PrefetchSdk(context);
                }
            }
        }
        return shared;
    }


    public void install(Player player) {
        player.getSettings()
                .setCacheProvider(new ExoCacheProvider() {
                    @Override
                    public DataSource.Factory buildDataSourceFactory() {
                        return PrefetchSdk.this.buildDataSourceFactory();
                    }

                    @Override
                    public Cache getCache() {
                        return downloadCache;
                    }
                });
    }

    public interface OnComplete<T> {
        void accept(Map<String, String> t, Exception e);
    }
}
