package com.kaltura.playkit.plugins;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class KalturaStatsEvent implements PKEvent{

    public enum Type {
        KALTURA_STATS_REPORT
    }

    public static class KalturaStatsReport extends KalturaStatsEvent {

        private String reportedEventName;

        public KalturaStatsReport(String reportedEventName) {
            this.reportedEventName = reportedEventName;
        }

        public String getReportedEventName() {
            return reportedEventName;
        }
    }


    @Override
    public Enum eventType() {
        return Type.KALTURA_STATS_REPORT;
    }
}
