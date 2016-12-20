package com.kaltura.playkit;

/**
 * Created by zivilan on 24/11/2016.
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
