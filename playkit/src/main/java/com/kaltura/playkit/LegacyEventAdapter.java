package com.kaltura.playkit;

import com.kaltura.playkit.player.AudioTrack;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.player.TextTrack;
import com.kaltura.playkit.player.VideoTrack;
import com.kaltura.playkit.player.metadata.PKMetadata;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;

import java.util.List;

class LegacyEventAdapter {

    private final DefaultMessageBus messageBus;

    final PKEvent.Listener legacyToNewAdsEvents = event -> {
        final AdEvent e = (AdEvent) event;
        switch (e.type) {

            case AD_REQUESTED:
                postAdsEvent(L -> L.onAdRequested(((AdEvent.AdRequestedEvent) e).adTagUrl));
                break;
            case AD_FIRST_PLAY:
                postAdsEvent(AdsListener::onAdFirstPlay);
                break;
            case STARTED:
                postAdsEvent(L -> L.onAdStarted(((AdEvent.AdStartedEvent) e).adInfo));
                break;
            case AD_DISPLAYED_AFTER_CONTENT_PAUSE:
                postAdsEvent(AdsListener::onAdDisplayedAfterContentPause);
                break;
            case PAUSED:
                postAdsEvent(L -> L.onAdPaused(((AdEvent.AdPausedEvent) e).adInfo));
                break;
            case RESUMED:
                postAdsEvent(L -> L.onAdResumed(((AdEvent.AdResumedEvent) e).adInfo));
                break;
            case COMPLETED:
                postAdsEvent(AdsListener::onCompleted);
                break;
            case FIRST_QUARTILE:
                postAdsEvent(AdsListener::onFirstQuartile);
                break;
            case MIDPOINT:
                postAdsEvent(AdsListener::onMidpoint);
                break;
            case THIRD_QUARTILE:
                postAdsEvent(AdsListener::onThirdQuartile);
                break;
            case SKIPPED:
                postAdsEvent(L -> L.onAdSkipped(((AdEvent.AdSkippedEvent) e).adInfo));
                break;
            case CLICKED:
                postAdsEvent(AdsListener::onClicked);
                break;
            case TAPPED:
                postAdsEvent(AdsListener::onTapped);
                break;
            case ICON_TAPPED:
                postAdsEvent(AdsListener::onIconTapped);
                break;
            case AD_BREAK_READY:
                postAdsEvent(AdsListener::onIconTapped);
                break;
            case AD_PROGRESS:
                postAdsEvent(AdsListener::onAdProgress);
                break;
            case AD_BREAK_STARTED:
                postAdsEvent(AdsListener::onAdBreakStarted);
                break;
            case AD_BREAK_ENDED:
                postAdsEvent(AdsListener::onAdBreakEnded);
                break;
            case AD_BREAK_IGNORED:
                postAdsEvent(AdsListener::onAdBreakIgnored);
                break;
            case CUEPOINTS_CHANGED:
                postAdsEvent(L -> L.onAdCuePointsUpdate(((AdEvent.AdCuePointsUpdateEvent) e).cuePoints));
                break;
            case PLAY_HEAD_CHANGED:
                postAdsEvent(L -> L.onAdPlayHead(((AdEvent.AdPlayHeadEvent) e).adPlayHead));
                break;
            case LOADED:
                postAdsEvent(L -> L.onAdLoaded(((AdEvent.AdLoadedEvent) e).adInfo));
                break;
            case CONTENT_PAUSE_REQUESTED:
                postAdsEvent(AdsListener::onContentPauseRequested);
                break;
            case CONTENT_RESUME_REQUESTED:
                postAdsEvent(AdsListener::onContentResumeRequested);
                break;
            case ALL_ADS_COMPLETED:
                postAdsEvent(AdsListener::onAllAdsCompleted);
                break;
            case AD_LOAD_TIMEOUT_TIMER_STARTED:
                postAdsEvent(AdsListener::onAdLoadTimeoutTimerStarted);
                break;
            case AD_BUFFER_START:
                postAdsEvent(L -> L.onAdBufferStart(((AdEvent.AdBufferStart) e).adPosition));
                break;
            case AD_BUFFER_END:
                postAdsEvent(L -> L.onAdBufferEnd(((AdEvent.AdBufferEnd) e).adPosition));
                break;
            case ERROR:
                postAdsEvent(L -> L.onError(((AdEvent.Error) e).error));
                break;
        }
    };

