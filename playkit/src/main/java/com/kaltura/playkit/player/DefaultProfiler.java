package com.kaltura.playkit.player;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.google.android.exoplayer2.C;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kaltura.playkit.PKDrmParams;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import okhttp3.OkHttpClient;

class ConfigFile {
    String putLogURL;
    float sendPercentage;
}

class DefaultProfiler extends Profiler {

    static final String SEPARATOR = "\t";
    private static final boolean devMode = true;
    private static final int SEND_INTERVAL_SEC = devMode ? 10 : 300;   // Report every 5 minutes
    private static final float DEFAULT_SEND_PERCENTAGE = 1; // Start disabled
    static float sendPercentage = DEFAULT_SEND_PERCENTAGE;
    private static final String CONFIG_CACHE_FILENAME = "profilerConfig.json";
    private static final String CONFIG_URL = "https://s3.amazonaws.com/player-profiler/config.json-";
    private static final String DEFAULT_POST_URL = "https://3vbje2fyag.execute-api.us-east-1.amazonaws.com/default/profilog";
    static String postURL = DEFAULT_POST_URL;
    private static final int MAX_CONFIG_SIZE = 10240;
    static Handler ioHandler;
    static boolean initialized;

    private static DisplayMetrics metrics;
    private static File externalFilesDir;   // for debug logs
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    long startTime;
    private ExoPlayerProfilingListener analyticsListener;
    private String sessionId;

    private final Set<String> serversLookedUp = new HashSet<>();


    DefaultProfiler() {

        ioHandler.post(new Runnable() {
            @Override
            public void run() {

                // Send queue content to the server
                sendLogChunk();

                ioHandler.postDelayed(this, SEND_INTERVAL_SEC * 1000);
            }
        });

    }

    public static boolean isInitialized() {
        return initialized;
    }

    static void initMembers(final Context context) {

        metrics = context.getResources().getDisplayMetrics();

        if (devMode) {
            externalFilesDir = context.getExternalFilesDir(null);
        }
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
        return String.format(Locale.US, "%s=%.03f", name, value);
    }

    static String timeField(String name, long value) {
        return value == C.TIME_UNSET ? field(name, null) : field(name, value / 1000f);
    }

    static Profiler maybeCreate() {

        return initialized && Math.random() < (sendPercentage / 100) ? new DefaultProfiler() : null;
    }

    static void downloadConfig(Context context) {
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

    static void loadCachedConfig(Context context) {
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

    static String nullable(String name, String value) {
        if (value == null) {
            return name + "=null";
        }

        return field(name, value);
    }

    public static void init(Context context) {
        if (initialized) {
            return;
        }

        final Context appContext = context.getApplicationContext();

        synchronized (Profiler.class) {

            // Load cached config. Will load from network later, in a handler thread.
            loadCachedConfig(appContext);

            HandlerThread handlerThread = new HandlerThread("ProfilerIO", Process.THREAD_PRIORITY_BACKGROUND);
            handlerThread.start();
            ioHandler = new Handler(handlerThread.getLooper());

            ioHandler.post(() -> downloadConfig(appContext));

            initMembers(appContext);

            initialized = true;
        }
    }

    @Override
    void newSession(final String sessionId) {

        if (this.sessionId != null) {
            // close current session
            closeSession();
        }

        this.sessionId = sessionId;
        if (sessionId == null) {
            return;     // the null profiler
        }

        this.startTime = SystemClock.elapsedRealtime();
        this.logQueue.clear();

        this.serversLookedUp.clear();

        pkLog.d("New profiler with sessionId: " + sessionId);

        log("StartSession",
                field("now", System.currentTimeMillis()),
                field("strNow", new Date().toString()),
                field("sessionId", sessionId),
                // TODO: remove screenSize and screenDpi after backend is updated
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
                field("fingerprint", Build.FINGERPRINT),
                field("screenSize", metrics.widthPixels + "x" + metrics.heightPixels),
                field("screenDpi", metrics.xdpi + "x" + metrics.ydpi)
        );

        logExperiments();
    }

    private void logExperiments() {

        List<String> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : experiments.entrySet()) {

            final String key = entry.getKey();
            final Object value = entry.getValue();
            String strValue;

            if (key == null) {
                continue;
            }

            if (value instanceof String) {
                strValue = "{" + value + "}";
            } else if (value instanceof Number || value instanceof Boolean) {
                strValue = value.toString();
            } else {
                continue;
            }

            values.add(key + "=" + strValue);
        }

        log("Experiments", TextUtils.join("\t", values));
    }



    @Override
    void startListener(ExoPlayerWrapper playerEngine) {
        if (analyticsListener == null) {
            analyticsListener = new ExoPlayerProfilingListener(this, playerEngine);
        }
        playerEngine.addAnalyticsListener(analyticsListener);
    }

    @Override
    void stopListener(ExoPlayerWrapper playerEngine) {
        playerEngine.removeAnalyticsListener(analyticsListener);
    }

    private void sendLogChunk() {

        if (sessionId == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = logQueue.iterator();
        while (iterator.hasNext()) {
            String entry = iterator.next();
            sb.append(entry).append('\n');
            iterator.remove();
        }

        if (sb.length() == 0) {
            return;
        }

        final String string = sb.toString();

        if (Looper.myLooper() == ioHandler.getLooper()) {
            postChunk(string);
        } else {
            ioHandler.post(() -> postChunk(string));
        }

        if (devMode && externalFilesDir != null) {
            // Write to disk
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(new File(externalFilesDir, sessionId.replace(':', '_') + ".prof.log"), true));
                writer.append(string);
                writer.newLine();
                writer.newLine();
                writer.flush();

            } catch (IOException e) {
                pkLog.e("Failed saving local log", e);
            } finally {
                Utils.safeClose(writer);
            }
        }
    }

