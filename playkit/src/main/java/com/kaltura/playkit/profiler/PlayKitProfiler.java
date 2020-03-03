package com.kaltura.playkit.profiler;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kaltura.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.player.ExoPlayerWrapper;
import com.kaltura.playkit.player.LoadControlBuffers;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.player.PKMediaSourceConfig;
import com.kaltura.playkit.player.PlayerEngine;
import com.kaltura.playkit.player.PlayerSettings;
import com.kaltura.playkit.player.Profiler;
import com.kaltura.playkit.player.ProfilerFactory;
import com.kaltura.playkit.utils.Consts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import okhttp3.EventListener;

import static com.kaltura.playkit.profiler.ProfilerUtil.SEPARATOR;
import static com.kaltura.playkit.profiler.ProfilerUtil.field;
import static com.kaltura.playkit.profiler.ProfilerUtil.timeField;
import static com.kaltura.playkit.profiler.ProfilerUtil.toJSON;

public class PlayKitProfiler {

    // Static constants
    private static final PKLog pkLog = PKLog.get("PlayKitProfiler");

    // Dev mode: shorter logs, write to local file, always enable
    private static final boolean devMode = false;
    private static final int SEND_INTERVAL_DEV = 10;    // in seconds
    private static final int SEND_PERCENTAGE_DEV = 100; // always

    private static final int SEND_INTERVAL_PROD = 120;  // 2 minutes
    private static final int SEND_INTERVAL_SEC = devMode ? SEND_INTERVAL_DEV : SEND_INTERVAL_PROD;

    private static final String CONFIG_BASE_URL = "https://player-profiler.s3.amazonaws.com/configs/";

    private static final Map<String, String> experiments = new LinkedHashMap<>();
    private static final int PERCENTAGE_MULTIPLIER = 100;

    // Configuration
    private static String postURL;
    private static float sendPercentage = devMode ? SEND_PERCENTAGE_DEV : 0;

    // Static setup
    private static Handler ioHandler;
    private static boolean initialized;
    private static DisplayMetrics metrics;
    private static File externalFilesDir;   // for debug logs
    private static String packageName;
    private static String networkType;
    private static String deviceType;
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private final ExoPlayerProfilingListener analyticsListener = new ExoPlayerProfilingListener(this);
    private final EventListener.Factory okListenerFactory = call -> new OkHttpListener(PlayKitProfiler.this, call);
    private final Set<String> serversLookedUp = new HashSet<>();
    long sessionStartTime;
    private String sessionId;

    private int chunkCount = 0;

    // We need a reference to the player, but make sure not to keep it alive.
    @Nullable
    private WeakReference<ExoPlayerWrapper> playerEngine;

    private PlayKitProfiler() {

        ioHandler.post(new Runnable() {
            @Override
            public void run() {

                // Send queue content to the server
                // TODO: 17/02/2019 also send to server if more than 1000 lines?
                sendLogChunk();

                ioHandler.postDelayed(this, SEND_INTERVAL_SEC * Consts.MILLISECONDS_MULTIPLIER);
            }
        });
    }

    /**
     * Initialize the static part of the profiler -- load the config and store it,
     * create IO thread and handler. Must be called by the app to enable the profiler.
     */
    public static void init(Context context, String token) {

        // This only has to happen once.
        if (initialized) {
            return;
        }

        if (token == null) {
            return;
        }

        synchronized (PlayKitProfiler.class) {

            // Ask again, after sync.
            if (initialized) {
                return;
            }

            final Context appContext = context.getApplicationContext();

            HandlerThread handlerThread = new HandlerThread("ProfilerIO", Process.THREAD_PRIORITY_BACKGROUND);
            handlerThread.start();
            ioHandler = new Handler(handlerThread.getLooper());

            ioHandler.post(() -> downloadConfig(token));

            initMembers(appContext);

            initialized = true;

            // Set the profiler factory.
            // Convert sendPercentage from 0..100 to 0..1 (divide by 100).
            // Math.random() returns a float between 0 and 1.
            // Check if the number is smaller than the send percentage -- if it is, a profiler is
            // created. If not, null is returned (and ProfilerFactory will return the NOOP profiler).

            // For example, with sendPercentage = 5 (5%). Scaled to 0..1, it's 0.05. If the
            // random number is smaller than 0.05 a profiler is created and returned.

            // As a result, 5 in every 100 calls to the factory will create a real profiler.
            ProfilerFactory.setFactory(() ->
                    Math.random() < sendPercentage / PERCENTAGE_MULTIPLIER ? new PlayKitProfiler().profilerImp : null);
        }
    }

