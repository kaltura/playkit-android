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
import okhttp3.OkHttpClient;

public abstract class Profiler {

    static final Map<String, Object> experiments = new LinkedHashMap<>();

    static boolean initDone;
    private static Method profilerFactory;

    private static Profiler NULL = new Profiler() {

        private AnalyticsListener exoAnalyticsListener = new AnalyticsListener() {};

        @Override
        public void setPlayerEngine(ExoPlayerWrapper playerEngine) {}

        @Override
        public void newSession(String sessionId, PlayerSettings playerSettings) {}

        @Override
        public AnalyticsListener getExoAnalyticsListener() {
            return exoAnalyticsListener;
        }

        @Override
        public void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig) {}

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

        @Override
        public void onSessionFinished() {}

        @Override
        public void onDurationChanged(long duration) {}

        @Override
        public void startNetworkListener(OkHttpClient.Builder builder) {}

        @Override
        public EventListener.Factory getOkListenerFactory() {
            return null;
        }
    };

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

    public abstract void setPlayerEngine(ExoPlayerWrapper playerEngine);

    // Called by PlayKitManager
    public static void init(Context context) {

        // If profiler is either ALREADY initialized or WON'T initialize, do nothing.
        if (initDone) return;

        try {
            // Get the DefaultProfiler class
            final Class<?> profilerClass = Class.forName("com.kaltura.playkit.profiler.DefaultProfiler");
            // Call static DefaultProfiler.init(context)
            profilerClass.getDeclaredMethod("init", Context.class).invoke(null, context);

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

    public static Map<String, Object> getExperiments() {
        return Collections.unmodifiableMap(experiments);
    }

    public static void setExperiment(String key, Object value) {
        experiments.put(key, value);
    }

    public abstract void newSession(String sessionId, PlayerSettings playerSettings);

    public abstract AnalyticsListener getExoAnalyticsListener();

    public abstract void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig);

    public abstract void onPrepareStarted(PlayerEngine playerEngine, PKMediaSourceConfig sourceConfig);

    public abstract void onSeekRequested(PlayerEngine playerEngine, long position);

    public abstract void onPauseRequested(PlayerEngine playerEngine);

    public abstract void onReplayRequested(PlayerEngine playerEngine);

    public abstract void onPlayRequested(PlayerEngine playerEngine);

    public abstract void onBandwidthSample(PlayerEngine playerEngine, long bitrate);

    public abstract void onSessionFinished();

    public abstract void onDurationChanged(long duration);

    public abstract void startNetworkListener(OkHttpClient.Builder builder);

    public abstract EventListener.Factory getOkListenerFactory();
}
