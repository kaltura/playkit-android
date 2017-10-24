/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

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
