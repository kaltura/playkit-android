package com.kaltura.playkit.plugins;

import com.kaltura.playkit.PKEvent;

/**
 * Analytics event class. This object will be send when analytics report sent.
 * Created by anton.afanasiev on 21/03/2017.
 */

public class AnalyticsEvent implements PKEvent {

    public AnalyticsEvent.Type type;

    public AnalyticsEvent(AnalyticsEvent.Type type) {
        this.type = type;
    }

    /**
     * KalturaLiveStats event. Sent every time when live stats report sent.
     * Holds the bufferTime of the live video.
     */
    public static class KalturaLiveStatsReportEvent extends AnalyticsEvent {

        private long bufferTime;

        public KalturaLiveStatsReportEvent(long bufferTime) {
            super(Type.KALTURA_LIVE_STATS_REPORT);
            this.bufferTime = bufferTime;
        }

        public long getBufferTime() {
            return bufferTime;
        }
    }

    /**
     * BaseAnalytics event. Sent every time when any of the analytics plugin report is sent.
     * Holds the String name of the event that is sent to analytics.
     */
    public static class BaseAnalyticsReportEvent extends AnalyticsEvent {

        private String reportedEventName;

        public BaseAnalyticsReportEvent(Type type, String reportedEventName) {
            super(type);
            this.reportedEventName = reportedEventName;
        }

        public String getReportedEventName() {
            return reportedEventName;
        }
    }

    public enum Type {
        KALTURA_STATS_REPORT,
        KALTURA_LIVE_STATS_REPORT,
        TVPAPI_REPORT,
        PHOENIX_REPORT,
        YOUBORA_REPORT
    }


    @Override
    public Enum eventType() {
        return this.type;
    }
}
