package com.kaltura.playkit.plugins.ads;

import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPublicAPI;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

@PKPublicAPI
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

    public static class AdCuePointsUpdateEvent extends AdEvent {

        public AdCuePoints cuePoints;

        public AdCuePointsUpdateEvent(AdCuePoints cuePoints) {
            super(Type.CUEPOINTS_CHANGED);
            this.cuePoints = cuePoints;
        }
    }

    public enum Type {
        STARTED,
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
