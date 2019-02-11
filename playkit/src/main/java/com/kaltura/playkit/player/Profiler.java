package com.kaltura.playkit.player;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.playkit.PKMediaConfig;

import java.util.Map;

import okhttp3.EventListener;

public abstract class Profiler {
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
}
