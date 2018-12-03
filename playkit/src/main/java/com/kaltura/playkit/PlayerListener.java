package com.kaltura.playkit;

import com.kaltura.playkit.player.AudioTrack;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.player.TextTrack;
import com.kaltura.playkit.player.VideoTrack;
import com.kaltura.playkit.player.metadata.PKMetadata;

import java.util.List;

@SuppressWarnings("unused")
public interface PlayerListener {

    default void onError(PKError error) {}

    default void onPlayerStateChanged(PlayerState newState, PlayerState oldState) {}

    default void onDurationChanged(long duration) {}

    default void onTracksAvailable(PKTracks tracksInfo) {}

    default void onVolumeChanged(float volume) {}

    default void onPlaybackInfoUpdated(PlaybackInfo playbackInfo) {}

    default void onMetadataAvailable(List<PKMetadata> metadataList) {}

    default void onSourceSelected(PKMediaSource source) {}

    default void onPlayheadUpdated(long position, long duration) {}

    default void onSeeking(long targetPosition) {}

    default void onVideoTrackChanged(VideoTrack newTrack) {}

    default void onAudioTrackChanged(AudioTrack newTrack) {}

    default void onTextTrackChanged(TextTrack newTrack) {}

    default void onPlaybackRateChanged(float rate) {}

    default void onSubtitlesStyleChanged(String styleName) {}

    default void onCanPlay() {}

    default void onEnded() {}

    default void onPause() {}

    default void onPlay() {}

    default void onPlaying() {}

    default void onSeeked() {}

    default void onReplay() {}

    default void onStopped() {}

    default void onLoadedMetadata() {}
}

