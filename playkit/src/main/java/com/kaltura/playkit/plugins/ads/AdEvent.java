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

import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKError;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdEvent implements PKEvent {

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

        public AdRequestedEvent(String adTagUrl) {
            super(Type.AD_REQUESTED);
            this.adTagUrl = adTagUrl;
        }
    }

    public static class Error extends AdEvent {

        public final PKError error;

        public Error(PKError error) {
            super(Type.ERROR);
            this.error = error;
        }
    }

    public enum Type {
        AD_REQUESTED,
        STARTED,
        AD_DISPLAYED_AFTER_CONTENT_PAUSE,
        PAUSED,
        RESUMED,
        COMPLETED,
        FIRST_QUARTILE,
        MIDPOINT,
        THIRD_QUARTILE,
        SKIPPED(),
        CLICKED,
        TAPPED,
        ICON_TAPPED,
        AD_BREAK_READY,
        AD_PROGRESS,
        AD_BREAK_STARTED,
        AD_BREAK_ENDED,
        AD_BREAK_IGNORED,
        CUEPOINTS_CHANGED,
        PLAY_HEAD_CHANGED,
        LOADED,
        CONTENT_PAUSE_REQUESTED,
        CONTENT_RESUME_REQUESTED,
        ALL_ADS_COMPLETED,
        AD_LOAD_TIMEOUT_TIMER_STARTED,
        ERROR
    }


    @Override
    public Enum eventType() {
        return this.type;
    }
}
