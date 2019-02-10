package com.kaltura.playkit.player;

import android.support.annotation.NonNull;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.playkit.PKMediaConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.EventListener;

public abstract class Profiler {

    @NonNull private static final Map<String, Object> experiments = new LinkedHashMap<>();

    @NonNull private static final AnalyticsListener exoAnalyticsListener = new AnalyticsListener() {};

    // a profiler that doesn't do anything.
    @NonNull private static final Profiler NULL = new Profiler() {};

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

    // Called by PlayerController. Always returns a profiler, but the profiler may be a no-op.
    @NonNull
    static Profiler get() {
        return profilerFactory.getProfiler();
    }

    // Called by the profiler to send list of experiments
    @SuppressWarnings("WeakerAccess")
    public static Map<String, Object> getExperiments() {
        return Collections.unmodifiableMap(experiments);
    }

    public void setPlayerEngine(PlayerEngine playerEngine) {}

    public void newSession(String sessionId, PlayerSettings playerSettings) {}

    public AnalyticsListener getExoAnalyticsListener() {
        return exoAnalyticsListener;
    }

    public void onSetMedia(PKMediaConfig mediaConfig) {}

    public void onPrepareStarted(PKMediaSourceConfig sourceConfig) {}

    public void onSeekRequested(long position) {}

    public void onPauseRequested() {}

    public void onReplayRequested() {}

    public void onPlayRequested() {}

    public void onBandwidthSample(long bitrate) {}

    public void onSessionFinished() {}

    public void onDurationChanged(long duration) {}

    public EventListener.Factory getOkListenerFactory() {
        return null;
    }

    public interface Factory {
        Profiler getProfiler();
    }
}
