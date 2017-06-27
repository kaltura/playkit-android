package com.kaltura.playkit.plugins.youbora;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class YouboraEvent implements PKEvent {

    public enum Type {
        REPORT_SENT
    }

    public static class YouboraReport extends YouboraEvent{

        public final String reportedEventName;

        public YouboraReport(String reportedEventName) {
            this.reportedEventName = reportedEventName;
        }
    }


    @Override
    public Enum eventType() {
        return Type.REPORT_SENT;
    }
}
