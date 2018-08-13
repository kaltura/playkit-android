package com.kaltura.playkit.player;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Profiler {

    private static final String TAG = "Profiler";
    static final String SEPARATOR = "\t";
    private static final int FLUSH_INTERVAL_SEC = 5;
    private static final int NO_ACTIVITY_LIMIT = 5*60/FLUSH_INTERVAL_SEC;   // 5 minutes
    private static final HashMap<String, Profiler> profilers = new HashMap<>();

    private static File logDir;
    private static boolean started;
    private static Handler flushHandler;
    private static String currentExperimentId;
    private static DisplayMetrics metrics;

    private final ConcurrentLinkedQueue<String> logQueue;

    private int noActivityCounter;
    long startTime = SystemClock.elapsedRealtime();
    final boolean active;

    private ExoPlayerProfilingListener playerEngineListener;

    public static boolean isStarted() {
        return started;
    }

    public synchronized static void start(Context context) {

        if (started) {
            return;
        }

        metrics = context.getResources().getDisplayMetrics();

        logDir = new File(context.getExternalFilesDir(""), "ProfilerLogs");
        logDir.mkdir();

        HandlerThread handlerThread = new HandlerThread("ProfilerFlush", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        flushHandler = new Handler(handlerThread.getLooper());

        started = true;
    }

    private Profiler(final String sessionId) {

        logQueue = new ConcurrentLinkedQueue<>();

        if (sessionId == null) {
            active = false;
            return;
        }

        active = true;

        final File logFile = new File(logDir, sessionId.replace(':','_') + ".txt");

        log("StartSession", "time=" + System.currentTimeMillis(), "screenSize=" + metrics.widthPixels + "x" + metrics.heightPixels, "screenDpi=" + metrics.xdpi + "x" + metrics.ydpi);

        flushHandler.post(new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "run: size=" + logQueue.size());
                BufferedOutputStream outputStream = null;
                try {
                    outputStream = new BufferedOutputStream(new FileOutputStream(logFile, true));
                    Iterator<String> iterator = logQueue.iterator();
                    boolean hasNewData = false;
                    while (iterator.hasNext()) {
                        hasNewData = true;
                        String entry = iterator.next();
                        outputStream.write(entry.getBytes());
                        outputStream.write('\n');

                        iterator.remove();
                    }
                    if (!hasNewData) {
                        noActivityCounter++;
                    } else {
                        noActivityCounter = 0;
                    }
                } catch (FileNotFoundException e) {
                    // Unlikely
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (noActivityCounter > NO_ACTIVITY_LIMIT) {
                    log("ProfilerSessionReleased");
                    profilers.remove(sessionId);
                } else {
                    flushHandler.postDelayed(this, FLUSH_INTERVAL_SEC * 1000);
                }
            }
        });
    }

    AnalyticsListener getExoPlayerListener(PlayerEngine playerEngine) {
        return new ExoPlayerProfilingListener(this, playerEngine);
    }

    static Profiler get(String sessionId) {

        if (!started || sessionId == null) {
            return null;//nullProfiler();
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

    public static void setCurrentExperimentId(String currentExperimentId) {
        Profiler.currentExperimentId = currentExperimentId;
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
        flushHandler.post(new Runnable() {
            @Override
            public void run() {
                logQueue.add(sb.toString());
                Log.d(TAG, "endLog: size=" + logQueue.size());
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
