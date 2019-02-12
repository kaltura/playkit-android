package com.kaltura.playkit.player;

import android.support.annotation.NonNull;

import com.kaltura.playkit.PKLog;

import java.util.concurrent.Callable;

public abstract class ProfilerFactory {

    private static final PKLog log = PKLog.get("ProfilerFactory");


    private static Callable<Profiler> profilerFactory;

    // Called by the profiler when it's ready for use.
    public static void setFactory(@NonNull Callable<Profiler> profilerFactory) {
        ProfilerFactory.profilerFactory = profilerFactory;
    }

    // Called by PlayerController. Always returns a profiler, but the profiler may be a no-op.
    @NonNull
    static Profiler get() {
        Profiler profiler = null;

        if (profilerFactory != null) {
            try {
                profiler = profilerFactory.call();
            } catch (Exception e) {
                // This should never happen, but it's harmless so just log.
                log.e("Failed to get a profiler", e);
            }
        }

        return profiler != null ? profiler : Profiler.NOOP;
    }
}
