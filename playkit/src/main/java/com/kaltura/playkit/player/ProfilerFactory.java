package com.kaltura.playkit.player;

import android.support.annotation.NonNull;

public abstract class ProfilerFactory {

    // a profiler that doesn't do anything.
    @NonNull private static final Profiler NULL = new Profiler() {};

    // Factory, by default returns the NULL profiler
    @NonNull private static ProfilerFactory profilerFactory = new ProfilerFactory() {};

    // Called by the profiler when it's ready for use.
    public static void setProfilerFactory(@NonNull ProfilerFactory profilerFactory) {
        ProfilerFactory.profilerFactory = profilerFactory;
    }

    // Called by PlayerController. Always returns a profiler, but the profiler may be a no-op.
    @NonNull
    static Profiler get() {
        return profilerFactory.getProfiler();
    }

    protected Profiler getProfiler() {
        return NULL;
    }
}
