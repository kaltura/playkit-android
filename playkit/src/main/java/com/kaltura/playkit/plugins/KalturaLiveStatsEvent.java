package com.kaltura.playkit.plugins;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class KalturaLiveStatsEvent implements PKEvent {

    public enum Type {
        KALTURA_LIVE_STATS_REPORT
    }

    public static class KalturaLiveStatsReport extends KalturaLiveStatsEvent {

        private long bufferTime;

        public KalturaLiveStatsReport(long bufferTime) {
            this.bufferTime = bufferTime;
        }

        public long getBufferTime() {
            return bufferTime;
        }
    }


    @Override
    public Enum eventType() {
        return Type.KALTURA_LIVE_STATS_REPORT;
    }
}
