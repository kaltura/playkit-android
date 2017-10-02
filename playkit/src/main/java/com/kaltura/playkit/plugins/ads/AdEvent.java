package com.kaltura.playkit.plugins.ads;

///*
// * ============================================================================
// * Copyright (C) 2017 Kaltura Inc.
// *
// * Licensed under the AGPLv3 license, unless a different license for a
// * particular library is specified in the applicable library path.
// *
// * You may obtain a copy of the License at
// * https://www.gnu.org/licenses/agpl-3.0.html
// * ============================================================================
// */

import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.ads.PKAdBreakEndedReason;

import java.util.ArrayList;
import java.util.Set;


public class AdEvent implements PKEvent {

    public AdEvent.Type type;

    public AdEvent(AdEvent.Type type) {
        this.type = type;
    }

    public AdEvent(AdEvent.Type type, String message) {
        this.type = type;
    }

    public static class AdRequestedEvent extends AdEvent {

        public String adTagUrl;

        public AdRequestedEvent(String adTagUrl) {
            super(Type.AD_REQUESTED);
            this.adTagUrl = adTagUrl;
        }
    }

    public static class AdProgressUpdateEvent extends AdEvent {

        public long currentPosition;
        public long duration;

        public AdProgressUpdateEvent(long currentPosition, long duration) {
            super(Type.AD_PROGRESS_UPDATE);
            this.currentPosition = currentPosition;
            this.duration = duration;
        }
    }

    public static class AdBreakStarted extends AdEvent {

        public AdInfo adInfo;

        public AdBreakStarted(AdInfo adInfo) {
            super(Type.AD_BREAK_STARTED);
            this.adInfo = adInfo;
        }
    }

    public static class AdBreakEnded extends AdEvent {

        public PKAdBreakEndedReason adBreakEndedReason;

        public AdBreakEnded(PKAdBreakEndedReason adBreakEndedReason) {
            super(Type.AD_BREAK_ENDED);
            this.adBreakEndedReason = adBreakEndedReason;
        }
    }

    public static class AdLoadedEvent extends AdEvent {

        public AdInfo adInfo;

        public AdLoadedEvent(AdInfo adInfo) {
            super(Type.LOADED);
            this.adInfo = adInfo;
        }
    }

    public static class AdvtClickEvent extends AdEvent {

        public String advtLink;

        public AdvtClickEvent(String advtLink) {
            super(Type.CLICKED);
            this.advtLink = advtLink;
        }
    }

    public static class AdBufferEvent extends AdEvent {

        public boolean show;

        public AdBufferEvent(boolean show) {
            super(Type.AD_BUFFER);
            this.show = show;
        }
    }

    public static class AdCuePointsChangedEvent extends AdEvent {

        public AdCuePoints adCuePoints;

        public AdCuePointsChangedEvent(Set<Double> cuePoints) {
            super(Type.CUEPOINTS_CHANGED);
            this.adCuePoints = new AdCuePoints(new ArrayList<Double>(cuePoints));
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
        AD_BREAK_PENDING, //CONTENT_PAUSE_REQUESTED
        AD_BREAK_STARTED,
        AD_BREAK_ENDED,
        AD_BREAK_IGNORED,
        ADS_PLAYBACK_ENDED, //CONTENT_RESUME_REQUESTED
        AD_PROGRESS_UPDATE,
        PLAYBACK_STATE,
        AD_REQUESTED,
        CUEPOINTS_CHANGED,
        LOADED,
        STARTED,
        FIRST_QUARTILE,
        MIDPOINT,
        THIRD_QUARTILE,
        PAUSED,
        RESUMED,
        COMPLETED,
        SKIPPED,
        CLICKED,
        PLAYER_SWITCH_MAXIMIZE,
        PLAYER_STATE,
        AD_BUFFER,
        TAPPED,
        ICON_TAPPED,
        ERROR_LOG, // none fatal error while player cannot play stream URL
        ERROR,
        ALL_ADS_COMPLETED

    }

    @Override
    public Enum eventType() {
        return this.type;
    }
}
