package com.kaltura.playkit.player;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

class ConfigFile {
    String putLogURL;
    float sendPercentage;
}

public class Profiler {

    private static PKLog pkLog = PKLog.get("Profiler");

    private static final String CONFIG_CACHE_FILENAME = "profilerConfig.json";
    private static final String CONFIG_URL = "https://s3.amazonaws.com/player-profiler-pre/config/config.json";
    private static final String DEFAULT_POST_URL = "https://3vbje2fyag.execute-api.us-east-1.amazonaws.com/default/profilog";
    private static final float DEFAULT_SEND_PERCENTAGE = 100; // FIXME: 03/09/2018
    private static final int MAX_CONFIG_SIZE = 10240;

    static final String SEPARATOR = "\t";
    private static final int SEND_INTERVAL_SEC = 300;   // Report every 5 minutes
    private static final int NO_ACTIVITY_LIMIT = 2;     // Close profiler after 2 empty intervals
    private static final HashMap<String, Profiler> profilers = new HashMap<>();

    private static boolean started;
    private static Handler ioHandler;
    private static String currentExperiment;
    private static DisplayMetrics metrics;
    private static Set<String> closedSessions = new HashSet<>();

    private Boolean active;
    final long startTime = SystemClock.elapsedRealtime();
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private int noActivityCounter;
    private final String sessionId;

    // Config
    private static String postURL = DEFAULT_POST_URL;
    private static float sendPercentage = DEFAULT_SEND_PERCENTAGE;

    public boolean isActive() {
        return active != null && active;
    }

