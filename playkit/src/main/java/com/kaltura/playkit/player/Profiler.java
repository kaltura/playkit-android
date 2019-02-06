package com.kaltura.playkit.player;

import android.support.annotation.NonNull;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.playkit.PKMediaConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.EventListener;

public abstract class Profiler {

    private static final Map<String, Object> experiments = new LinkedHashMap<>();

    private static AnalyticsListener exoAnalyticsListener = new AnalyticsListener() {};

    // a profiler that doesn't do anything.
    private static Profiler NULL = new Profiler() {};

    // Factory, by default returns the NULL profiler
    @NonNull private static Factory profilerFactory = () -> NULL;

    // Called by the profiler when it's ready for use.
    public static void setProfilerFactory(@NonNull Factory profilerFactory) {
        Profiler.profilerFactory = profilerFactory;
    }

    // Called by the app
    public static void setExperiment(String key, Object value) {
        experiments.put(key, value);
    }

    // Called by PlayerController
    @NonNull
    static Profiler get() {
        return profilerFactory.getProfiler();
    }

    // Called by the profiler to send list of experiments
    @SuppressWarnings("WeakerAccess")
    public static Map<String, Object> getExperiments() {
        return Collections.unmodifiableMap(experiments);
    }

    public void setPlayerEngine(ExoPlayerWrapper playerEngine) {}

    public void newSession(String sessionId, PlayerSettings playerSettings) {}

    public AnalyticsListener getExoAnalyticsListener() {
        return exoAnalyticsListener;
    }

    public void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig) {}

    public void onPrepareStarted(PlayerEngine playerEngine, PKMediaSourceConfig sourceConfig) {}

    public void onSeekRequested(PlayerEngine playerEngine, long position) {}

    public void onPauseRequested(PlayerEngine playerEngine) {}

    public void onReplayRequested(PlayerEngine playerEngine) {}

    public void onPlayRequested(PlayerEngine playerEngine) {}

    public void onBandwidthSample(PlayerEngine playerEngine, long bitrate) {}

    public void onSessionFinished() {}

    public void onDurationChanged(long duration) {}

    public EventListener.Factory getOkListenerFactory() {
        return null;
    }

    public interface Factory {
        Profiler getProfiler();
    }
}
