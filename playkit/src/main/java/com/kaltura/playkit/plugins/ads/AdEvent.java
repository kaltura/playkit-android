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

package com.kaltura.playkit.plugins.ads;

import androidx.annotation.Nullable;

import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.ads.AdBreakConfig;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;

@SuppressWarnings("unused")
public class AdEvent implements PKEvent {

    public static final Class<AdLoadedEvent> loaded = AdLoadedEvent.class;
    public static final Class<AdStartedEvent> started = AdStartedEvent.class;
    public static final Class<AdPausedEvent> paused = AdPausedEvent.class;
    public static final Class<AdResumedEvent> resumed = AdResumedEvent.class;
    public static final Class<AdSkippedEvent> skipped = AdSkippedEvent.class;
    public static final Class<AdCuePointsUpdateEvent> cuepointsChanged = AdCuePointsUpdateEvent.class;
    public static final Class<AdPlayHeadEvent> playHeadChanged = AdPlayHeadEvent.class;
    public static final Class<AdRequestedEvent> adRequested = AdRequestedEvent.class;
    public static final Class<AdBufferStart> adBufferStart = AdBufferStart.class;
    public static final Class<AdBufferEnd> adBufferEnd = AdBufferEnd.class;
    public static final Class<AdProgress> adProgress = AdProgress.class;
    public static final Class<AdPlaybackInfoUpdated> adPlaybackInfoUpdated = AdPlaybackInfoUpdated.class;
    public static final Class<AdClickedEvent> adClickedEvent = AdClickedEvent.class;
    public static final Class<Error> error = Error.class;
    public static final Class<DAISourceSelected> daiSourceSelected = DAISourceSelected.class;
    public static final Class<AdWaterFalling> adWaterFalling = AdWaterFalling.class;
    public static final Class<AdWaterFallingFailed> adWaterFallingFailed = AdWaterFallingFailed.class;
    public static final Class<AdSurfaceAspectRatioResizeModeChanged> adSurfaceAspectRationSizeModeChanged = AdSurfaceAspectRatioResizeModeChanged.class;

    public static final AdEvent.Type adFirstPlay = Type.AD_FIRST_PLAY;
    public static final AdEvent.Type adDisplayedAfterContentPause = Type.AD_DISPLAYED_AFTER_CONTENT_PAUSE;
    public static final AdEvent.Type completed = Type.COMPLETED;
    public static final AdEvent.Type firstQuartile = Type.FIRST_QUARTILE;
    public static final AdEvent.Type midpoint = Type.MIDPOINT;
    public static final AdEvent.Type thirdQuartile = Type.THIRD_QUARTILE;
    public static final AdEvent.Type skippableStateChanged = Type.SKIPPABLE_STATE_CHANGED;
    public static final AdEvent.Type tapped = Type.TAPPED;
    public static final AdEvent.Type iconFallbackImageClosed = Type.ICON_FALLBACK_IMAGE_CLOSED;
    public static final AdEvent.Type iconTapped = Type.ICON_TAPPED;
    public static final AdEvent.Type adBreakReady = Type.AD_BREAK_READY;
    public static final AdEvent.Type adBreakStarted = Type.AD_BREAK_STARTED;
    public static final AdEvent.Type adBreakEnded = Type.AD_BREAK_ENDED;
    public static final AdEvent.Type adBreakFetchError = Type.AD_BREAK_FETCH_ERROR;
    public static final AdEvent.Type adBreakIgnored = Type.AD_BREAK_IGNORED;
    public static final AdEvent.Type contentPauseRequested = Type.CONTENT_PAUSE_REQUESTED;
    public static final AdEvent.Type contentResumeRequested = Type.CONTENT_RESUME_REQUESTED;
    public static final AdEvent.Type allAdsCompleted = Type.ALL_ADS_COMPLETED;
    public static final AdEvent.Type adLoadTimeoutTimerStarted = Type.AD_LOAD_TIMEOUT_TIMER_STARTED;


    public Type type;

    public AdEvent(Type type) {
        this.type = type;
    }

    public static class AdLoadedEvent extends AdEvent {

        public final AdInfo adInfo;

        public AdLoadedEvent(AdInfo adInfo) {
            super(Type.LOADED);
            this.adInfo = adInfo;
        }
    }

    public static class AdStartedEvent extends AdEvent {

        public final AdInfo adInfo;

        public AdStartedEvent(AdInfo adInfo) {
            super(Type.STARTED);
            this.adInfo = adInfo;
        }
    }

    public static class AdPausedEvent extends AdEvent {

        public final AdInfo adInfo;

        public AdPausedEvent(AdInfo adInfo) {
            super(Type.PAUSED);
            this.adInfo = adInfo;
        }
    }

    public static class AdResumedEvent extends AdEvent {

        public final AdInfo adInfo;

        public AdResumedEvent(AdInfo adInfo) {
            super(Type.RESUMED);
            this.adInfo = adInfo;
        }
    }

    public static class AdSkippedEvent extends AdEvent {

        public final AdInfo adInfo;

        public AdSkippedEvent(AdInfo adInfo) {
            super(Type.SKIPPED);
            this.adInfo = adInfo;
        }
    }

    public static class AdCuePointsUpdateEvent extends AdEvent {

