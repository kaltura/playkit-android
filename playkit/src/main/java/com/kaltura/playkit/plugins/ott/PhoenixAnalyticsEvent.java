package com.kaltura.playkit.plugins.ott;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class PhoenixAnalyticsEvent implements PKEvent {

    public enum Type {
        REPORT_SENT
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
        return Type.REPORT_SENT;
    }
}
