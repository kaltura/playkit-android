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

    public static class AdStartedEvent extends AdEvent {

        public AdInfo adInfo;

        public AdStartedEvent(AdInfo adInfo) {
            super(Type.STARTED);
            this.adInfo = adInfo;
        }
    }
    public enum Type {
        STARTED,
        PAUSED,
        APP_PAUSED_ON_AD,
        APP_RESUMED_ON_AD,
        APP_PAUSED_NO_AD,
        APP_RESUMED_NO_AD,
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
        CUEPOINTS_CHANGED,
        LOADED,
        CONTENT_PAUSE_REQUESTED,
        CONTENT_RESUME_REQUESTED,
        ALL_ADS_COMPLETED
    }


    @Override
    public Enum eventType() {
        return this.type;
    }
}
