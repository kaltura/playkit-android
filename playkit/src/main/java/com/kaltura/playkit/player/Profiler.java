package com.kaltura.playkit.player;

import android.support.annotation.NonNull;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.playkit.PKMediaConfig;

import okhttp3.EventListener;

public abstract class Profiler {
    // A no-op profiler to avoid null checks.
    @NonNull static final Profiler NOOP = new Profiler() {};

    public void setPlayerEngine(PlayerEngine playerEngine) {}

    public void newSession(String sessionId, PlayerSettings playerSettings) {}
    public void onSetMedia(PKMediaConfig mediaConfig) {}
    public void onPrepareStarted(PKMediaSourceConfig sourceConfig) {}
    public void onSeekRequested(long position) {}
    public void onPauseRequested() {}
    public void onReplayRequested() {}
    public void onPlayRequested() {}
    public void onSessionFinished() {}
    public void onDurationChanged(long duration) {}

    public EventListener.Factory getOkListenerFactory() {return null;}
    public AnalyticsListener getExoAnalyticsListener() {return null;}

    public void onApplicationPaused() {}
    public void onApplicationResumed() {}
}
