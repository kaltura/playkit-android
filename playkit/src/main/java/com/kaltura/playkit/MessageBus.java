/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kaltura.playkit.player.AudioTrack;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.player.TextTrack;
import com.kaltura.playkit.player.VideoTrack;
import com.kaltura.playkit.player.metadata.PKMetadata;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Noam Tamim @ Kaltura on 07/11/2016.
 */
@SuppressWarnings("WeakerAccess")
public class MessageBus implements Post.Target {
    private static final String TAG = "MessageBus";

    private Handler postHandler = new Handler(Looper.getMainLooper());
    private Map<Object, Set<Object>> listeners;

    enum ListenerType {
        player, ads
    }

    public MessageBus() {
        listeners = new ConcurrentHashMap<>();

        // Pre-allocate the sets for player and ads listeners.
        listeners.put(ListenerType.player, new HashSet<>(10));
        listeners.put(ListenerType.ads, new HashSet<>(10));

        // The player only sends the new style events, forward them to users of the old events.
        listeners.get(ListenerType.player).add(legacyPlayerListenerProxy);
        listeners.get(ListenerType.ads).add(legacyAdsListenerProxy);
    }

    public void postPlayerEvent(Post<PlayerListener> post) {
        final Set<Object> listeners = this.listeners.get(ListenerType.player);
        post(() -> {
            for (Object listener : listeners) {
                if (listener instanceof PlayerListener) {
                    post.run(((PlayerListener) listener));
                }
            }
        });
    }

    public void postAdsEvent(Post<AdsListener> post) {
        final Set<Object> listeners = this.listeners.get(ListenerType.player);
        post(() -> {
            for (Object listener : listeners) {
                if (listener instanceof AdsListener) {
                    post.run(((AdsListener) listener));
                }
            }
        });
    }

    void post(Runnable runnable) {
        postHandler.post(runnable);
    }

    public void post(final PKEvent event) {

        if (event instanceof PlayerEvent || event instanceof AdEvent) {
            Log.d(TAG, "LEGACY POSTING EVENT " + event.eventType());
        }

        postInternal(event);
    }

    private void postFromProxy(PKEvent event) {

        Log.d(TAG, "PROXY POSTING EVENT " + event.eventType());

        postInternal(event);
    }

    private void postInternal(PKEvent event) {
        final Set<Object> listeners = this.listeners.get(event.eventType());

        if (listeners != null) {
            postHandler.post(() -> {
                for (Object listener : new HashSet<>(listeners)) {
                    if (listener instanceof PKEvent.Listener) {
                        ((PKEvent.Listener) listener).onEvent(event);
                    }
                }
            });
        }
    }

