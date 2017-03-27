package com.kaltura.playkit.plugins.Youbora;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class YouboraEvent implements PKEvent {

    public enum Type {
        YOUBORA_REPORT
    }

    public static class YouboraReport extends YouboraEvent{

        private String reportedEventName;

        public YouboraReport(String reportedEventName) {
            this.reportedEventName = reportedEventName;
        }

        public String getReportedEventName() {
            return reportedEventName;
        }
    }


    @Override
    public Enum eventType() {
        return Type.YOUBORA_REPORT;
    }
}
