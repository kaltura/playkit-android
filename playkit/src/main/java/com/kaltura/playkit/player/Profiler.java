package com.kaltura.playkit.player;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public abstract class Profiler {

    private static final float DEFAULT_SEND_PERCENTAGE = 100; // Start disabled
    private static final String CONFIG_CACHE_FILENAME = "profilerConfig.json";
    private static final String CONFIG_URL = "https://s3.amazonaws.com/player-profiler/config.json-";
    private static final String DEFAULT_POST_URL = "https://3vbje2fyag.execute-api.us-east-1.amazonaws.com/default/profilog";
    private static final int MAX_CONFIG_SIZE = 10240;
    static String postURL = DEFAULT_POST_URL;
    static Handler ioHandler;
    static PKLog pkLog = PKLog.get("Profiler");
    private static float sendPercentage = DEFAULT_SEND_PERCENTAGE;
    private static boolean started;
    private static Profiler NULL = new Profiler() {

        @Override
        void newSession(String sessionId) {}

        @Override
        void startListener(ExoPlayerWrapper playerEngine) {}

        @Override
        void stopListener(ExoPlayerWrapper playerEngine) {}

        @Override
        void setCurrentExperiment(String currentExperiment) {}

        @Override
        void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig) {}

        @Override
        void onPrepareStarted(PlayerEngine playerEngine, PKMediaSourceConfig sourceConfig) {}

        @Override
        void onSeekRequested(PlayerEngine playerEngine, long position) {}

        @Override
        void onPauseRequested(PlayerEngine playerEngine) {}

        @Override
        void onReplayRequested(PlayerEngine playerEngine) {}

        @Override
        void onPlayRequested(PlayerEngine playerEngine) {}

        @Override
        void onBandwidthSample(PlayerEngine playerEngine, long bitrate) {}

        @Override
        void onSessionFinished() {}

        @Override
        void onDurationChanged(long duration) {}
    };

    public static void init(Context context) {
        if (started) {
            return;
        }

        final Context appContext = context.getApplicationContext();

        synchronized (Profiler.class) {

            // Load cached config. Will load from network later, in a handler thread.
            loadCachedConfig(appContext);

            HandlerThread handlerThread = new HandlerThread("ProfilerIO", Process.THREAD_PRIORITY_BACKGROUND);
            handlerThread.start();
            ioHandler = new Handler(handlerThread.getLooper());

            ioHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadConfig(appContext);
                }
            });

            DefaultProfiler.initMembers(appContext);

            started = true;
        }
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

    static Profiler create() {
        if (!Profiler.started || !Profiler.shouldEnable()) {
            return NULL;
        }

        return new DefaultProfiler();
    }

    private static boolean shouldEnable() {
        return Math.random() < (Profiler.sendPercentage / 100);
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

    abstract void newSession(String sessionId);

    abstract void startListener(ExoPlayerWrapper playerEngine);

    abstract void stopListener(ExoPlayerWrapper playerEngine);

    abstract void setCurrentExperiment(String currentExperiment);

    abstract void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig);

    abstract void onPrepareStarted(PlayerEngine playerEngine, PKMediaSourceConfig sourceConfig);

    abstract void onSeekRequested(PlayerEngine playerEngine, long position);

    abstract void onPauseRequested(PlayerEngine playerEngine);

    abstract void onReplayRequested(PlayerEngine playerEngine);

    abstract void onPlayRequested(PlayerEngine playerEngine);

    abstract void onBandwidthSample(PlayerEngine playerEngine, long bitrate);

    abstract void onSessionFinished();

    abstract void onDurationChanged(long duration);
}
