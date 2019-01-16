package com.kaltura.playkit.player;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
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

    private static String currentExperiment;
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


        if (currentExperiment != null) {
            log("Experiment", field("info", currentExperiment));
        }
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
        DefaultProfiler.currentExperiment = currentExperiment;
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
