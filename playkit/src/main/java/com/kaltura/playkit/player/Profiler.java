package com.kaltura.playkit.player;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.playkit.PKMediaConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.EventListener;

public class Profiler {

    private static final Map<String, Object> experiments = new LinkedHashMap<>();
    private static final String DEFAULT_PROFILER_CLASS_NAME = "com.kaltura.playkit.profiler.DefaultProfiler";
    private static final String DEFAULT_PROFILER_INIT_METHOD = "init";

    private static boolean initDone;
    private static Method profilerFactory;
    private AnalyticsListener exoAnalyticsListener = new AnalyticsListener() {};

    private static Profiler NULL = new Profiler(); // a profiler that doesn't do anything.

    // Called by the app
    public static void setExperiment(String key, Object value) {
        experiments.put(key, value);
    }

    // Called by PlayerController
    @NonNull
    static Profiler get() {

        if (profilerFactory != null) {
            try {
                final Object profilerObj = profilerFactory.invoke(null);
                if (profilerObj instanceof Profiler) {
                    return ((Profiler) profilerObj);
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return NULL;
    }

    // Called by PlayKitManager
    public static void init(Context context) {

        // If profiler is either ALREADY initialized or WON'T initialize, do nothing.
        if (initDone) return;

        try {
            // Get the DefaultProfiler class
            final Class<?> profilerClass = Class.forName(DEFAULT_PROFILER_CLASS_NAME);
            // Call static DefaultProfiler.init(context)
            profilerClass.getDeclaredMethod(DEFAULT_PROFILER_INIT_METHOD, Context.class).invoke(null, context);

            // Save the factory for later
            profilerFactory = profilerClass.getDeclaredMethod("maybeCreate");

        } catch (ClassNotFoundException e) {
            // No profiler -- ignore.
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        initDone = true;
    }

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
}
