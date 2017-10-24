package com.kaltura.playkit.plugins.ovp;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 28/09/2017.
 */

public class KavaAnalyticsEvent implements PKEvent {

    public enum Type {
        REPORT_SENT
    }

    public static class KavaAnalyticsReport extends KavaAnalyticsEvent {

        public final String reportedEventName;

        public KavaAnalyticsReport(String reportedEventName) {
            this.reportedEventName = reportedEventName;
        }
    }

    @Override
    public Enum eventType() {
        return Type.REPORT_SENT;
    }
}
