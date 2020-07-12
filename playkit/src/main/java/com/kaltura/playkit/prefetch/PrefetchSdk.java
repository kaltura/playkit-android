package com.kaltura.playkit.prefetch;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.kaltura.android.exoplayer2.offline.StreamKey;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.FileDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.android.exoplayer2.upstream.cache.CacheDataSource;
import com.kaltura.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.kaltura.android.exoplayer2.upstream.cache.SimpleCache;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.ExoCacheProvider;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrefetchSdk {

    private static final String TAG = "PrefetchSdk";
//    private static final String SERVICE_URL = "http://192.168.164.17/ptr.json";
    private static final String SERVICE_URL = "https://***REMOVED***.execute-api.eu-central-1.amazonaws.com/default/getEntriesForPrefetch";
    private static final String URI_STRING = "https://cdnapisec.kaltura.com/p/***REMOVED***/sp/***REMOVED***00/playManifest/entryId/1_aworxd15/format/applehttp/protocol/https/a.m3u8";
    private static PrefetchSdk shared;
    private final PrefetchDb db;
    private final Handler dbHandler;
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
        db = Room.databaseBuilder(this.context, PrefetchDb.class, "history").build();

        HandlerThread dbThread = new HandlerThread("dbThread");
        dbThread.start();
        dbHandler = new Handler(dbThread.getLooper());

        userAgent = Util.getUserAgent(this.context, "PrimeTimeDemo");

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
            DownloaderConstructorHelper downloaderConstructorHelper =
                    new DownloaderConstructorHelper(getDownloadCache(), buildHttpDataSourceFactory());
            downloadManager =
                    new DownloadManager(
                            downloaderConstructorHelper,
                            MAX_SIMULTANEOUS_DOWNLOADS,
                            DownloadManager.DEFAULT_MIN_RETRY_COUNT,
                            new File(getDownloadDirectory(), DOWNLOAD_ACTION_FILE));
            downloadTracker =
                    new DownloadTracker(
                            context,
                            buildDataSourceFactory(),
                            new File(getDownloadDirectory(), DOWNLOAD_TRACKER_ACTION_FILE));
            downloadManager.addListener(downloadTracker);
        }
    }

    private synchronized Cache getDownloadCache() {
        if (downloadCache == null) {
            File downloadContentDirectory = new File(getDownloadDirectory(), DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache = new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor());
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
    private static CacheDataSourceFactory buildReadOnlyCacheDataSource(
            DefaultDataSourceFactory upstreamFactory, Cache cache) {
        return new CacheDataSourceFactory(
                cache,
                upstreamFactory,
                new FileDataSourceFactory(),
                /* cacheWriteDataSinkFactory= */ null,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                /* eventListener= */ null);
    }

    public void reportWatchedEntry(@NonNull String entryId) {
        final long now = System.currentTimeMillis();
        dbHandler.post(() -> db.dao().insert(new WatchedEntry(entryId, now)));
    }

    public void prefetchNow(OnComplete<Map<String, String>> onComplete) {
        dbHandler.post(() -> {
            try {
                final Map<String, String> strings = prefetchNow();
                onComplete.accept(strings, null);
            } catch (JSONException | IOException e) {
                onComplete.accept(null, e);
            }
        });
    }

    private Map<String, String> prefetchNow() throws JSONException, IOException {
        final Map<String, String> entries = submit();

        prefetchEntries(entries);

        db.dao().clearHistory();

        return entries;
    }

    private void prefetchEntries(Map<String, String> entries) {

        Log.d(TAG, "prefetching entries: " + entries);


        downloadTracker.startDownload(entries);
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

    private Map<String, String> submit() throws JSONException, IOException {
        final List<WatchedEntry> all = db.dao().getAllWatched();

        JSONArray entries = new JSONArray();
        for (WatchedEntry watchedEntry : all) {
            entries.put(watchedEntry.entryId);
        }

        JSONObject request = new JSONObject()
                .put("entries", entries);

        final byte[] bytes = Utils.executePost(SERVICE_URL, request.toString().getBytes(), null);

        JSONObject response = new JSONObject(new String(bytes));

        Map<String, String> prefetchList = new LinkedHashMap<>();

        JSONArray array = response.getJSONArray("entries");
        for (int i = 0, length = array.length(); i < length; i++) {
            final JSONObject object = array.getJSONObject(i);
            prefetchList.put(object.getString("id"), object.getString("playManifestUrl"));
        }

        return prefetchList;
    }

    public void install(Player player) {
        player.getSettings()
                .setCacheProvider(new ExoCacheProvider() {
                    @Override
                    public DataSource.Factory buildDataSourceFactory() {
                        return PrefetchSdk.this.buildDataSourceFactory();
                    }

                    @Override
                    public List<StreamKey> getOfflineStreamKeys(Uri uri) {
                        return downloadTracker.getOfflineStreamKeys(uri);
                    }
                });
    }

    public interface OnComplete<T> {
        void accept(Map<String, String> t, Exception e);
    }
}
