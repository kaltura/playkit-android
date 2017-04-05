package com.kaltura.playkit.plugins;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class TVPapiAnalyticsEvent implements PKEvent {

    public enum Type {
        REPORT_SENT
    }

    public static class TVPapiAnalyticsReport extends TVPapiAnalyticsEvent {

        private String reportedEventName;

        public TVPapiAnalyticsReport(String reportedEventName) {
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
