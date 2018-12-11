package com.kaltura.playkit;

import com.kaltura.playkit.player.AudioTrack;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.player.TextTrack;
import com.kaltura.playkit.player.VideoTrack;
import com.kaltura.playkit.player.metadata.PKMetadata;

import java.util.List;

@SuppressWarnings("deprecation")
class PlayerListenerAdapter implements PlayerListener {
    private final MessageBus messageBus;

    PlayerListenerAdapter(DefaultMessageBus messageBus) {
        this.messageBus = messageBus;
    }

    @Override
    public void onError(PKError error) {
        messageBus.post(new PlayerEvent.Error(error));
    }

    @Override
    public void onPlayerStateChanged(PlayerState newState, PlayerState oldState) {
        messageBus.post(new PlayerEvent.StateChanged(newState, oldState));
    }

    @Override
    public void onDurationChanged(long duration) {
        messageBus.post(new PlayerEvent.DurationChanged(duration));
    }

    @Override
    public void onTracksAvailable(PKTracks tracksInfo) {
        messageBus.post(new PlayerEvent.TracksAvailable(tracksInfo));
    }

    @Override
    public void onVolumeChanged(float volume) {
        messageBus.post(new PlayerEvent.VolumeChanged(volume));
    }

    @Override
    public void onPlaybackInfoUpdated(PlaybackInfo playbackInfo) {
        messageBus.post(new PlayerEvent.PlaybackInfoUpdated(playbackInfo));
    }

    @Override
    public void onMetadataAvailable(List<PKMetadata> metadataList) {
        messageBus.post(new PlayerEvent.MetadataAvailable(metadataList));
    }

    @Override
    public void onSourceSelected(PKMediaSource source) {
        messageBus.post(new PlayerEvent.SourceSelected(source));
    }

    @Override
    public void onPlayheadUpdated(long position, long duration) {
        messageBus.post(new PlayerEvent.PlayheadUpdated(position, duration));
    }

    @Override
    public void onSeeking(long targetPosition) {
        messageBus.post(new PlayerEvent.Seeking(targetPosition));
    }

    @Override
    public void onVideoTrackChanged(VideoTrack newTrack) {
        messageBus.post(new PlayerEvent.VideoTrackChanged(newTrack));
    }

    @Override
    public void onAudioTrackChanged(AudioTrack newTrack) {
        messageBus.post(new PlayerEvent.AudioTrackChanged(newTrack));
    }

    @Override
    public void onTextTrackChanged(TextTrack newTrack) {
        messageBus.post(new PlayerEvent.TextTrackChanged(newTrack));
    }

    @Override
    public void onPlaybackRateChanged(float rate) {
        messageBus.post(new PlayerEvent.PlaybackRateChanged(rate));
    }

    @Override
    public void onSubtitlesStyleChanged(String styleName) {
        messageBus.post(new PlayerEvent.SubtitlesStyleChanged(styleName));
    }

    @Override
    public void onCanPlay() {
        messageBus.post(new PlayerEvent.Generic(PlayerEvent.Type.CAN_PLAY));
    }

    @Override
    public void onEnded() {
        messageBus.post(new PlayerEvent.Generic(PlayerEvent.Type.ENDED));
    }

    @Override
    public void onPause() {
        messageBus.post(new PlayerEvent.Generic(PlayerEvent.Type.PAUSE));
    }

    @Override
    public void onPlay() {
        messageBus.post(new PlayerEvent.Generic(PlayerEvent.Type.PLAY));
    }

    @Override
    public void onPlaying() {
        messageBus.post(new PlayerEvent.Generic(PlayerEvent.Type.PLAYING));
    }

    @Override
    public void onSeeked() {
        messageBus.post(new PlayerEvent.Generic(PlayerEvent.Type.SEEKED));
    }

    @Override
    public void onReplay() {
        messageBus.post(new PlayerEvent.Generic(PlayerEvent.Type.REPLAY));
    }

    @Override
    public void onStopping(long stopPosition) {
        messageBus.post(new PlayerEvent.Generic(PlayerEvent.Type.STOPPED));
    }

    @Override
    public void onLoadedMetadata() {
        messageBus.post(new PlayerEvent.Generic(PlayerEvent.Type.LOADED_METADATA));
    }
}
