package com.kaltura.playkit.profiler;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.kaltura.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.PlayKitManager.ProfilerConfig;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.player.ExoPlayerWrapper;
import com.kaltura.playkit.player.PKMediaSourceConfig;
import com.kaltura.playkit.player.PlayerEngine;
import com.kaltura.playkit.player.PlayerSettings;
import com.kaltura.playkit.player.Profiler;
import com.kaltura.playkit.player.Profiler.Event;
import com.kaltura.playkit.player.ProfilerFactory;
import com.kaltura.playkit.utils.Consts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import okhttp3.EventListener;

public class PlayKitProfiler {

    public static final String FORMAT_VERSION = "2.0";

    // Static constants
    private static final PKLog pkLog = PKLog.get("PlayKitProfiler");

    // Dev mode: shorter logs, write to local file, always enable
    private static final boolean devMode = true;
    private static final int SEND_INTERVAL_DEV = 30;    // sec
    private static final int SEND_PERCENTAGE_DEV = 100; // always

    private static final int SEND_INTERVAL_PROD = 60;  // sec
    private static final int SEND_INTERVAL_SEC = devMode ? SEND_INTERVAL_DEV : SEND_INTERVAL_PROD;

    private static final float DEFAULT_SEND_PERCENTAGE = devMode ? SEND_PERCENTAGE_DEV : 0; // Start disabled

    static final float MSEC_MULTIPLIER_FLOAT = 1000f;

    private static final JsonObject experiments = new JsonObject();
    private static final int PERCENTAGE_MULTIPLIER = 100;
    // Configuration
    private static String postURL = "https://dtvqq1tbxf.execute-api.us-east-1.amazonaws.com/default/profilerLogCollector";
    private static float sendPercentage = DEFAULT_SEND_PERCENTAGE;
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

    private Profiler profilerImp;

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
    public static void init(Context context, ProfilerConfig config) {

        // This only has to happen once.
        if (initialized) {
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

            ioHandler.post(() -> applyConfig(config));

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
            ProfilerFactory.setFactory(() -> {
                final double random = Math.random();
                final boolean enable = random < sendPercentage / PERCENTAGE_MULTIPLIER;
                pkLog.d("Profiler enabled for session? " + enable);
                if (enable) {
                    return new PlayKitProfiler().getProfilerImp();
                }
                return null;
            });
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
    public static void setExperiment(@NonNull String key, @Nullable Object value) {

        if (value instanceof Number) {
            experiments.addProperty(key, (Number) value);
        } else if (value instanceof Boolean) {
            experiments.addProperty(key, (Boolean) value);
        } else if (value instanceof Character) {
            experiments.addProperty(key, (Character) value);
        } else if (value == null) {
            experiments.add(key, JsonNull.INSTANCE);
        } else {
            // Anything else (including a String) is used as a string
            experiments.addProperty(key, value.toString());
        }
    }

    private void logExperiments() {
        logStart("Experiments")
                .addAll(experiments)
                .end();
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

    private static void applyConfig(ProfilerConfig config) {
        postURL = config.postURL;
        sendPercentage = config.sendPercentage;
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
            final String url = postURL + "?sessionId=" + sessionId + "&index=" + chunkIndex;
            if (pkLog.isLoggable(PKLog.Level.verbose)) {
                pkLog.v("POST to " + url);
                pkLog.v(string);
            }
            Utils.executePost(url, string.getBytes(), null);
        } catch (IOException e) {
            // FIXME: 03/09/2018 Is it bad that we lost this log chunk?
            pkLog.e("Failed sending log", e);
            pkLog.e(string);
        }
    }

    public long timestamp() {
        return SystemClock.elapsedRealtime() - sessionStartTime;
    }

    public Event logStart(String name) {
        return new Event(this, name);
    }

    public Event logStart(String name, @Nullable PlayerEngine playerEngine) {
        final Event event = logStart(name);
        if (playerEngine != null) {
            event.addTime("pos", playerEngine.getCurrentPosition());
            event.addTime("buf", playerEngine.getBufferedPosition());
        }
        return event;
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

        logStart("ServerInfo")
                .add("hostName", hostName)
                .add("canonicalHostName", canonicalHostName)
                .add("hostIp", hostIp)
                .addTime("lookupTime", lookupTime)
                .add("lookupError", error).end();

        serversLookedUp.add(hostName);
    }


    private void closeSession() {
        sendLogChunk();
    }

    Event logWithPlaybackInfo(String event) {
        return logStart(event, playerEngine != null ? playerEngine.get() : null);
    }

    private Profiler getProfilerImp() {
        if (profilerImp == null) {
            profilerImp = createProfilerImp();
        }

        return profilerImp;
    }

    private Profiler createProfilerImp() {

        pkLog.d("Creating a real profiler");

        return new Profiler() {
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

                logStart("PrepareStarted")
                        .add("engine", "ExoPlayer")
                        .add("source", sourceUrl.toString()).end();

                maybeLogServerInfo(sourceUrl);
            }

            @Override
            public void newSession(final String sessionId, PlayerSettings playerSettings) {

                pkLog.d("New session " + sessionId);

                if (PlayKitProfiler.this.sessionId != null) {
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

                logStart("StartSession")
                        .add("now", System.currentTimeMillis())
                        .add("strNow", new Date().toString())
                        .add("sessionId", sessionId)
                        .add("appId", packageName)
                        .add("version", FORMAT_VERSION)
                        .end();

                logStart("PlayKit")
                        .add("version", PlayKitManager.VERSION_STRING)
                        .add("clientTag", PlayKitManager.CLIENT_TAG)
                        .add("type", "sdk").end();

                logStart("Platform")
                        .add("os", "Android")
                        .add("screenSize", metrics.widthPixels + "x" + metrics.heightPixels)
                        .add("screenDpi", metrics.xdpi + "x" + metrics.ydpi)
                        .add("deviceType", deviceType)
                        .add("networkType", networkType).end();

                logStart("AndroidInfo")
                        .addAll(ToJson.buildInfoJson())
                        .end();

                logStart("PlayerSettings")
                        .addAll(ToJson.toJson(playerSettings))
                        .end();

                logExperiments();
            }

            @Override
            public AnalyticsListener getExoAnalyticsListener() {
                return analyticsListener;
            }

            @Override
            public void onApplicationPaused() {
                logStart("ApplicationPaused").end();
            }

            @Override
            public void onApplicationResumed() {
                logStart("ApplicationResumed").end();
            }

            @Override
            public void onSetMedia(PKMediaConfig mediaConfig) {

                logStart("SetMedia")
                        .add("entry", ToJson.toJson(mediaConfig.getMediaEntry()))
                        .add("startPosition", mediaConfig.getStartPosition())
                        .end();
            }

            @Override
            public void onSeekRequested(long position) {
                logWithPlaybackInfo("SeekRequested")
                        .addTime("targetPosition", position)
                        .end();
            }

            @Override
            public void onPauseRequested() {
                logWithPlaybackInfo("PauseRequested").end();
            }

            @Override
            public void onReplayRequested() {
                logWithPlaybackInfo("ReplayRequested").end();
            }

            @Override
            public void onPlayRequested() {
                logWithPlaybackInfo("PlayRequested").end();
            }

            @Override
            public void onSessionFinished() {
                closeSession();
            }

            @Override
            public void onDurationChanged(long duration) {
                logStart("DurationChanged")
                        .addTime("duration", duration)
                        .end();
            }

            @Override
            public EventListener.Factory getOkListenerFactory() {
                return okListenerFactory;
            }
        };
    }

    public void append(JsonObject jo) {
        logQueue.add(jo.toString());
    }
}
