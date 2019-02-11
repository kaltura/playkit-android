package com.kaltura.playkit.player;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.kaltura.playkit.PKMediaConfig;

import java.util.Map;

import okhttp3.EventListener;

public interface Profiler {
    default void setPlayerEngine(PlayerEngine playerEngine) {}

    default void newSession(String sessionId, PlayerSettings playerSettings) {}
    default void onSetMedia(PKMediaConfig mediaConfig) {}
    default void onPrepareStarted(PKMediaSourceConfig sourceConfig) {}
    default void onSeekRequested(long position) {}
    default void onPauseRequested() {}
    default void onReplayRequested() {}
    default void onPlayRequested() {}
    default void onSessionFinished() {}
    default void onDurationChanged(long duration) {}

    default EventListener.Factory getOkListenerFactory() {return null;}
    default AnalyticsListener getExoAnalyticsListener() {return null;}
}
