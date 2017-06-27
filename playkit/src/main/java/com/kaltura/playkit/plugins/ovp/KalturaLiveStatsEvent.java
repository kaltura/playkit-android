package com.kaltura.playkit.plugins.ovp;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class KalturaLiveStatsEvent implements PKEvent {

    public enum Type {
        REPORT_SENT
    }

    public static class KalturaLiveStatsReport extends KalturaLiveStatsEvent {

        public final long bufferTime;

        public KalturaLiveStatsReport(long bufferTime) {
            this.bufferTime = bufferTime;
        }

    }


    @Override
    public Enum eventType() {
        return Type.REPORT_SENT;
    }
}
