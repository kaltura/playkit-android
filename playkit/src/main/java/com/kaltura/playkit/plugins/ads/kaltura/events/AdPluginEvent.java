package com.kaltura.playkit.plugins.ads.kaltura.events;

import com.kaltura.admanager.AdBreakInfo;
import com.kaltura.admanager.AdInfo;
import com.kaltura.playkit.PKEvent;

import java.util.Set;


public class AdPluginEvent implements PKEvent {

    public AdPluginEvent.Type type;

    public AdPluginEvent(AdPluginEvent.Type type) {
        this.type = type;
    }

    public AdPluginEvent(AdPluginEvent.Type type, String message) {
        this.type = type;
    }

    public static class AdRequestedEvent extends AdPluginEvent {

        public String adTagUrl;

        public AdRequestedEvent(String adTagUrl) {
            super(Type.AD_REQUESTED);
            this.adTagUrl = adTagUrl;
        }
    }

    public static class ProgressUpdateEvent extends AdPluginEvent {

        public long currentPosition;
        public long duration;

        public ProgressUpdateEvent(long currentPosition, long duration) {
            super(Type.AD_PROGRESS_UPDATE);
            this.currentPosition = currentPosition;
            this.duration = duration;
        }
    }

    public static class AdBreakStarted extends AdPluginEvent {

        public AdBreakInfo adBreakInfo;
        public AdInfo adInfo;

        public AdBreakStarted(AdBreakInfo adBreakInfo, AdInfo adInfo) {
            super(Type.CONTENT_PAUSE_REQUESTED);
            this.adBreakInfo = adBreakInfo;
            this.adInfo = adInfo;
        }
    }

    public static class AdLoadedEvent extends AdPluginEvent {

        public AdInfo adInfo;

        public AdLoadedEvent(AdInfo adInfo) {
            super(Type.LOADED);
            this.adInfo = adInfo;
        }
    }

    public static class AdvtClickEvent extends AdPluginEvent {

        public String advtLink;

        public AdvtClickEvent(String advtLink) {
            super(Type.CLICKED);
            this.advtLink = advtLink;
        }
    }

    public static class AdBufferEvent extends AdPluginEvent {

        public boolean show;

        public AdBufferEvent(boolean show) {
            super(Type.AD_BUFFER);
            this.show = show;
        }
    }

    public static class CuePointsChangedEvent extends AdPluginEvent {

        public Set<Double> cuePoints;

        public CuePointsChangedEvent(Set<Double> cuePoints) {
            super(Type.CUEPOINTS_CHANGED);
            this.cuePoints = cuePoints;
        }
    }

    public enum Type {
        AD_BREAK_PENDING,
        AD_BREAK_ENDED,
        AD_PROGRESS_UPDATE,
        PLAYBACK_STATE,
        AD_REQUESTED,
        CUEPOINTS_CHANGED,
        LOADED,
        STARTED,
        PAUSED,
        RESUMED,
        COMPLETED,
        SKIPPED,
        CLICKED,
        PLAYER_SWITCH_MAXIMIZE,
        PLAYER_STATE,
        AD_BUFFER,
        CONTENT_PAUSE_REQUESTED,
        CONTENT_RESUME_REQUESTED,
        AD_BREAK_IGNORED,
        ALL_ADS_COMPLETED
    }


    @Override
    public Enum eventType() {
        return this.type;
    }
}