    public void remove(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            Set<Object> listenerSet = listeners.get(eventType);
            if (listenerSet != null) {
                listenerSet.remove(listener);
            }
        }
    }

    public void removeListener(PKEvent.Listener listener) {
        for (Set<Object> listenerSet : listeners.values()) {
            Iterator<Object> iterator = listenerSet.iterator();
            while (iterator.hasNext()) {
                Object element = iterator.next();
                if (element == listener) {
                    iterator.remove();
                }
            }
        }
    }

    public PKEvent.Listener listen(PKEvent.Listener listener, Enum... eventTypes) {
        for (Enum eventType : eventTypes) {
            Set<Object> listenerSet = listeners.get(eventType);
            if (listenerSet == null) {
                listenerSet = new HashSet<>();
                listenerSet.add(listener);
                listeners.put(eventType, listenerSet);
            } else {
                listenerSet.add(listener);
            }
        }
        return listener;
    }

    public void listen(PlayerListener listener) {
        listeners.get(ListenerType.player).add(listener);
    }

    public void listen(AdsListener listener) {
        listeners.get(ListenerType.ads).add(listener);
    }



    @SuppressWarnings({"deprecation", "FieldCanBeLocal"})
    private PlayerListener legacyPlayerListenerProxy = new PlayerListener() {
        @Override
        public void onError(PKError error) {
            postFromProxy(new PlayerEvent.Error(error));
        }

        @Override
        public void onPlayerStateChanged(PlayerState newState, PlayerState oldState) {
            postFromProxy(new PlayerEvent.StateChanged(newState, oldState));
        }

        @Override
        public void onDurationChanged(long duration) {
            postFromProxy(new PlayerEvent.DurationChanged(duration));
        }

        @Override
        public void onTracksAvailable(PKTracks tracksInfo) {
            postFromProxy(new PlayerEvent.TracksAvailable(tracksInfo));
        }

        @Override
        public void onVolumeChanged(float volume) {
            postFromProxy(new PlayerEvent.VolumeChanged(volume));
        }

        @Override
        public void onPlaybackInfoUpdated(PlaybackInfo playbackInfo) {
            postFromProxy(new PlayerEvent.PlaybackInfoUpdated(playbackInfo));
        }

        @Override
        public void onMetadataAvailable(List<PKMetadata> metadataList) {
            postFromProxy(new PlayerEvent.MetadataAvailable(metadataList));
        }

        @Override
        public void onSourceSelected(PKMediaSource source) {
            postFromProxy(new PlayerEvent.SourceSelected(source));
        }

        @Override
        public void onPlayheadUpdated(long position, long duration) {
            postFromProxy(new PlayerEvent.PlayheadUpdated(position, duration));
        }

        @Override
        public void onSeeking(long targetPosition) {
            postFromProxy(new PlayerEvent.Seeking(targetPosition));
        }

        @Override
        public void onVideoTrackChanged(VideoTrack newTrack) {
            postFromProxy(new PlayerEvent.VideoTrackChanged(newTrack));
        }

        @Override
        public void onAudioTrackChanged(AudioTrack newTrack) {
            postFromProxy(new PlayerEvent.AudioTrackChanged(newTrack));
        }

        @Override
        public void onTextTrackChanged(TextTrack newTrack) {
            postFromProxy(new PlayerEvent.TextTrackChanged(newTrack));
        }

        @Override
        public void onPlaybackRateChanged(float rate) {
            postFromProxy(new PlayerEvent.PlaybackRateChanged(rate));
        }

        @Override
        public void onSubtitlesStyleChanged(String styleName) {
            postFromProxy(new PlayerEvent.SubtitlesStyleChanged(styleName));
        }

        @Override
        public void onCanPlay() {
            postFromProxy(new PlayerEvent.Generic(PlayerEvent.Type.CAN_PLAY));
        }

        @Override
        public void onEnded() {
            postFromProxy(new PlayerEvent.Generic(PlayerEvent.Type.ENDED));
        }

        @Override
        public void onPause() {
            postFromProxy(new PlayerEvent.Generic(PlayerEvent.Type.PAUSE));
        }

        @Override
        public void onPlay() {
            postFromProxy(new PlayerEvent.Generic(PlayerEvent.Type.PLAY));
        }

        @Override
        public void onPlaying() {
            postFromProxy(new PlayerEvent.Generic(PlayerEvent.Type.PLAYING));
        }

        @Override
        public void onSeeked() {
            postFromProxy(new PlayerEvent.Generic(PlayerEvent.Type.SEEKED));
        }

        @Override
        public void onReplay() {
            postFromProxy(new PlayerEvent.Generic(PlayerEvent.Type.REPLAY));
        }

        @Override
        public void onStopped() {
            postFromProxy(new PlayerEvent.Generic(PlayerEvent.Type.STOPPED));
        }
    };

    @SuppressWarnings({"deprecation", "FieldCanBeLocal"})
    private AdsListener legacyAdsListenerProxy = new AdsListener() {
        @Override
        public void onAdFirstPlay() {
            post(new AdEvent(AdEvent.Type.AD_FIRST_PLAY));
        }

        @Override
        public void onAdDisplayedAfterContentPause() {
            post(new AdEvent(AdEvent.Type.AD_DISPLAYED_AFTER_CONTENT_PAUSE));
        }

        @Override
        public void onCompleted() {
            post(new AdEvent(AdEvent.Type.COMPLETED));
        }

        @Override
        public void onFirstQuartile() {
            post(new AdEvent(AdEvent.Type.FIRST_QUARTILE));
        }

        @Override
        public void onMidpoint() {
            post(new AdEvent(AdEvent.Type.MIDPOINT));
        }

        @Override
        public void onThirdQuartile() {
            post(new AdEvent(AdEvent.Type.THIRD_QUARTILE));
        }

        @Override
        public void onClicked() {
            post(new AdEvent(AdEvent.Type.CLICKED));
        }

        @Override
        public void onTapped() {
            post(new AdEvent(AdEvent.Type.TAPPED));
        }

        @Override
        public void onIconTapped() {
            post(new AdEvent(AdEvent.Type.ICON_TAPPED));
        }

        @Override
        public void onAdBreakReady() {
            post(new AdEvent(AdEvent.Type.AD_BREAK_READY));
        }

        @Override
        public void onAdProgress() {
            post(new AdEvent(AdEvent.Type.AD_PROGRESS));
        }

        @Override
        public void onAdBreakStarted() {
            post(new AdEvent(AdEvent.Type.AD_BREAK_STARTED));
        }

        @Override
        public void onAdBreakEnded() {
            post(new AdEvent(AdEvent.Type.AD_BREAK_ENDED));
        }

        @Override
        public void onAdBreakIgnored() {
            post(new AdEvent(AdEvent.Type.AD_BREAK_IGNORED));
        }

        @Override
        public void onContentPauseRequested() {
            post(new AdEvent(AdEvent.Type.CONTENT_PAUSE_REQUESTED));
        }

        @Override
        public void onContentResumeRequested() {
            post(new AdEvent(AdEvent.Type.CONTENT_RESUME_REQUESTED));
        }

        @Override
        public void onAllAdsCompleted() {
            post(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
        }

        @Override
        public void onAdLoadTimeoutTimerStarted() {
            post(new AdEvent(AdEvent.Type.AD_LOAD_TIMEOUT_TIMER_STARTED));
        }

        @Override
        public void onAdLoaded(AdInfo adInfo) {
            post(new AdEvent.AdLoadedEvent(adInfo));
        }

        @Override
        public void onAdStarted(AdInfo adInfo) {
            post(new AdEvent.AdStartedEvent(adInfo));
        }

        @Override
        public void onAdPaused(AdInfo adInfo) {
            post(new AdEvent.AdPausedEvent(adInfo));
        }

        @Override
        public void onAdResumed(AdInfo adInfo) {
            post(new AdEvent.AdResumedEvent(adInfo));
        }

        @Override
        public void onAdSkipped(AdInfo adInfo) {
            post(new AdEvent.AdSkippedEvent(adInfo));
        }

        @Override
        public void onAdCuePointsUpdate(AdCuePoints cuePoints) {
            post(new AdEvent.AdCuePointsUpdateEvent(cuePoints));
        }

        @Override
        public void onAdPlayHead(long adPlayHead) {
            post(new AdEvent.AdPlayHeadEvent(adPlayHead));
        }

        @Override
        public void onAdRequested(String adTagUrl) {
            post(new AdEvent.AdRequestedEvent(adTagUrl));
        }

        @Override
        public void onAdBufferStart(long adPosition) {
            post(new AdEvent.AdBufferStart(adPosition));
        }

        @Override
        public void onAdBufferEnd(long adPosition) {
            post(new AdEvent.AdBufferEnd(adPosition));
        }

        @Override
        public void onError(PKError error) {
            post(new AdEvent.Error(error));
        }
    };

}
