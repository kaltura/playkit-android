package com.kaltura.playkit.player;

import android.support.annotation.NonNull;

import java.util.concurrent.Callable;

public abstract class ProfilerFactory {

    // a profiler that doesn't do anything.
    @NonNull private static final Profiler NULL = new Profiler() {};

    @NonNull private static Callable<Profiler> profilerFactory = () -> NULL;

    // Called by the profiler when it's ready for use.
    public static void setFactory(@NonNull Callable<Profiler> profilerFactory) {
        ProfilerFactory.profilerFactory = profilerFactory;
    }

    // Called by PlayerController. Always returns a profiler, but the profiler may be a no-op.
    @NonNull
    static Profiler get() {
        Profiler profiler = null;

        try {
            profiler = profilerFactory.call();
        } catch (Exception e) {
            // ignore
        }

        return profiler != null ? profiler : NULL;
    }
}