        public final AdCuePoints cuePoints;

        public AdCuePointsUpdateEvent(AdCuePoints cuePoints) {
            super(Type.CUEPOINTS_CHANGED);
            this.cuePoints = cuePoints;
        }
    }

    public static class AdPlayHeadEvent extends AdEvent {

        public final long adPlayHead;

        public AdPlayHeadEvent(long adPlayHead) {
            super(Type.PLAY_HEAD_CHANGED);
            this.adPlayHead = adPlayHead;
        }
    }

    public static class AdRequestedEvent extends AdEvent {

        public final String adTagUrl;
        public final boolean isAutoPlay;

        public AdRequestedEvent(String adTagUrl, boolean isAutoPlay) {
            super(Type.AD_REQUESTED);
            this.adTagUrl = adTagUrl;
            this.isAutoPlay = isAutoPlay;
        }
    }

    public static class AdClickedEvent extends AdEvent {

        public final String clickThruUrl;

        public AdClickedEvent(@Nullable String clickThruUrl) {
            super(Type.CLICKED);
            this.clickThruUrl = clickThruUrl;
        }
    }

    public static class AdBufferStart extends AdEvent {

        public final long adPosition;

        public AdBufferStart(long adPosition) {
            super(Type.AD_BUFFER_START);
            this.adPosition = adPosition;
        }
    }

    public static class AdBufferEnd extends AdEvent {

        public final long adPosition;

        public AdBufferEnd(long adPosition) {
            super(Type.AD_BUFFER_END);
            this.adPosition = adPosition;
        }
    }

    public static class AdProgress extends AdEvent {

        public final long currentAdPosition;

        public AdProgress(long currentAdPosition) {
            super(Type.AD_PROGRESS);
            this.currentAdPosition = currentAdPosition;
        }
    }

    public static class AdPlaybackInfoUpdated extends AdEvent {

        public final int width;
        public final int height;
        public final int bitrate;

        public AdPlaybackInfoUpdated(int width, int height, int bitrate) {
            super(Type.AD_PLAYBACK_INFO_UPDATED);
            this.width = width;
            this.height = height;
            this.bitrate = bitrate;
        }
    }

    public static class Error extends AdEvent {

        public final PKError error;

        public Error(PKError error) {
            super(Type.ERROR);
            this.error = error;
        }
    }

    public static class DAISourceSelected extends AdEvent {

        public final String sourceURL;

        public DAISourceSelected(String sourceURL) {
            super(Type.DAI_SOURCE_SELECTED);
            this.sourceURL = sourceURL;
        }
    }

    /**
     * This event is only being sent for IMA Ads not for IMADAI Ads
     * For IMADAI ads event, listen to {@link com.kaltura.playkit.PlayerEvent#surfaceAspectRationSizeModeChanged}
     */
    public static class AdSurfaceAspectRatioResizeModeChanged extends AdEvent {

        public final PKAspectRatioResizeMode resizeMode;

        public AdSurfaceAspectRatioResizeModeChanged(PKAspectRatioResizeMode resizeMode) {
            super(Type.ASPECT_RATIO_RESIZE_MODE_CHANGED);
            this.resizeMode = resizeMode;
        }
    }

    public static class AdWaterFalling extends AdEvent {

        public final AdBreakConfig adBreakConfig;

        public AdWaterFalling(AdBreakConfig adBreakConfig) {
            super(Type.AD_WATERFALLING);
            this.adBreakConfig = adBreakConfig;
        }
    }

    public static class AdWaterFallingFailed extends AdEvent {

        public final AdBreakConfig adBreakConfig;

        public AdWaterFallingFailed(AdBreakConfig adBreakConfig) {
            super(Type.AD_WATERFALLING_FAILED);
            this.adBreakConfig = adBreakConfig;
        }
    }

    public enum Type {
        AD_REQUESTED,
        AD_FIRST_PLAY,
        STARTED,
        AD_DISPLAYED_AFTER_CONTENT_PAUSE,
        PAUSED,
        RESUMED,
        COMPLETED,
        FIRST_QUARTILE,
        MIDPOINT,
        THIRD_QUARTILE,
        SKIPPED,
        SKIPPABLE_STATE_CHANGED,
        CLICKED,
        TAPPED,
        ICON_FALLBACK_IMAGE_CLOSED,
        ICON_TAPPED,
        AD_BREAK_READY,
        AD_PROGRESS,
        AD_BREAK_STARTED,
        AD_BREAK_ENDED,
        AD_BREAK_FETCH_ERROR,
        AD_BREAK_IGNORED,
        CUEPOINTS_CHANGED,
        PLAY_HEAD_CHANGED,
        LOADED,
        CONTENT_PAUSE_REQUESTED,
        CONTENT_RESUME_REQUESTED,
        ALL_ADS_COMPLETED,
        AD_LOAD_TIMEOUT_TIMER_STARTED,
        AD_BUFFER_START,
        AD_BUFFER_END,
        AD_PLAYBACK_INFO_UPDATED,
        ERROR,
        DAI_SOURCE_SELECTED,
        ASPECT_RATIO_RESIZE_MODE_CHANGED,
        AD_WATERFALLING,
        AD_WATERFALLING_FAILED
    }


    @Override
    public Enum eventType() {
        return this.type;
    }
}
