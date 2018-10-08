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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

class ConfigFile {
    String putLogURL;
    float sendPercentage;
}

public class Profiler {

    private static PKLog pkLog = PKLog.get("Profiler");

    private static final boolean devMode = true;
    private static final boolean localMode = true;

    private static final String CONFIG_CACHE_FILENAME = "profilerConfig.json";
    private static final String CONFIG_URL = "https://s3.amazonaws.com/player-profiler/config.json";
    private static final String DEFAULT_POST_URL = "https://3vbje2fyag.execute-api.us-east-1.amazonaws.com/default/profilog";
    private static final float DEFAULT_SEND_PERCENTAGE = 100; // FIXME: 03/09/2018
    private static final int MAX_CONFIG_SIZE = 10240;

    static final String SEPARATOR = "\t";
    private static final int SEND_INTERVAL_SEC = localMode ? 10 : 300;   // Report every 5 minutes
    private static final int NO_ACTIVITY_LIMIT = localMode ? 60 : 2;     // Close profiler after 2 empty intervals
    private static final HashMap<String, Profiler> profilers = new HashMap<>();

    private static boolean started;
    private static Handler ioHandler;
    private static String currentExperiment;
    private static DisplayMetrics metrics;
    private static Set<String> closedSessions = new HashSet<>();
    private static File externalFilesDir;   // for debug logs

    final long startTime = SystemClock.elapsedRealtime();
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private int noActivityCounter;
    private final String sessionId;

    // Config
    private static String postURL = DEFAULT_POST_URL;
    private static float sendPercentage = DEFAULT_SEND_PERCENTAGE;

    static String field(String name, String value) {
        if (value == null) {
            return null;
        }
        return name + "={" + value + "}";
    }

    static String field(String name, long value) {
        return name + "=" + value;
    }

    static String field(String name, boolean value) {
        return name + "=" + value;
    }

    static String field(String name, float value) {
        return String.format(Locale.US, "%s=%03f", name, value);
    }

    static String timeField(String name, long value) {
        return field(name, value / 1000f);
    }

    public boolean isActive() {
        return true;
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

        if (devMode) {
            externalFilesDir = context.getExternalFilesDir(null);
        }

        started = true;
    }

    private Profiler(final String sessionId) {

        this.sessionId = sessionId;
        if (sessionId == null) {
            return;     // the null profiler
        }

        pkLog.d("New profiler with sessionId: " + sessionId);

        log("StartSession",
                field("strNow", new Date().toString()),
                field("sessionId", sessionId),
                field("now", System.currentTimeMillis()),
                field("screenSize", metrics.widthPixels + "x" + metrics.heightPixels),
                field("screenDpi", metrics.xdpi + "x" + metrics.ydpi)
                );

        log("PlayKit",
                field("version", PlayKitManager.VERSION_STRING),
                field("clientTag", PlayKitManager.CLIENT_TAG)
                );

        log("Platform",
                field("name", "Android"),
                field("apiLevel", Build.VERSION.SDK_INT),
                field("chipset", MediaSupport.DEVICE_CHIPSET),
                field("brand", Build.BRAND),
                field("model", Build.MODEL),
                field("manufacturer", Build.MANUFACTURER),
                field("device", Build.DEVICE),
                field("tags", Build.TAGS),
                field("fingerprint", Build.FINGERPRINT)
                );


        if (currentExperiment != null) {
            log("Experiment", field("info", currentExperiment));
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

    private static boolean shouldEnable() {
        return Math.random() < (sendPercentage / 100);
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

        if (!localMode) {
            try {
                Utils.executePost(postURL + "?mode=addChunk&sessionId=" + sessionId, string.getBytes(), null);
            } catch (IOException e) {
                // FIXME: 03/09/2018 Is it bad that we lost this log chunk?
                pkLog.e("Failed sending log", e);
                pkLog.e(string);
            }
        }

        if (devMode && externalFilesDir != null) {
            // Write to disk
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(new File(externalFilesDir, sessionId + ".txt"), true));
                writer.append(string);
                writer.newLine();
                writer.flush();

            } catch (IOException e) {
                pkLog.e("Failed saving local log", e);
            } finally {
                Utils.safeClose(writer);
            }
        }
    }

    AnalyticsListener getAnalyticsListener(PlayerEngine playerEngine) {
        return new ExoPlayerProfilingListener(this, playerEngine);
    }

    static Profiler get(String sessionId) {

        if (!started || sessionId == null || closedSessions.contains(sessionId) || !shouldEnable()) {
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

    void log(String event, String... strings) {
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

    private void logPayload(StringBuilder sb, String... strings) {
        for (String s : strings) {
            if (s == null) {
                continue;
            }
            sb.append(SEPARATOR).append(s);
        }
    }

    private void endLog(final StringBuilder sb) {
        logQueue.add(sb.toString());
    }

    void logWithPlaybackInfo(String event, PlayerEngine playerEngine, String... strings) {

        StringBuilder sb = startLog(event);

        logPayload(sb, timeField("pos", playerEngine.getCurrentPosition()), timeField("buf", playerEngine.getBufferedPosition()));
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

        if (entry == null) {
            return null;
        }

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

        log("SetMedia", field("config", json.toString()));
    }

    public void onPrepareStarted(final PlayerEngine playerEngine, final PKMediaSourceConfig sourceConfig) {
        log("PrepareStarted", field("engine", playerEngine.getClass().getSimpleName()), field("source", sourceConfig.getUrl().toString()), field("useTextureView", sourceConfig.playerSettings.useTextureView()));
    }

    public void onSeekRequested(PlayerEngine playerEngine, long position) {
        logWithPlaybackInfo("SeekRequested", playerEngine, timeField("targetPosition", position));
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
        log("BandwidthSample", field("bandwidth", bitrate));
    }

    public void onSessionFinished() {
        ioHandler.post(new Runnable() {
            @Override
            public void run() {
                closeSession();
            }
        });
    }

    private static Profiler nullProfiler() {
        return new Profiler(null) {

            @Override
            public boolean isActive() {
                return false;
            }

            @Override
            void log(String event, String... strings) {}

            @Override
            void logWithPlaybackInfo(String event, PlayerEngine playerEngine, String... strings) {}

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

            @Override
            public void onSessionFinished() {}

            @Override
            public void onViewportSizeChange(PlayerEngine playerEngine, int width, int height) {}

            @Override
            public void onDurationChanged(long duration) {}
        };
    }

    private void closeSession() {
        profilers.remove(sessionId);
        sendLogChunk();
        closedSessions.add(sessionId);
    }

    public void onViewportSizeChange(PlayerEngine playerEngine, int width, int height) {
        log("ViewportSizeChange", field("width", width), field("height", height));
    }

    public void onDurationChanged(long duration) {
        log("DurationChanged", timeField("duration", duration));
    }
}