    @SuppressWarnings({"deprecation", "FieldCanBeLocal"})
    final PlayerListener newToLegacyPlayerEvents = new PlayerListener() {
        @Override
        public void onError(PKError error) {
            messageBus.postFromAdapter(new PlayerEvent.Error(error));
        }

        @Override
        public void onPlayerStateChanged(PlayerState newState, PlayerState oldState) {
            messageBus.postFromAdapter(new PlayerEvent.StateChanged(newState, oldState));
        }

        @Override
        public void onDurationChanged(long duration) {
            messageBus.postFromAdapter(new PlayerEvent.DurationChanged(duration));
        }

        @Override
        public void onTracksAvailable(PKTracks tracksInfo) {
            messageBus.postFromAdapter(new PlayerEvent.TracksAvailable(tracksInfo));
        }

        @Override
        public void onVolumeChanged(float volume) {
            messageBus.postFromAdapter(new PlayerEvent.VolumeChanged(volume));
        }

        @Override
        public void onPlaybackInfoUpdated(PlaybackInfo playbackInfo) {
            messageBus.postFromAdapter(new PlayerEvent.PlaybackInfoUpdated(playbackInfo));
        }

        @Override
        public void onMetadataAvailable(List<PKMetadata> metadataList) {
            messageBus.postFromAdapter(new PlayerEvent.MetadataAvailable(metadataList));
        }

        @Override
        public void onSourceSelected(PKMediaSource source) {
            messageBus.postFromAdapter(new PlayerEvent.SourceSelected(source));
        }

        @Override
        public void onPlayheadUpdated(long position, long duration) {
            messageBus.postFromAdapter(new PlayerEvent.PlayheadUpdated(position, duration));
        }

        @Override
        public void onSeeking(long targetPosition) {
            messageBus.postFromAdapter(new PlayerEvent.Seeking(targetPosition));
        }

        @Override
        public void onVideoTrackChanged(VideoTrack newTrack) {
            messageBus.postFromAdapter(new PlayerEvent.VideoTrackChanged(newTrack));
        }

        @Override
        public void onAudioTrackChanged(AudioTrack newTrack) {
            messageBus.postFromAdapter(new PlayerEvent.AudioTrackChanged(newTrack));
        }

        @Override
        public void onTextTrackChanged(TextTrack newTrack) {
            messageBus.postFromAdapter(new PlayerEvent.TextTrackChanged(newTrack));
        }

        @Override
        public void onPlaybackRateChanged(float rate) {
            messageBus.postFromAdapter(new PlayerEvent.PlaybackRateChanged(rate));
        }

        @Override
        public void onSubtitlesStyleChanged(String styleName) {
            messageBus.postFromAdapter(new PlayerEvent.SubtitlesStyleChanged(styleName));
        }

        @Override
        public void onCanPlay() {
            messageBus.postFromAdapter(new PlayerEvent.Generic(PlayerEvent.Type.CAN_PLAY));
        }

        @Override
        public void onEnded() {
            messageBus.postFromAdapter(new PlayerEvent.Generic(PlayerEvent.Type.ENDED));
        }

        @Override
        public void onPause() {
            messageBus.postFromAdapter(new PlayerEvent.Generic(PlayerEvent.Type.PAUSE));
        }

        @Override
        public void onPlay() {
            messageBus.postFromAdapter(new PlayerEvent.Generic(PlayerEvent.Type.PLAY));
        }

        @Override
        public void onPlaying() {
            messageBus.postFromAdapter(new PlayerEvent.Generic(PlayerEvent.Type.PLAYING));
        }

        @Override
        public void onSeeked() {
            messageBus.postFromAdapter(new PlayerEvent.Generic(PlayerEvent.Type.SEEKED));
        }

        @Override
        public void onReplay() {
            messageBus.postFromAdapter(new PlayerEvent.Generic(PlayerEvent.Type.REPLAY));
        }

        @Override
        public void onStopping(long stopPosition) {
            messageBus.postFromAdapter(new PlayerEvent.Generic(PlayerEvent.Type.STOPPED));
        }
    };

