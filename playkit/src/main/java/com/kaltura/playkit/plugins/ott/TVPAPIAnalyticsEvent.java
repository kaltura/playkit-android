package com.kaltura.playkit.plugins.ott;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class TVPAPIAnalyticsEvent implements PKEvent {

    public enum Type {
        REPORT_SENT
    }

    public static class TVPAPIAnalyticsReport extends TVPAPIAnalyticsEvent {

        private String reportedEventName;

        public TVPAPIAnalyticsReport(String reportedEventName) {
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