    /**
     * Set an experiment key+value that will be added to the log. There's no limit to the number of
     * experiments that can be set, but the key must be unique (using the same key again overrides
     * the value).
     *
     * @param key       A unique string that describes the property being tested
     * @param value     the value of the key, must be a string, a number or a boolean.
     */
    public static void setExperiment(String key, Object value) {
        if (key == null) {
            pkLog.w("setExperiment: key is null");
            return;
        }

        final String strValue;
        if (value instanceof String) {
            strValue = "{" + value + "}";
        } else if (value instanceof Number || value instanceof Boolean) {
            strValue = value.toString();
        } else {
            pkLog.w("setExperiment: value type is not valid (" + (value != null ? value.getClass().toString() : null) + "); ignored");
            return;
        }

        experiments.put(key, strValue);
    }

    private void logExperiments() {

        List<String> values = new ArrayList<>();

        for (Map.Entry<String, String> entry : experiments.entrySet()) {

            final String key = entry.getKey();
            final String value = entry.getValue();

            values.add(key + "=" + value);
        }

        log("Experiments", TextUtils.join("\t", values));
    }

    private static void initMembers(final Context context) {

        packageName = context.getPackageName();

        metrics = context.getResources().getDisplayMetrics();
        networkType = Utils.getNetworkClass(context);
        deviceType = Utils.getDeviceType(context);


        if (devMode) {
            externalFilesDir = context.getExternalFilesDir(null);
        }
    }

    private static void downloadConfig(String token) {
        final byte[] bytes;

        // Download
        try {
            bytes = Utils.executeGet(CONFIG_BASE_URL + token + ".json", null);

            if (bytes == null || bytes.length == 0) {
                pkLog.w("Nothing returned from executeGet");
                return;
            }

            parseConfig(bytes);

        } catch (IOException e) {
            pkLog.w("Failed to download config", e);
        }
    }

    private static void parseConfig(byte[] bytes) {
        try {
            Config config = new Gson().fromJson(new String(bytes), Config.class);
            sendPercentage = config.sendPercentage;
            postURL = config.postURL;
        } catch (JsonParseException e) {
            pkLog.e("Failed to parse config", e);
        }
    }

