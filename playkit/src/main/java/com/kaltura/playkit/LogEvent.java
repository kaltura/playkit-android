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

package com.kaltura.playkit;

/**
 * @hide
 */

public class LogEvent implements PKEvent {
    public final String log;
    public final LogType type;
    public final String request;

    public enum LogType
    {LogEvent} //Plan to separate it to different log events for each plugin

    public LogEvent(String log) {
        this.type = LogType.LogEvent;
        this.log = log;
        this.request = "";
    }
    public LogEvent(String log, String request) {
        this.type = LogType.LogEvent;
        this.log = log;
        this.request = request;
    }

    @Override
    public Enum eventType() {
        return this.type;
    }

    public interface Listener {
        void onLogEvent(String log);
    }
}
