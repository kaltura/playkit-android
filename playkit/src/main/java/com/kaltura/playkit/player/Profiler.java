package com.kaltura.playkit.player;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.EventListener;
import okhttp3.OkHttpClient;

public abstract class Profiler {

    static PKLog pkLog = PKLog.get("Profiler");
    static final Map<String, Object> experiments = new LinkedHashMap<>();

    static boolean initDone;
    private static Method profilerFactory;

    private static Profiler NULL = new Profiler() {

        private AnalyticsListener exoAnalyticsListener = new AnalyticsListener() {};

        @Override
        public void setPlayerEngine(ExoPlayerWrapper playerEngine) {}

        @Override
        void newSession(String sessionId) {}

        @Override
        AnalyticsListener getExoAnalyticsListener() {
            return exoAnalyticsListener;
        }

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

        @Override
        void startNetworkListener(OkHttpClient.Builder builder) {}

        @Override
        EventListener.Factory getOkListenerFactory() {
            return null;
        }
    };

    // Called by PlayerController
    @NonNull
    static Profiler get() {

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

        return NULL;
    }

    public abstract void setPlayerEngine(ExoPlayerWrapper playerEngine);

    // Called by PlayKitManager
    public static void init(Context context) {

        // If profiler is either ALREADY initialized or WON'T initialize, do nothing.
        if (initDone) return;

        try {
            // Get the DefaultProfiler class
            final Class<?> profilerClass = Class.forName("com.kaltura.playkit.player.DefaultProfiler");
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

    public static void setExperiment(String key, Object value) {
        experiments.put(key, value);
    }

    abstract void newSession(String sessionId);


    abstract AnalyticsListener getExoAnalyticsListener();

    abstract void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig);

    abstract void onPrepareStarted(PlayerEngine playerEngine, PKMediaSourceConfig sourceConfig);

    abstract void onSeekRequested(PlayerEngine playerEngine, long position);

    abstract void onPauseRequested(PlayerEngine playerEngine);

    abstract void onReplayRequested(PlayerEngine playerEngine);

    abstract void onPlayRequested(PlayerEngine playerEngine);

    abstract void onBandwidthSample(PlayerEngine playerEngine, long bitrate);

    abstract void onSessionFinished();

    abstract void onDurationChanged(long duration);

    abstract void startNetworkListener(OkHttpClient.Builder builder);

    abstract EventListener.Factory getOkListenerFactory();
}
