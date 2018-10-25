package com.kaltura.playkit.player;

import com.kaltura.playkit.PKMediaConfig;

interface Profiler {
    void newSession(String sessionId);

    void startListener(ExoPlayerWrapper playerEngine);

    void stopListener(ExoPlayerWrapper playerEngine);

    void onSetMedia(PlayerController playerController, PKMediaConfig mediaConfig);

    void onPrepareStarted(PlayerEngine playerEngine, PKMediaSourceConfig sourceConfig);

    void onSeekRequested(PlayerEngine playerEngine, long position);

    void onPauseRequested(PlayerEngine playerEngine);

    void onReplayRequested(PlayerEngine playerEngine);

    void onPlayRequested(PlayerEngine playerEngine);

    void onBandwidthSample(PlayerEngine playerEngine, long bitrate);

    void onSessionFinished();

    void onViewportSizeChange(PlayerEngine playerEngine, int width, int height);

    void onDurationChanged(long duration);
}