    private void sendLogChunk() {

        if (sessionId == null) {
            return;
        }

        pkLog.d("sendLogChunk");

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

        int chunkIndex = chunkCount;
        chunkCount++;

        if (Looper.myLooper() == ioHandler.getLooper()) {
            postChunk(string, chunkIndex);
        } else {
            ioHandler.post(() -> postChunk(string, chunkIndex));
        }

        // TODO: 17/02/2019 what if there's no network when sending the log?

        if (devMode && externalFilesDir != null) {

            pkLog.d("writing log to disk");
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

    private void postChunk(String string, int chunkIndex) {
        if (postURL == null) {
            pkLog.w("No POST URL");
            return;
        }

        try {
            Utils.executePost(postURL + "?mode=addChunk&sessionId=" + sessionId + "&index=" + chunkIndex, string.getBytes(), null);
        } catch (IOException e) {
            // FIXME: 03/09/2018 Is it bad that we lost this log chunk?
            pkLog.e("Failed sending log", e);
            pkLog.e(string);
        }
    }

    void log(String event, String... strings) {
        StringBuilder sb = startLog(event);
        logPayload(sb, strings);
        endLog(sb);
    }

    private StringBuilder startLog(String event) {

        // Pre-allocate the string to something reasonable
        StringBuilder sb = new StringBuilder(100);
        sb
                .append(SystemClock.elapsedRealtime() - sessionStartTime)
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

    private void logWithPlaybackInfo(String event, PlayerEngine playerEngine, String... strings) {

        StringBuilder sb = startLog(event);

        logPayload(sb, timeField("pos", playerEngine.getCurrentPosition()), timeField("buf", playerEngine.getBufferedPosition()));
        logPayload(sb, strings);

        endLog(sb);
    }

    void maybeLogServerInfo(final Uri url) {
        final String hostName = url.getHost();
        if (serversLookedUp.contains(hostName)) {
            return;
        }

        ioHandler.postAtFrontOfQueue(() -> logServerInfo(hostName));
    }

    private void logServerInfo(String hostName) {

        if (serversLookedUp.contains(hostName)) {
            return;
        }

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


    private void closeSession() {
        sendLogChunk();
    }

    void logWithPlaybackInfo(String event, String... strings) {
        if (playerEngine != null) {
            logWithPlaybackInfo(event, playerEngine.get(), strings);
        }
    }

    private Profiler profilerImp = new Profiler() {
        @Override
        public void setPlayerEngine(PlayerEngine engine) {

            if (engine instanceof ExoPlayerWrapper) {
                playerEngine = new WeakReference<>(((ExoPlayerWrapper) engine));
            } else {
                playerEngine = null;   // other engines are not supported
            }
        }

        @Override
        public void onPrepareStarted(final PKMediaSourceConfig sourceConfig) {

            final Uri sourceUrl = sourceConfig.getRequestParams().url;

            log("PrepareStarted",
                    field("engine", playerEngine.get().getClass().getSimpleName()),
                    field("source", sourceUrl.toString()));

            maybeLogServerInfo(sourceUrl);
        }

        @Override
        public void newSession(final String sessionId, PlayerSettings playerSettings) {

            if (sessionId != null) {
                // close current session
                closeSession();
            }

            PlayKitProfiler.this.sessionId = sessionId;
            PlayKitProfiler.this.chunkCount = 0;
            if (sessionId == null) {
                return;     // the null profiler
            }

            PlayKitProfiler.this.sessionStartTime = SystemClock.elapsedRealtime();
            PlayKitProfiler.this.logQueue.clear();

            PlayKitProfiler.this.serversLookedUp.clear();

            pkLog.d("New profiler with sessionId: " + sessionId);

            log("StartSession",
                    field("now", System.currentTimeMillis()),
                    field("strNow", new Date().toString()),
                    field("sessionId", sessionId),
                    field("packageName", packageName)
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
                    field("screenDpi", metrics.xdpi + "x" + metrics.ydpi),
                    field("deviceType", deviceType),
                    field("networkType", networkType)
            );

            log("PlayerSettings",
                    field("allowClearLead", playerSettings.allowClearLead()),
                    field("useTextureView", playerSettings.useTextureView()));

            final LoadControlBuffers loadControl = playerSettings.getLoadControlBuffers();
            if (loadControl != null) {
                log("PlayerLoadControl",
                        field("minBufferLenMs", loadControl.getMinPlayerBufferMs()),
                        field("maxBufferLenMs", loadControl.getMaxPlayerBufferMs()),
                        field("minRebufferLenMs", loadControl.getMinBufferAfterReBufferMs()),
                        field("minSeekBufferLenMs", loadControl.getMinBufferAfterInteractionMs())
                );
            }


            logExperiments();
        }

        @Override
        public AnalyticsListener getExoAnalyticsListener() {
            return analyticsListener;
        }

        @Override
        public void onApplicationPaused() {
            log("ApplicationPaused");
        }

        @Override
        public void onApplicationResumed() {
            log("onApplicationResumed");
        }

        @Override
        public void onSetMedia(PKMediaConfig mediaConfig) {
            JsonObject json = new JsonObject();
            json.add("entry", toJSON(mediaConfig.getMediaEntry()));
            json.addProperty("startPosition", mediaConfig.getStartPosition());

            log("SetMedia", field("config", json.toString()));
        }

        @Override
        public void onSeekRequested(long position) {
            logWithPlaybackInfo("SeekRequested", timeField("targetPosition", position));
        }

        @Override
        public void onPauseRequested() {
            logWithPlaybackInfo("PauseRequested");
        }

        @Override
        public void onReplayRequested() {
            logWithPlaybackInfo("ReplayRequested");
        }

        @Override
        public void onPlayRequested() {
            logWithPlaybackInfo("PlayRequested");
        }

        @Override
        public void onSessionFinished() {
            closeSession();
        }

        @Override
        public void onDurationChanged(long duration) {
            log("DurationChanged", timeField("duration", duration));
        }

        @Override
        public EventListener.Factory getOkListenerFactory() {
            return okListenerFactory;
        }
    };

    private static class Config {
        String postURL;
        float sendPercentage;
    }
}