    private void postChunk(String string) {
        if (postURL == null) {
            pkLog.w("No POST URL");
            return;
        }

        try {
            Utils.executePost(postURL + "?mode=addChunk&sessionId=" + sessionId, string.getBytes(), null);
        } catch (IOException e) {
            // FIXME: 03/09/2018 Is it bad that we lost this log chunk?
            pkLog.e("Failed sending log", e);
            pkLog.e(string);
        }
    }

    @Override
    void setCurrentExperiment(String currentExperiment) {
    }

    void log(String event, String... strings) {
        StringBuilder sb = startLog(event);
        logPayload(sb, strings);
        endLog(sb);
    }

    private StringBuilder startLog(String event) {
//        pkLog.v("Profiler.startLog: " + sessionId + " " + event);

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

    @Override
    void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig) {
        JsonObject json = new JsonObject();
        json.add("entry", toJSON(mediaConfig.getMediaEntry()));
        json.addProperty("startPosition", mediaConfig.getStartPosition());

        log("SetMedia", field("config", json.toString()));
    }

    @Override
    void onPrepareStarted(final PlayerEngine playerEngine, final PKMediaSourceConfig sourceConfig) {

        final Uri sourceUrl = sourceConfig.getUrl();

        final PlayerSettings playerSettings = sourceConfig.playerSettings;

        final LoadControlBuffers loadControl = playerSettings.getLoadControlBuffers();
        if (loadControl != null) {
            log("PlayerLoadControl",
                    field("minBufferLenMs", loadControl.getMinPlayerBufferMs()),
                    field("maxBufferLenMs", loadControl.getMaxPlayerBufferMs()),
                    field("minRebufferLenMs", loadControl.getMinBufferAfterReBufferMs()),
                    field("minSeekBufferLenMs", loadControl.getMinBufferAfterInteractionMs())
            );
        }

        log("PrepareStarted",
                field("engine", playerEngine.getClass().getSimpleName()),
                field("source", sourceUrl.toString()),
                field("useTextureView", playerSettings.useTextureView()));


        maybeLogServerInfo(sourceUrl);
    }

    void maybeLogServerInfo(final Uri url) {
        final String hostName = url.getHost();
        if (serversLookedUp.contains(hostName)) {
            return;
        }

        ioHandler.postAtFrontOfQueue(() -> logServerInfo(hostName));
    }

    private void logServerInfo(String hostName) {

        if (serversLookedUp.contains(hostName)) return;

        String canonicalHostName = null;
        String hostIp = null;
        String error = null;
        long lookupTime = -1;

        try {
            long start = SystemClock.elapsedRealtime();
            InetAddress address = InetAddress.getByName(hostName);
            hostIp = address.getHostAddress();
            lookupTime = SystemClock.elapsedRealtime() - start;
            canonicalHostName = address.getCanonicalHostName();
        } catch (UnknownHostException e) {
            error = e.toString();
        }

        log("ServerInfo",
                field("hostName", hostName),
                field("canonicalHostName", canonicalHostName),
                field("hostIp", hostIp),
                timeField("lookupTime", lookupTime),
                field("lookupError", error)
        );

        serversLookedUp.add(hostName);
    }

    @Override
    void onSeekRequested(PlayerEngine playerEngine, long position) {
        logWithPlaybackInfo("SeekRequested", playerEngine, timeField("targetPosition", position));
    }

    @Override
    void onPauseRequested(PlayerEngine playerEngine) {
        logWithPlaybackInfo("PauseRequested", playerEngine);
    }

    @Override
    void onReplayRequested(PlayerEngine playerEngine) {
        logWithPlaybackInfo("ReplayRequested", playerEngine);
    }

    @Override
    void onPlayRequested(PlayerEngine playerEngine) {
        logWithPlaybackInfo("PlayRequested", playerEngine);
    }

    @Override
    void onBandwidthSample(PlayerEngine playerEngine, long bitrate) {
        log("BandwidthSample", field("bandwidth", bitrate));
    }

    @Override
    void onSessionFinished() {
        closeSession();
    }

    private void closeSession() {
        sendLogChunk();
    }

    @Override
    void onDurationChanged(long duration) {
        log("DurationChanged", timeField("duration", duration));
    }

    @Override
    void startNetworkListener(OkHttpClient.Builder builder) {
//        builder.eventListener(new OkHttpListener(this));
        builder.eventListenerFactory(call -> new OkHttpListener(this, call));
    }

}
