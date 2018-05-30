package com.kaltura.playkit.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.utils.Consts;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class Profiler {

    private static final String TAG = "Profiler";
    private static final String SEPARATOR = "\t";
    private static final int FLUSH_INTERVAL_SEC = 2;
    private static final int NO_ACTIVITY_LIMIT = 5*60/FLUSH_INTERVAL_SEC;   // 5 minutes
    private static final HashMap<String, Profiler> profilers = new HashMap<>();

    private static File logDir;
    private static boolean started;
    private static Handler flushHandler;
    private static String currentExperimentId;
    private static DisplayMetrics metrics;

    private ConcurrentLinkedQueue<String> log = new ConcurrentLinkedQueue<>();
    private AtomicInteger logSize = new AtomicInteger(0);

    private int noActivityCounter;
    private long startTime = System.currentTimeMillis();

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

        if (sessionId == null) {
            return;
        }

        final File logFile = new File(logDir, sessionId.replace(':','_') + ".txt");

        log("StartSession", startTime, metrics.widthPixels + "x" + metrics.heightPixels, metrics.xdpi + "x" + metrics.ydpi);

        flushHandler.post(new Runnable() {
            @Override
            public void run() {

                int size = logSize.get();
                if (size > 0) {
                    noActivityCounter = 0;
                    Log.d(TAG, "Flushing " + size + " messages to disk");
                    BufferedOutputStream outputStream = null;
                    try {
                        outputStream = new BufferedOutputStream(new FileOutputStream(logFile, true));
                        Iterator<String> iterator = log.iterator();
                        while (size > 0 && iterator.hasNext()) {
                            String entry = iterator.next();
                            outputStream.write(entry.getBytes());
                            outputStream.write('\n');

                            iterator.remove();
                            logSize.decrementAndGet();
                            size--;
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
                } else {
                    noActivityCounter++;
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

    static Profiler get(String sessionId) {

        if (!started) {
            return nullProfiler;
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
        logStrings(sb, strings);
        endLog(sb);
    }

    private StringBuilder startLog(String event) {
        StringBuilder sb = new StringBuilder(100);
        logStrings(sb, System.currentTimeMillis() - startTime, event);
        return sb;
    }

    private void logStrings(StringBuilder sb, Object... strings) {
        for (Object s : strings) {
            sb.append(SEPARATOR).append(s);
        }
    }

    private void endLog(StringBuilder sb) {
        log.add(sb.toString());
        logSize.incrementAndGet();
    }

    void log(String event, PlayerEngine playerEngine, Object... strings) {

        StringBuilder sb = startLog(event);

        logStrings(sb, playerEngine.getCurrentPosition() / 1000f, playerEngine.getBufferedPosition() / 1000f);
        logStrings(sb, strings);

        endLog(sb);
    }

    private static JsonObject toJSON(PKMediaEntry entry) {
        JsonObject json = new JsonObject();

        json.addProperty("id", entry.getId());
        json.addProperty("duration", entry.getDuration()/1000f);
        json.addProperty("type", entry.getMediaType().name());

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
                array.add(params.getScheme().name());
            }
            json.add("drm", array);
        }

        return json;
    }

    private static String durationString(long duration) {
        if (duration == Consts.TIME_UNSET) {
            return null;
        } else {
            return String.valueOf(duration / 1000f);
        }
    }

    void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig) {
        JsonObject json = new JsonObject();
        json.add("entry", toJSON(mediaConfig.getMediaEntry()));
        json.addProperty("startPosition", mediaConfig.getStartPosition()/1000f);

        log("SetMedia", json.toString());
    }

    public void onPrepareStarted(PlayerEngine playerEngine, PKMediaSourceConfig sourceConfig) {
        log("PrepareStarted", playerEngine.getClass().getSimpleName(), sourceConfig.getUrl(), sourceConfig.playerSettings.useTextureView());
    }

    public void onSeekRequested(PlayerEngine playerEngine, long position) {
        log("SeekRequested", playerEngine, position/1000f);
    }

    public void onSeekEnded(PlayerEngine playerEngine) {
        log("SeekEnded", playerEngine);
    }

    public void onPauseRequested(PlayerEngine playerEngine) {
        log("PauseRequested", playerEngine);
    }

    public void onReplayRequested(PlayerEngine playerEngine) {
        log("ReplayRequested", playerEngine);
    }

    public void onPlayRequested(PlayerEngine playerEngine) {
        log("PlayRequested", playerEngine);
    }

    public void onPlayerError(PlayerEngine playerEngine, String errorStr) {
        log("PlayerError", playerEngine, errorStr);
    }

    public void onPlayerIdle(PlayerEngine playerEngine, boolean shouldPlay) {
        log("PlayerIdle", playerEngine);
    }

    public void onPlayerBuffering(PlayerEngine playerEngine, boolean shouldPlay) {
        log("PlayerBuffering", playerEngine, shouldPlay, durationString(playerEngine.getDuration()));
    }

    public void onPlayerReady(PlayerEngine playerEngine, boolean shouldPlay) {
        log("PlayerReady", playerEngine, shouldPlay, durationString(playerEngine.getDuration()));
    }

    public void onPlayerEnded(PlayerEngine playerEngine, boolean shouldPlay) {
        log("PlayerEnded", playerEngine);
    }

    public void onBandwidthEstimation(PlayerEngine playerEngine, long bitrate) {
        log("BandwidthEstimation", playerEngine, bitrate);
    }

    public void onTracksChanged(PlayerEngine playerEngine, String videoTrackId, int videoTrackBitrate, String videoTrackCodec, String audioTrackId, int audioTrackBitrate, String audioTrackCodec) {
        PlayerView view = playerEngine.getView();
        log("TracksChanged", playerEngine, view.getWidth() + "x" + view.getHeight(), videoTrackId, videoTrackBitrate, videoTrackCodec, audioTrackId, audioTrackBitrate, audioTrackCodec);
    }

    public void onTransferStarted(PlayerEngine playerEngine, Uri uri) {
//        log("TransferStarted", playerEngine, uri, niceHash(uri));
    }

//    private String niceHash(Object uri) {
//        return "[" + (uri.hashCode() + (long)Integer.MAX_VALUE + 1) + "]";
//    }

    public void onTransferEnd(PlayerEngine playerEngine, Uri uri, int size, long time) {
        float sec = time / 1000f;
        log("TransferEnd", playerEngine, size, sec, size * 8 / sec / 1024 / 1024, uri.getLastPathSegment(), uri);
    }

    private static Profiler nullProfiler = new Profiler(null) {
        @Override
        void log(String event, Object... strings) {}

        @Override
        void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig) {}

        @Override
        public void onPrepareStarted(PlayerEngine playerEngine, PKMediaSourceConfig sourceConfig) {}

        @Override
        public void onSeekRequested(PlayerEngine playerEngine, long position) {}

        @Override
        public void onSeekEnded(PlayerEngine playerEngine) {}

        @Override
        public void onPauseRequested(PlayerEngine playerEngine) {}

        @Override
        public void onReplayRequested(PlayerEngine playerEngine) {}

        @Override
        public void onPlayRequested(PlayerEngine playerEngine) {}

        @Override
        public void onPlayerError(PlayerEngine playerEngine, String errorStr) {}

        @Override
        public void onPlayerIdle(PlayerEngine playerEngine, boolean shouldPlay) {}

        @Override
        public void onPlayerBuffering(PlayerEngine playerEngine, boolean shouldPlay) {}

        @Override
        public void onPlayerReady(PlayerEngine playerEngine, boolean shouldPlay) {}

        @Override
        public void onPlayerEnded(PlayerEngine playerEngine, boolean shouldPlay) {}

        @Override
        public void onBandwidthEstimation(PlayerEngine playerEngine, long bitrate) {}

        @Override
        public void onTracksChanged(PlayerEngine playerEngine, String videoTrackId, int videoTrackBitrate, String videoTrackCodec, String audioTrackId, int audioTrackBitrate, String audioTrackCodec) {}

        @Override
        public void onTransferStarted(PlayerEngine playerEngine, Uri uri) {}

        @Override
        public void onTransferEnd(PlayerEngine playerEngine, Uri uri, int size, long time) {}
    };
}
