package com.kaltura.playkit.player;

import androidx.annotation.NonNull;

import com.kaltura.androidx.media3.exoplayer.analytics.AnalyticsListener;
import com.kaltura.playkit.PKMediaConfig;

import okhttp3.EventListener;

public abstract class Profiler {
    // A no-op profiler to avoid null checks.
    @NonNull static final Profiler NOOP = new Profiler() {};

    public void setPlayerEngine(PlayerEngine playerEngine) {/*NOOP*/}

    public void newSession(String sessionId, PlayerSettings playerSettings) {/*NOOP*/}
    public void onSetMedia(PKMediaConfig mediaConfig) {/*NOOP*/}
    public void onPrepareStarted(PKMediaSourceConfig sourceConfig) {/*NOOP*/}
    public void onSeekRequested(long position) {/*NOOP*/}
    public void onPauseRequested() {/*NOOP*/}
    public void onReplayRequested() {/*NOOP*/}
    public void onPlayRequested() {/*NOOP*/}
    public void onSessionFinished() {/*NOOP*/}
    public void onDurationChanged(long duration) {/*NOOP*/}

    public EventListener.Factory getOkListenerFactory() {return null;}
    public AnalyticsListener getExoAnalyticsListener() {return null;}

    public void onApplicationPaused() {/*NOOP*/}
    public void onApplicationResumed() {/*NOOP*/}
}
