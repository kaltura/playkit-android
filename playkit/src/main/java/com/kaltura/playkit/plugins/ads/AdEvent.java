package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.PKEvent;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdEvent implements PKEvent {

    public Type type;

    public AdEvent(Type type) {
        this.type = type;
    }

    public AdEvent(Type type, String message) {
        this.type = type;
    }

    public static class AdLoadedEvent extends AdEvent {

        public AdInfo adInfo;

        public AdLoadedEvent(AdInfo adInfo) {
            super(Type.LOADED);
            this.adInfo = adInfo;
        }
    }

    public static class AdStartedEvent extends AdEvent {

        public AdInfo adInfo;

        public AdStartedEvent(AdInfo adInfo) {
            super(Type.STARTED);
            this.adInfo = adInfo;
        }
    }

    public static class AdPausedEvent extends AdEvent {

        public AdInfo adInfo;

        public AdPausedEvent(AdInfo adInfo) {
            super(Type.PAUSED);
            this.adInfo = adInfo;
        }
    }

    public static class AdResumedEvent extends AdEvent {

        public AdInfo adInfo;

        public AdResumedEvent(AdInfo adInfo) {
            super(Type.RESUMED);
            this.adInfo = adInfo;
        }
    }

    public static class AdSkippedEvent extends AdEvent {

        public AdInfo adInfo;

        public AdSkippedEvent(AdInfo adInfo) {
            super(Type.SKIPPED);
            this.adInfo = adInfo;
        }
    }

    public static class AdCuePointsUpdateEvent extends AdEvent {

        public AdCuePoints cuePoints;

        public AdCuePointsUpdateEvent(AdCuePoints cuePoints) {
            super(Type.CUEPOINTS_CHANGED);
            this.cuePoints = cuePoints;
        }
    }

    public static class AdPlayHeadEvent extends AdEvent {

        public long adPlayHead;

        public AdPlayHeadEvent(long adPlayHead) {
            super(Type.PLAY_HEAD_CHANGED);
            this.adPlayHead = adPlayHead;
        }
    }

    public static class AdRequestedEvent extends AdEvent {

        public String adTagUrl;

        public AdRequestedEvent(String adTagUrl) {
            super(Type.AD_REQUESTED);
            this.adTagUrl = adTagUrl;
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
        AD_LOAD_TIMEOUT_TIMER_STARTED
    }


    @Override
    public Enum eventType() {
        return this.type;
    }
}