    private static void downloadConfig(Context context) {
        final byte[] bytes;

        // Download
        try {
            bytes = Utils.executeGet(CONFIG_URL, null);

            parseConfig(bytes);

        } catch (IOException e) {
            pkLog.e("Failed to download config", e);
            return;
        }

        // Save to cache
        final File cachedConfigFile = getCachedConfigFile(context);
        if (cachedConfigFile.getParentFile().canWrite()) {
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(cachedConfigFile);
                outputStream.write(bytes);
            } catch (IOException e) {
                pkLog.e("Failed to save config to cache", e);
            } finally {
                Utils.safeClose(outputStream);
            }
        }
    }

    private static void loadCachedConfig(Context context) {
        final File configFile = getCachedConfigFile(context);

        if (configFile.canRead()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(configFile);
                parseConfig(Utils.fullyReadInputStream(inputStream, MAX_CONFIG_SIZE).toByteArray());

            } catch (IOException e) {
                pkLog.e("Failed to read cached config file", e);

            } finally {
                Utils.safeClose(inputStream);
            }
        }
    }

    @NonNull
    private static File getCachedConfigFile(Context context) {
        return new File(context.getFilesDir(), CONFIG_CACHE_FILENAME);
    }

    private static void parseConfig(byte[] bytes) {
        try {
            final ConfigFile configFile = new Gson().fromJson(new String(bytes), ConfigFile.class);
            postURL = configFile.putLogURL;
            sendPercentage = configFile.sendPercentage;
        } catch (JsonParseException e) {
            pkLog.e("Failed to parse config", e);
        }
    }


    public synchronized static void init(final Context context) {

        if (started) {
            return;
        }

        // Load cached config. Will load from network later, in a handler thread.
        loadCachedConfig(context);

        metrics = context.getResources().getDisplayMetrics();

        HandlerThread handlerThread = new HandlerThread("ProfilerIO", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        ioHandler = new Handler(handlerThread.getLooper());

        ioHandler.post(new Runnable() {
            @Override
            public void run() {
                downloadConfig(context);
            }
        });

        started = true;
    }

    private Profiler(final String sessionId) {

        this.sessionId = sessionId;

        if (sendPercentage == 0 || sessionId == null || Math.random() > (sendPercentage / 100)) {
            // initialize final fields and exit.
            active = false;
            return;
        }

        active = true;


        pkLog.d("New profiler with sessionId: " + sessionId);

        log("StartSession", "sessionId=" + sessionId, "time=" + System.currentTimeMillis(),
                "screenSize=" + metrics.widthPixels + "x" + metrics.heightPixels,
                "screenDpi=" + metrics.xdpi + "x" + metrics.ydpi,
                "playkitVersion=" + PlayKitManager.VERSION_STRING,
                "playkitClientTag=" + PlayKitManager.CLIENT_TAG,
                "android:apiLevel=" + Build.VERSION.SDK_INT);

        if (currentExperiment != null) {
            log("Experiment", "info=" + currentExperiment);
        }

        ioHandler.post(new Runnable() {
            @Override
            public void run() {

                // Send queue content to the server
                sendLogChunk();

                if (noActivityCounter > NO_ACTIVITY_LIMIT) {
                    closeSession();
                } else {
                    ioHandler.postDelayed(this, SEND_INTERVAL_SEC * 1000);
                }
            }
        });
    }

    private void sendLogChunk() {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = logQueue.iterator();
        while (iterator.hasNext()) {
            String entry = iterator.next();
            sb.append(entry).append('\n');
            iterator.remove();
        }

        if (sb.length() == 0) {
            noActivityCounter++;
            return;
        }

        noActivityCounter = 0;

        final String string = sb.toString();

        try {
            Utils.executePost(postURL + "?mode=addChunk&sessionId=" + sessionId, string.getBytes(), null);
        } catch (IOException e) {
            // FIXME: 03/09/2018 Is it bad that we lost this log chunk?
            pkLog.e("Failed sending log", e);
            pkLog.e(string);
        }
    }

    AnalyticsListener getAnalyticsListener(PlayerEngine playerEngine) {
        return new ExoPlayerProfilingListener(this, playerEngine);
    }

    static Profiler get(String sessionId) {

        if (!started || sessionId == null || closedSessions.contains(sessionId)) {
            return nullProfiler();
        }

        Profiler profiler = profilers.get(sessionId);
        if (profiler == null) {
            synchronized (profilers) {
                profiler = profilers.get(sessionId);
                if (profiler == null) {
                    profiler = new Profiler(sessionId);
                    profilers.put(sessionId, profiler);
                }
            }
        }

        return profiler;
    }

    public static void setCurrentExperiment(String currentExperiment) {
        Profiler.currentExperiment = currentExperiment;
    }

    void log(String event, Object... strings) {
        StringBuilder sb = startLog(event);
        logPayload(sb, strings);
        endLog(sb);
    }

    private StringBuilder startLog(String event) {
        StringBuilder sb = new StringBuilder(100);
        sb
                .append(SystemClock.elapsedRealtime() - startTime)
                .append(SEPARATOR)
                .append(event);

        return sb;
    }

    private void logPayload(StringBuilder sb, Object... strings) {
        for (Object s : strings) {
            if (s instanceof Opt) {
                if (((Opt) s).obj == null) {
                    continue;
                }
            }
            sb.append(SEPARATOR).append(s);
        }
    }

    private void endLog(final StringBuilder sb) {
        ioHandler.post(new Runnable() {
            @Override
            public void run() {
                logQueue.add(sb.toString());
            }
        });
    }

    void logWithPlaybackInfo(String event, PlayerEngine playerEngine, Object... strings) {

        StringBuilder sb = startLog(event);

        logPayload(sb, "pos=" + playerEngine.getCurrentPosition(), "buf=" + playerEngine.getBufferedPosition());
        logPayload(sb, strings);

        endLog(sb);
    }

    private static String toString(Enum e) {
        if (e == null) {
            return "null";
        }
        return e.name();
    }

    private static JsonObject toJSON(PKMediaEntry entry) {
        JsonObject json = new JsonObject();

        json.addProperty("id", entry.getId());
        json.addProperty("duration", entry.getDuration());
        json.addProperty("type", toString(entry.getMediaType()));

        if (entry.hasSources()) {
            JsonArray array = new JsonArray();
            for (PKMediaSource source : entry.getSources()) {
                array.add(toJSON(source));
            }
            json.add("sources", array);
        }

        return json;
    }

    private static JsonObject toJSON(PKMediaSource source) {
        JsonObject json = new JsonObject();

        json.addProperty("id", source.getId());
        json.addProperty("format", source.getMediaFormat().name());
        json.addProperty("url", source.getUrl());

        if (source.hasDrmParams()) {
            JsonArray array = new JsonArray();
            for (PKDrmParams params : source.getDrmData()) {
                PKDrmParams.Scheme scheme = params.getScheme();
                if (scheme != null) {
                    array.add(scheme.name());
                }
            }
            json.add("drm", array);
        }

        return json;
    }

    void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig) {
        JsonObject json = new JsonObject();
        json.add("entry", toJSON(mediaConfig.getMediaEntry()));
        json.addProperty("startPosition", mediaConfig.getStartPosition());

        log("SetMedia", "config=" + json);
    }

    public void onPrepareStarted(final PlayerEngine playerEngine, final PKMediaSourceConfig sourceConfig) {
        log("PrepareStarted", "engine=" + playerEngine.getClass().getSimpleName(), "source=" + sourceConfig.getUrl(), "useTextureView=" + sourceConfig.playerSettings.useTextureView());
    }

    public void onSeekRequested(PlayerEngine playerEngine, long position) {
        logWithPlaybackInfo("SeekRequested", playerEngine, position);
    }

    public void onPauseRequested(PlayerEngine playerEngine) {
        logWithPlaybackInfo("PauseRequested", playerEngine);
    }

    public void onReplayRequested(PlayerEngine playerEngine) {
        logWithPlaybackInfo("ReplayRequested", playerEngine);
    }

    public void onPlayRequested(PlayerEngine playerEngine) {
        logWithPlaybackInfo("PlayRequested", playerEngine);
    }

    public void onBandwidthSample(PlayerEngine playerEngine, long bitrate) {
        log("BandwidthSample", "bandwidth=" + bitrate);
    }

    private static Profiler nullProfiler() {
        return new Profiler(null) {
            @Override
            void log(String event, Object... strings) {}

            @Override
            void logWithPlaybackInfo(String event, PlayerEngine playerEngine, Object... strings) {}

            @Override
            void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig) {}

            @Override
            public void onPrepareStarted(PlayerEngine playerEngine, PKMediaSourceConfig sourceConfig) {}

            @Override
            public void onSeekRequested(PlayerEngine playerEngine, long position) {}

            @Override
            public void onPauseRequested(PlayerEngine playerEngine) {}

            @Override
            public void onReplayRequested(PlayerEngine playerEngine) {}

            @Override
            public void onPlayRequested(PlayerEngine playerEngine) {}

            @Override
            public void onBandwidthSample(PlayerEngine playerEngine, long bitrate) {}
        };
    }

    public void onSessionFinished() {
        ioHandler.post(new Runnable() {
            @Override
            public void run() {
                closeSession();
            }
        });
    }

    private void closeSession() {
        log("ProfilerSessionReleased");
        profilers.remove(sessionId);
        sendLogChunk();
        sendSessionEnd();
        closedSessions.add(sessionId);
    }

    private void sendSessionEnd() {
        try {
            Utils.executePost(postURL + "?mode=saveChunks&sessionId=" + sessionId, null, null);
        } catch (IOException e) {
            pkLog.e("Failed sending saveChunks for session " + sessionId);
        }
    }

    static class Opt {
        Object obj;

        Opt(Object o) {
            obj = o;
        }

        @Override
        public String toString() {
            return String.valueOf(obj);
        }
    }
}
