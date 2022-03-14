package com.kaltura.playkit;

import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.playkit.player.ABRSettings;
import com.kaltura.playkit.player.BaseTrack;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.PKLowLatencyConfig;
import com.kaltura.playkit.player.PKMediaSourceConfig;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.player.PlayerEngine;
import com.kaltura.playkit.player.PlayerView;
import com.kaltura.playkit.player.Profiler;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.playkit.player.metadata.PKMetadata;
import com.kaltura.playkit.player.thumbnail.ThumbnailInfo;

import java.util.List;

public class PlayerEngineWrapper implements PlayerEngine {

    protected PlayerEngine playerEngine;

    @Override
    public void load(PKMediaSourceConfig mediaSourceConfig) {
        playerEngine.load(mediaSourceConfig);
    }

    @Override
    public PlayerView getView() {
        return playerEngine.getView();
    }

    @Override
    public void play() {
        playerEngine.play();
    }

    @Override
    public void pause() {
        playerEngine.pause();
    }

    @Override
    public void replay() {
        playerEngine.replay();
    }

    @Override
    public long getCurrentPosition() {
        return playerEngine.getCurrentPosition();
    }

    @Override
    public long getPositionInWindowMs() {
        return playerEngine.getPositionInWindowMs();
    }

    @Override
    public long getProgramStartTime() {
        return playerEngine.getProgramStartTime();
    }

    @Override
    public long getDuration() {
        return playerEngine.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        return playerEngine.getBufferedPosition();
    }

    @Override
    public long getCurrentLiveOffset() {
        return playerEngine.getCurrentLiveOffset();
    }

    @Override
    public float getVolume() {
        return playerEngine.getVolume();
    }

    @Override
    public PKTracks getPKTracks() {
        return playerEngine.getPKTracks();
    }

    @Override
    public void changeTrack(String uniqueId) {
        playerEngine.changeTrack(uniqueId);
    }

    @Override
    public void overrideMediaVideoCodec() {
        playerEngine.overrideMediaVideoCodec();
    }

    @Override
    public void overrideMediaDefaultABR(long minVideoBitrate, long maxVideoBitrate, PKAbrFilter pkAbrFilter) {
        playerEngine.overrideMediaDefaultABR(minVideoBitrate, maxVideoBitrate, pkAbrFilter);
    }

    @Override
    public void seekTo(long position) {
        playerEngine.seekTo(position);
    }

    @Override
    public void seekToDefaultPosition() {
        playerEngine.seekToDefaultPosition();
    }
    
    @Override
    public void startFrom(long position) {
        playerEngine.startFrom(position);
    }

    @Override
    public void setVolume(float volume) {
        playerEngine.setVolume(volume);
    }

    @Override
    public boolean isPlaying() {
        return playerEngine.isPlaying();
    }

    @Override
    public void setEventListener(EventListener eventTrigger) {
        playerEngine.setEventListener(eventTrigger);
    }

    @Override
    public void setStateChangedListener(StateChangedListener stateChangedTrigger) {
        playerEngine.setStateChangedListener(stateChangedTrigger);
    }

    @Override
    public void setAnalyticsListener(AnalyticsListener analyticsListener) {
        playerEngine.setAnalyticsListener(analyticsListener);
    }

    @Override
    public void setInputFormatChangedListener(Boolean enableListener) {
        playerEngine.setInputFormatChangedListener(enableListener);
    }

    @Override
    public void release() {
        playerEngine.release();
    }

    @Override
    public void restore() {
        playerEngine.restore();
    }

    @Override
    public void destroy() {
        playerEngine.destroy();
    }

    @Override
    public PlaybackInfo getPlaybackInfo() {
        return playerEngine.getPlaybackInfo();
    }

    @Override
    public PKError getCurrentError() {
        return playerEngine.getCurrentError();
    }

    @Override
    public void stop() {
        playerEngine.stop();
    }

    @Override
    public List<PKMetadata> getMetadata() {
        return playerEngine.getMetadata();
    }

    @Override
    public BaseTrack getLastSelectedTrack(int renderType) {
        return playerEngine.getLastSelectedTrack(renderType);
    }

    @Override
    public boolean isLive() {
        return playerEngine.isLive();
    }

    @Override
    public void setPlaybackRate(float rate) {
        playerEngine.setPlaybackRate(rate);
    }

    @Override
    public float getPlaybackRate() {
        return playerEngine.getPlaybackRate();
    }

    @Override
    public void setDownloadCache(Cache downloadCache) {
        playerEngine.setDownloadCache(downloadCache);
    }
    
    @Override
    public ThumbnailInfo getThumbnailInfo(long positionMS) {
        return playerEngine.getThumbnailInfo(positionMS);
    }

    @Override
    public void setProfiler(Profiler profiler) {
        this.playerEngine.setProfiler(profiler);
    }

    @Override
    public void updateSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings) {
        playerEngine.updateSubtitleStyle(subtitleStyleSettings);
    }

    @Override
    public void updateABRSettings(ABRSettings abrSettings) {
        playerEngine.updateABRSettings(abrSettings);
    }

    @Override
    public void resetABRSettings() {
        playerEngine.resetABRSettings();
    }

    @Override
    public void updateSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        playerEngine.updateSurfaceAspectRatioResizeMode(resizeMode);
    }

    @Override
    public void updatePKLowLatencyConfig(PKLowLatencyConfig pkLowLatencyConfig) {
        playerEngine.updatePKLowLatencyConfig(pkLowLatencyConfig);
    }

    @Override
    public <T extends PKController> T getController(Class<T> type) {
        return playerEngine.getController(type);
    }

    @Override
    public void onOrientationChanged() {
        playerEngine.onOrientationChanged();
    }

    public void setPlayerEngine(PlayerEngine playerEngine) {
        this.playerEngine = playerEngine;
    }

    public PlayerEngine getPlayerEngine() {
        return this.playerEngine;
    }
}
