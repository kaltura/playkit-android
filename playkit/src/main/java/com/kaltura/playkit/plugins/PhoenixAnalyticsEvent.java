package com.kaltura.playkit.plugins;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class PhoenixAnalyticsEvent implements PKEvent {

    public enum Type {
        PHOENIX_ANALYTICS_REPORT
    }

    public static class PhoenixAnalyticsReport extends PhoenixAnalyticsEvent {

        private String reportedEventName;

        public PhoenixAnalyticsReport(String reportedEventName) {
            this.reportedEventName = reportedEventName;
        }

        public String getReportedEventName() {
            return reportedEventName;
        }
    }


    @Override
    public Enum eventType() {
        return Type.PHOENIX_ANALYTICS_REPORT;
    }
}