    @SuppressWarnings({"deprecation", "FieldCanBeLocal"})
    final AdsListener newToLegacyAdsEvents = new AdsListener() {
        @Override
        public void onAdFirstPlay() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.AD_FIRST_PLAY));
        }

        @Override
        public void onAdDisplayedAfterContentPause() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.AD_DISPLAYED_AFTER_CONTENT_PAUSE));
        }

        @Override
        public void onCompleted() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.COMPLETED));
        }

        @Override
        public void onFirstQuartile() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.FIRST_QUARTILE));
        }

        @Override
        public void onMidpoint() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.MIDPOINT));
        }

        @Override
        public void onThirdQuartile() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.THIRD_QUARTILE));
        }

        @Override
        public void onClicked() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.CLICKED));
        }

        @Override
        public void onTapped() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.TAPPED));
        }

        @Override
        public void onIconTapped() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.ICON_TAPPED));
        }

        @Override
        public void onAdBreakReady() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.AD_BREAK_READY));
        }

        @Override
        public void onAdProgress() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.AD_PROGRESS));
        }

        @Override
        public void onAdBreakStarted() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.AD_BREAK_STARTED));
        }

        @Override
        public void onAdBreakEnded() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.AD_BREAK_ENDED));
        }

        @Override
        public void onAdBreakIgnored() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.AD_BREAK_IGNORED));
        }

        @Override
        public void onContentPauseRequested() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.CONTENT_PAUSE_REQUESTED));
        }

        @Override
        public void onContentResumeRequested() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.CONTENT_RESUME_REQUESTED));
        }

        @Override
        public void onAllAdsCompleted() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
        }

        @Override
        public void onAdLoadTimeoutTimerStarted() {
            messageBus.postFromAdapter(new AdEvent(AdEvent.Type.AD_LOAD_TIMEOUT_TIMER_STARTED));
        }

        @Override
        public void onAdLoaded(AdInfo adInfo) {
            messageBus.postFromAdapter(new AdEvent.AdLoadedEvent(adInfo));
        }

        @Override
        public void onAdStarted(AdInfo adInfo) {
            messageBus.postFromAdapter(new AdEvent.AdStartedEvent(adInfo));
        }

        @Override
        public void onAdPaused(AdInfo adInfo) {
            messageBus.postFromAdapter(new AdEvent.AdPausedEvent(adInfo));
        }

        @Override
        public void onAdResumed(AdInfo adInfo) {
            messageBus.postFromAdapter(new AdEvent.AdResumedEvent(adInfo));
        }

        @Override
        public void onAdSkipped(AdInfo adInfo) {
            messageBus.postFromAdapter(new AdEvent.AdSkippedEvent(adInfo));
        }

        @Override
        public void onAdCuePointsUpdate(AdCuePoints cuePoints) {
            messageBus.postFromAdapter(new AdEvent.AdCuePointsUpdateEvent(cuePoints));
        }

        @Override
        public void onAdPlayHead(long adPlayHead) {
            messageBus.postFromAdapter(new AdEvent.AdPlayHeadEvent(adPlayHead));
        }

        @Override
        public void onAdRequested(String adTagUrl) {
            messageBus.postFromAdapter(new AdEvent.AdRequestedEvent(adTagUrl));
        }

        @Override
        public void onAdBufferStart(long adPosition) {
            messageBus.postFromAdapter(new AdEvent.AdBufferStart(adPosition));
        }

        @Override
        public void onAdBufferEnd(long adPosition) {
            messageBus.postFromAdapter(new AdEvent.AdBufferEnd(adPosition));
        }

        @Override
        public void onError(PKError error) {
            messageBus.postFromAdapter(new AdEvent.Error(error));
        }
    };

    LegacyEventAdapter(DefaultMessageBus messageBus) {
        this.messageBus = messageBus;
    }

    private void postAdsEvent(MessageBus.Post<AdsListener> post) {
        messageBus.postAdsEvent(new AdsPost(post));
    }

    // Proxy to prevent circular forwarding between adapters
    class AdsPost implements MessageBus.Post<AdsListener> {

        final MessageBus.Post<AdsListener> realPost;

        AdsPost(MessageBus.Post<AdsListener> realPost) {
            this.realPost = realPost;
        }

        @Override
        public void run(AdsListener L) {
            if (L != newToLegacyAdsEvents) {
                realPost.run(L);
            }
        }
    }

}
